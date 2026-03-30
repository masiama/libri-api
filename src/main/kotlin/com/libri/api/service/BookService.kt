package com.libri.api.service

import com.libri.api.entity.Book
import com.libri.api.repository.BookRepository
import com.libri.api.repository.SourceRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class BookService(
	private val bookRepository: BookRepository,
	private val sourceRepository: SourceRepository,
) {
	fun upsertBatch(books: List<Book>) {
		books.forEach { incoming ->
			val existing = bookRepository.findByIdOrNull(incoming.isbn)

			if (existing == null) {
				bookRepository.save(incoming)
				return@forEach
			}

			val incomingPriority = sourceRepository.findByIdOrNull(incoming.sourceName)?.priority ?: Short.MAX_VALUE
			val existingPriority = sourceRepository.findByIdOrNull(existing.sourceName)?.priority ?: Short.MAX_VALUE

			if (incomingPriority <= existingPriority) {
				bookRepository.save(incoming)
			}
		}
	}
}
