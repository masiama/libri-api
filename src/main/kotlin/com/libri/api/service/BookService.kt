package com.libri.api.service

import com.libri.api.config.StorageConfig
import com.libri.api.dto.BookDTO
import com.libri.api.entity.Barcode
import com.libri.api.entity.Book
import com.libri.api.entity.PurgatoryBarcode
import com.libri.api.entity.PurgatoryBook
import com.libri.api.repository.*
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
	private val purgatoryBookRepository: PurgatoryBookRepository,
	private val purgatoryBarcodeRepository: PurgatoryBarcodeRepository,
	private val sourceRepository: SourceRepository,
	private val storageService: StorageService,
	private val storageConfig: StorageConfig,
) {
	fun list(pageable: Pageable, filter: String?): Page<BookDTO> {
		val books =
			if (filter.isNullOrBlank()) bookRepository.findAll(pageable)
			else bookRepository.searchByTitle(filter, pageable)

		val isbns = books.content.map { it.isbn }
		val barcodesByIsbn = barcodeRepository
			.findAllByIsbnIn(isbns)
			.groupBy { it.isbn }
		return books.map { toDTO(it, barcodesByIsbn[it.isbn]) }
	}

	fun getByCode(code: String): BookDTO? {
		bookRepository.findByIdOrNull(code)?.let { return toDTO(it) }

		val barcode = barcodeRepository.findFirstByValue(code) ?: return null
		return bookRepository.findByIdOrNull(barcode.isbn)?.let { toDTO(it) }
	}

	fun existsByUrl(url: String): Boolean = bookRepository.existsByUrl(url)

	@Transactional
	fun upsertBatch(books: List<BookDTO>) {
		if (books.isEmpty()) return

		val (validBooks, invalidBooks) = books.partition { IsbnValidator.isValid(it.isbn) }
		val sourcePriorities = sourceRepository.findAll().associate { it.name to it.priority }

		if (validBooks.isNotEmpty()) {
			val existingByIsbn = bookRepository
				.findAllById(validBooks.map { it.isbn })
				.associateBy(Book::isbn)

			val booksToSave = validBooks.filter { incoming ->
				val existing = existingByIsbn[incoming.isbn] ?: return@filter true
				val incomingPriority = sourcePriorities[incoming.sourceName] ?: Short.MAX_VALUE
				val existingPriority = sourcePriorities[existing.sourceName] ?: Short.MAX_VALUE
				incomingPriority <= existingPriority
			}
			if (booksToSave.isNotEmpty()) bookRepository.saveAll(booksToSave.map { it.toEntity() })

			barcodeRepository.deleteAllByIsbnAndSourceNamePairs(
				validBooks.map { Pair(it.isbn, it.sourceName) }.distinct()
			)
			barcodeRepository.saveAll(
				validBooks.flatMap { book ->
					book.barcodes.map { it.toEntity(book.isbn, book.sourceName) }
				}
			)
		}

		if (invalidBooks.isNotEmpty()) {
			val existingPurgatory = purgatoryBookRepository
				.findAllByInvalidIsbnIn(invalidBooks.map { it.isbn })
				.groupBy { it.invalidIsbn }

			val purgatoryToInsert = mutableListOf<BookDTO>()
			val purgatoryToUpdate = mutableListOf<PurgatoryBook>()
			val purgatoryBarcodes = mutableListOf<PurgatoryBarcode>()
			val booksToUpdate = mutableListOf<Book>()
			val barcodes = mutableListOf<Barcode>()

			invalidBooks.forEach { incoming ->
				val existing = existingPurgatory[incoming.isbn]
					?.firstOrNull { it.sourceName == incoming.sourceName }

				if (existing == null) {
					purgatoryToInsert.add(incoming)
				} else {
					existing.title = incoming.title
					existing.authors = incoming.authors
					existing.url = incoming.url
					purgatoryToUpdate.add(existing)
					purgatoryBarcodes.addAll(incoming.barcodes.map {
						PurgatoryBarcode(it.value, it.type, existing.id, existing.sourceName)
					})

					existing.resolvedIsbn?.let { resolvedIsbn ->
						val book = bookRepository.findByIdOrNull(resolvedIsbn) ?: return@let
						val incomingPriority = sourcePriorities[incoming.sourceName] ?: Short.MAX_VALUE
						val existingPriority = sourcePriorities[book.sourceName] ?: Short.MAX_VALUE
						if (incomingPriority <= existingPriority) {
							booksToUpdate.add(incoming.copy(isbn = resolvedIsbn).toEntity())
						}
						barcodes.addAll(
							incoming.barcodes.map { it.toEntity(resolvedIsbn, incoming.sourceName) }
						)
					}
				}
			}

			if (booksToUpdate.isNotEmpty()) bookRepository.saveAll(booksToUpdate)
			if (barcodes.isNotEmpty()) {
				barcodeRepository.deleteAllByIsbnAndSourceNamePairs(
					barcodes.map { Pair(it.isbn, it.sourceName) }.distinct()
				)
				barcodeRepository.saveAll(barcodes)
			}
			if (purgatoryToUpdate.isNotEmpty()) purgatoryBookRepository.saveAll(purgatoryToUpdate)
			if (purgatoryBarcodes.isNotEmpty()) {
				purgatoryBarcodeRepository.deleteAllByPurgatoryIdAndSourceNamePairs(
					purgatoryBarcodes.map { Pair(it.purgatoryId, it.sourceName) }.distinct()
				)
				purgatoryBarcodeRepository.saveAll(purgatoryBarcodes)
			}
			if (purgatoryToInsert.isNotEmpty()) {
				val purgatoryBooks = purgatoryToInsert.map { it.toPurgatoryEntity() }
				val savedPurgatoryBooks =
					purgatoryBookRepository
						.saveAll(purgatoryBooks)
						.associate { (it.invalidIsbn to it.sourceName) to it.id }
				purgatoryBarcodeRepository.saveAll(
					purgatoryToInsert.flatMap { book ->
						book.barcodes.mapNotNull {
							val purgatoryId =
								savedPurgatoryBooks[book.isbn to book.sourceName] ?: return@mapNotNull null
							PurgatoryBarcode(it.value, it.type, purgatoryId, book.sourceName)
						}
					}
				)
			}
		}
	}

	@Transactional
	fun updateBook(isbn: String, newBook: BookDTO, image: MultipartFile?): BookDTO? {
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
			newBook.barcodes.map { it.toEntity(newBook.isbn, newBook.sourceName) }
		)

		return toDTO(saved)
	}

	@Transactional
	fun createBook(newBook: BookDTO, image: MultipartFile): BookDTO? {
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
			TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
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

						else -> trashDir.deleteRecursively()
					}
				}
			})
		}

		return true
	}

	private fun toDTO(book: Book, cachedBarcodes: List<Barcode>? = null): BookDTO {
		val barcodes = (cachedBarcodes ?: barcodeRepository.findAllByIsbn(book.isbn)).map { it.toDTO() }
		return book.toDTO(barcodes)
	}
}
