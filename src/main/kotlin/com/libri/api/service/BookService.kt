package com.libri.api.service

import com.libri.api.config.StorageConfig
import com.libri.api.dto.BarcodeDTO
import com.libri.api.dto.BookDTO
import com.libri.api.entity.Barcode
import com.libri.api.entity.Book
import com.libri.api.repository.BarcodeRepository
import com.libri.api.repository.BarcodeValueRow
import com.libri.api.repository.BookBarcodeReplacement
import com.libri.api.repository.BookBatchRepository
import com.libri.api.repository.BookRepository
import com.libri.api.repository.PurgatoryBarcodeReplacement
import com.libri.api.repository.PurgatoryBatchRepository
import com.libri.api.util.IsbnValidator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.io.File

@Service
class BookService(
    private val bookRepository: BookRepository,
    private val barcodeRepository: BarcodeRepository,
    private val bookBatchRepository: BookBatchRepository,
    private val purgatoryBatchRepository: PurgatoryBatchRepository,
    private val storageService: StorageService,
    private val storageConfig: StorageConfig,
    private val redisService: RedisService,
) {
    fun list(
        pageable: Pageable,
        filter: String?,
    ): Page<BookDTO> {
        val books =
            if (filter.isNullOrBlank()) {
                bookRepository.findAll(pageable)
            } else {
                bookRepository.searchByTitle(filter, pageable)
            }

        val isbns = books.content.map { it.isbn }
        val barcodesByIsbn =
            barcodeRepository
                .findAllByIsbnIn(isbns)
                .groupBy { it.isbn }
        return books.map { toDTO(it, barcodesByIsbn[it.isbn]) }
    }

    fun getByCode(code: String): BookDTO? {
        bookRepository.findByIdOrNull(code)?.let { return toDTO(it) }

        val barcode = barcodeRepository.findFirstByValue(code) ?: return null
        return bookRepository.findByIdOrNull(barcode.isbn)?.let { toDTO(it) }
    }

    @Transactional
    fun upsertBatch(books: List<BookDTO>) {
        if (books.isEmpty()) return

        val (validBooks, invalidBooks) = books.partition { IsbnValidator.isValid(it.isbn) }

        if (validBooks.isNotEmpty()) {
            upsertValidBooks(validBooks)
            redisService.addExistingUrls(validBooks.map { it.url })
        }
        if (invalidBooks.isNotEmpty()) upsertInvalidBooks(invalidBooks)
    }

    @Transactional
    fun updateBook(
        isbn: String,
        newBook: BookDTO,
        image: MultipartFile?,
    ): BookDTO? {
        if (!bookRepository.existsById(isbn)) {
            return null
        }

        if (image != null && !image.isEmpty) {
            storageService.storeTransactional(isbn, image)
        }

        val updatedBook = newBook.copy(isbn = isbn).toEntity()
        val saved = bookRepository.save(updatedBook)

        barcodeRepository.deleteAllByIsbn(isbn)
        barcodeRepository.saveAll(
            newBook.barcodes.map { it.toEntity(newBook.isbn, newBook.sourceName) },
        )

        return toDTO(saved)
    }

    @Transactional
    fun createBook(
        newBook: BookDTO,
        image: MultipartFile,
    ): BookDTO? {
        if (bookRepository.existsById(newBook.isbn) || image.isEmpty) {
            return null
        }

        storageService.storeTransactional(newBook.isbn, image)
        return toDTO(bookRepository.save(newBook.toEntity()))
    }

    @Transactional
    fun deleteBook(isbn: String): Boolean {
        if (!bookRepository.existsById(isbn)) {
            return false
        }

        storageService.deleteTransactional(isbn)
        bookRepository.deleteById(isbn)
        return true
    }

    @Transactional
    fun deleteBooks(isbns: List<String>): Boolean {
        if (isbns.isEmpty()) return true

        val existingBooks = bookRepository.findAllById(isbns)
        if (existingBooks.isEmpty()) return false

        val trashDir = File(storageConfig.imagesDir, ".trash/${System.currentTimeMillis()}")
        trashDir.mkdirs()

        val movedFiles = mutableMapOf<String, File>()
        isbns.forEach { isbn ->
            val file = storageConfig.resolveImagePath(isbn)
            if (file.exists()) {
                val target = File(trashDir, file.name)
                val success = file.renameTo(target)
                if (success) movedFiles[isbn] = target
            }
        }

        bookRepository.deleteAllByIdInBatch(isbns)

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCompletion(status: Int) {
                        when (status) {
                            TransactionSynchronization.STATUS_ROLLED_BACK -> {
                                movedFiles.forEach { (isbn, fileInTrash) ->
                                    val original = storageConfig.resolveImagePath(isbn)
                                    original.parentFile.mkdirs()
                                    fileInTrash.renameTo(original)
                                }

                                trashDir.deleteRecursively()
                            }

                            else -> {
                                trashDir.deleteRecursively()
                            }
                        }
                    }
                },
            )
        }

        return true
    }

    private fun toDTO(
        book: Book,
        cachedBarcodes: List<Barcode>? = null,
    ): BookDTO {
        val barcodes = (cachedBarcodes ?: barcodeRepository.findAllByIsbn(book.isbn)).map { it.toDTO() }
        return book.toDTO(barcodes)
    }

    private fun upsertValidBooks(books: List<BookDTO>) {
        val deduped = bookBatchRepository.deduplicateByPriority(books)
        bookBatchRepository.upsertBooks(deduped.map { it.toEntity() })
        bookBatchRepository.replaceBarcodes(deduped.map { it.toBookBarcodeReplacement() })
    }

    private fun upsertInvalidBooks(books: List<BookDTO>) {
        val deduped = books.distinctBy { it.isbn to it.sourceName }
        purgatoryBatchRepository.upsertBooks(deduped.map { it.toPurgatoryEntity() })

        val purgatoryByKey = purgatoryBatchRepository.findStatesByInvalidIsbn(deduped.map { it.isbn })
        val resolvedBooks =
            deduped.mapNotNull { book ->
                val purgatory = purgatoryByKey[book.isbn to book.sourceName] ?: return@mapNotNull null
                val resolvedIsbn = purgatory.resolvedIsbn ?: return@mapNotNull null
                book.copy(isbn = resolvedIsbn)
            }

        val dedupedResolved = bookBatchRepository.deduplicateByPriority(resolvedBooks)
        bookBatchRepository.upsertBooks(dedupedResolved.map { it.toEntity() })
        bookBatchRepository.replaceBarcodes(dedupedResolved.map { it.toBookBarcodeReplacement() })

        val purgatoryBarcodeReplacements =
            deduped.mapNotNull { book ->
                val p = purgatoryByKey[book.isbn to book.sourceName] ?: return@mapNotNull null
                PurgatoryBarcodeReplacement(
                    purgatoryId = p.id,
                    sourceName = book.sourceName,
                    barcodes = book.barcodes.map { it.toBarcodeValueRow() },
                )
            }
        purgatoryBatchRepository.replaceBarcodes(purgatoryBarcodeReplacements)
    }

    private fun BookDTO.toBookBarcodeReplacement(isbn: String = this.isbn) =
        BookBarcodeReplacement(
            isbn = isbn,
            sourceName = sourceName,
            barcodes = barcodes.map { it.toBarcodeValueRow() },
        )

    private fun BarcodeDTO.toBarcodeValueRow() =
        BarcodeValueRow(
            value = value,
            type = type,
        )
}
