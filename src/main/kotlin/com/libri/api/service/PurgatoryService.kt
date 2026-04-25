package com.libri.api.service

import com.libri.api.entity.Book
import com.libri.api.entity.PurgatoryBook
import com.libri.api.repository.BookRepository
import com.libri.api.repository.PurgatoryBookRepository
import com.libri.api.repository.SourceRepository
import com.libri.api.util.IsbnValidator
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PurgatoryService(
	private val purgatoryBookRepository: PurgatoryBookRepository,
	private val bookRepository: BookRepository,
	private val sourceRepository: SourceRepository,
	private val storageService: StorageService,
) {
	@Transactional
	fun approve(id: Long, newIsbn: String): PurgatoryBook? {
		if (!IsbnValidator.isValid(newIsbn)) return null

		val purgatoryBook = purgatoryBookRepository.findByIdOrNull(id) ?: return null

		val sourcePriorities = sourceRepository.findAll()
			.associate { it.name to it.priority }

		val incomingPriority = sourcePriorities[purgatoryBook.sourceName] ?: Short.MAX_VALUE
		val existing = bookRepository.findByIdOrNull(newIsbn)

		if (existing != null) {
			val existingPriority = sourcePriorities[existing.sourceName] ?: Short.MAX_VALUE
			if (incomingPriority <= existingPriority) {
				bookRepository.save(
					Book(
						isbn = existing.isbn,
						title = purgatoryBook.title,
						authors = purgatoryBook.authors,
						url = purgatoryBook.url,
						sourceName = purgatoryBook.sourceName,
					)
				)
			}
		} else {
			bookRepository.save(
				Book(
					isbn = newIsbn,
					title = purgatoryBook.title,
					authors = purgatoryBook.authors,
					url = purgatoryBook.url,
					sourceName = purgatoryBook.sourceName,
				)
			)
		}

		purgatoryBook.resolvedIsbn = newIsbn
		val saved = purgatoryBookRepository.save(purgatoryBook)

		storageService.copyImageTransactional(purgatoryBook.invalidIsbn, newIsbn)

		return saved
	}

	@Transactional
	fun markDeleted(id: Long): Boolean {
		val book = purgatoryBookRepository.findByIdOrNull(id) ?: return false
		book.deleted = true
		purgatoryBookRepository.save(book)
		storageService.deleteTransactional(book.invalidIsbn)
		return true
	}
}
