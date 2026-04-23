package com.libri.api.service

import com.libri.api.config.StorageConfig
import com.libri.api.entity.Book
import com.libri.api.repository.BookRepository
import com.libri.api.repository.SourceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.io.File

@Service
class BookService(
	private val bookRepository: BookRepository,
	private val sourceRepository: SourceRepository,
	private val storageService: StorageService,
	private val storageConfig: StorageConfig,
) {
	@Transactional
	fun upsertBatch(books: List<Book>) {
		if (books.isEmpty()) return

		val existingBooksByIsbn = bookRepository.findAllById(books.map { it.isbn })
			.associateBy(Book::isbn)
		val sourcePriorities = sourceRepository.findAll()
			.associate { it.name to it.priority }

		val booksToSave = books.filter { incoming ->
			val existing = existingBooksByIsbn[incoming.isbn] ?: return@filter true

			val incomingPriority = sourcePriorities[incoming.sourceName] ?: Short.MAX_VALUE
			val existingPriority = sourcePriorities[existing.sourceName] ?: Short.MAX_VALUE

			incomingPriority <= existingPriority
		}

		if (booksToSave.isNotEmpty()) {
			bookRepository.saveAll(booksToSave)
		}
	}

	@Transactional
	fun updateBook(isbn: String, newBook: Book, image: MultipartFile?): Book? {
		if (!bookRepository.existsById(isbn)) {
			return null
		}

		if (image != null && !image.isEmpty) {
			storageService.storeTransactional(isbn, image)
		}

		val updatedBook = Book(
			isbn = isbn,
			title = newBook.title,
			authors = newBook.authors,
			url = newBook.url,
			sourceName = newBook.sourceName
		)

		return bookRepository.save(updatedBook)
	}

	@Transactional
	fun createBook(newBook: Book, image: MultipartFile): Book? {
		if (bookRepository.existsById(newBook.isbn) || image.isEmpty) {
			return null
		}

		storageService.storeTransactional(newBook.isbn, image)

		val createdBook = Book(
			isbn = newBook.isbn,
			title = newBook.title,
			authors = newBook.authors,
			url = newBook.url,
			sourceName = newBook.sourceName
		)

		return bookRepository.save(createdBook)
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
