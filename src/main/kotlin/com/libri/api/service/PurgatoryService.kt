package com.libri.api.service

import com.libri.api.dto.PurgatoryBookDTO
import com.libri.api.entity.Barcode
import com.libri.api.repository.*
import com.libri.api.util.IsbnValidator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PurgatoryService(
	private val purgatoryBookRepository: PurgatoryBookRepository,
	private val purgatoryBarcodeRepository: PurgatoryBarcodeRepository,
	private val bookRepository: BookRepository,
	private val barcodeRepository: BarcodeRepository,
	private val sourceRepository: SourceRepository,
	private val storageService: StorageService,
) {
	fun list(pageable: Pageable, filter: String?): Page<PurgatoryBookDTO> {
		val books =
			if (filter.isNullOrBlank()) purgatoryBookRepository.findAllByResolvedIsbnIsNullAndDeletedFalse(pageable)
			else purgatoryBookRepository.findAllByTitleAndResolvedIsbnIsNullAndDeletedFalse(filter, pageable)
		return books.map { it.toDTO() }
	}

	@Transactional
	fun approve(id: Long, newIsbn: String): PurgatoryBookDTO? {
		if (!IsbnValidator.isValid(newIsbn)) return null

		val purgatoryBook = purgatoryBookRepository.findByIdOrNull(id) ?: return null

		val sourcePriorities = sourceRepository.findAll()
			.associate { it.name to it.priority }

		val incomingPriority = sourcePriorities[purgatoryBook.sourceName] ?: Short.MAX_VALUE
		val existing = bookRepository.findByIdOrNull(newIsbn)

		if (existing != null) {
			val existingPriority = sourcePriorities[existing.sourceName] ?: Short.MAX_VALUE
			if (incomingPriority <= existingPriority) {
				bookRepository.save(purgatoryBook.toBook(existing.isbn))
			}
		} else {
			bookRepository.save(purgatoryBook.toBook(newIsbn))
		}

		purgatoryBarcodeRepository
			.findAllByPurgatoryId(id)
			.map { Barcode(it.value, it.type, newIsbn, it.sourceName) }
			.let { barcodeRepository.saveAll(it) }
		purgatoryBarcodeRepository.deleteAllByPurgatoryId(id)

		purgatoryBook.resolvedIsbn = newIsbn
		val saved = purgatoryBookRepository.save(purgatoryBook)

		storageService.copyImageTransactional(purgatoryBook.invalidIsbn, newIsbn)

		return saved.toDTO()
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
