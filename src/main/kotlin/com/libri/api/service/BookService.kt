package com.libri.api.service

import com.libri.api.config.StorageConfig
import com.libri.api.dto.BookDTO
import com.libri.api.dto.toDTO
import com.libri.api.dto.toEntity
import com.libri.api.entity.Book
import com.libri.api.entity.PurgatoryBook
import com.libri.api.repository.BookRepository
import com.libri.api.repository.PurgatoryBookRepository
import com.libri.api.repository.SourceRepository
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
	private val purgatoryBookRepository: PurgatoryBookRepository,
	private val sourceRepository: SourceRepository,
	private val storageService: StorageService,
	private val storageConfig: StorageConfig,
) {
	fun list(pageable: Pageable, filter: String?): Page<BookDTO> {
		val books =
			if (filter.isNullOrBlank()) bookRepository.findAll(pageable)
			else bookRepository.searchByTitle(filter, pageable)
		return books.map { it.toDTO() }
	}

	fun getByIsbn(isbn: String): BookDTO? = bookRepository.findByIdOrNull(isbn)?.toDTO()
	
	fun existsByUrl(url: String): Boolean = bookRepository.existsByUrl(url)

	@Transactional
	fun upsertBatch(books: List<BookDTO>) {
		if (books.isEmpty()) return

		val (validBooks, invalidBooks) = books
			.map { it.toEntity() }
			.partition { IsbnValidator.isValid(it.isbn) }

		if (validBooks.isNotEmpty()) {
			val existingByIsbn = bookRepository.findAllById(validBooks.map { it.isbn })
				.associateBy(Book::isbn)
			val sourcePriorities = sourceRepository.findAll()
				.associate { it.name to it.priority }

			val booksToSave = validBooks
				.filter { incoming ->
					val existing = existingByIsbn[incoming.isbn] ?: return@filter true
					val incomingPriority = sourcePriorities[incoming.sourceName] ?: Short.MAX_VALUE
					val existingPriority = sourcePriorities[existing.sourceName] ?: Short.MAX_VALUE
					incomingPriority <= existingPriority
				}

			if (booksToSave.isNotEmpty()) bookRepository.saveAll(booksToSave)
		}

		if (invalidBooks.isNotEmpty()) {
			val existingPurgatory = purgatoryBookRepository
				.findAllByInvalidIsbnIn(invalidBooks.map { it.isbn })
				.groupBy { it.invalidIsbn }
			val sourcePriorities = sourceRepository.findAll()
				.associate { it.name to it.priority }

			val purgatoryToSave = mutableListOf<PurgatoryBook>()
			val booksToUpdate = mutableListOf<Book>()

			invalidBooks.forEach { incoming ->
				val existing = existingPurgatory[incoming.isbn]
					?.firstOrNull { it.sourceName == incoming.sourceName }

				if (existing == null) {
					purgatoryToSave.add(
						PurgatoryBook(
							invalidIsbn = incoming.isbn,
							title = incoming.title,
							authors = incoming.authors,
							url = incoming.url,
							sourceName = incoming.sourceName,
						)
					)
				} else {
					existing.title = incoming.title
					existing.authors = incoming.authors
					existing.url = incoming.url
					purgatoryToSave.add(existing)

					existing.resolvedIsbn?.let { resolvedIsbn ->
						val book = bookRepository.findByIdOrNull(resolvedIsbn) ?: return@let
						val incomingPriority = sourcePriorities[incoming.sourceName] ?: Short.MAX_VALUE
						val existingPriority = sourcePriorities[book.sourceName] ?: Short.MAX_VALUE
						if (incomingPriority <= existingPriority) {
							booksToUpdate.add(
								Book(
									isbn = resolvedIsbn,
									title = incoming.title,
									authors = incoming.authors,
									url = incoming.url,
									sourceName = incoming.sourceName,
								)
							)
						}
					}
				}
			}

			if (purgatoryToSave.isNotEmpty()) purgatoryBookRepository.saveAll(purgatoryToSave)
			if (booksToUpdate.isNotEmpty()) bookRepository.saveAll(booksToUpdate)
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
		return bookRepository.save(updatedBook).toDTO()
	}

	@Transactional
	fun createBook(newBook: BookDTO, image: MultipartFile): BookDTO? {
		if (bookRepository.existsById(newBook.isbn) || image.isEmpty) {
			return null
		}

		storageService.storeTransactional(newBook.isbn, image)
		return bookRepository.save(newBook.toEntity()).toDTO()
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
}
