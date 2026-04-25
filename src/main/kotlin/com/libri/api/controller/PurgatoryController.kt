package com.libri.api.controller

import com.libri.api.entity.PurgatoryBook
import com.libri.api.repository.PurgatoryBookRepository
import com.libri.api.service.PurgatoryService
import com.libri.api.util.IsbnValidator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/purgatory")
class PurgatoryController(
	private val purgatoryBookRepository: PurgatoryBookRepository,
	private val purgatoryService: PurgatoryService,
) {
	@GetMapping
	fun list(@RequestParam(required = false) filter: String?, pageable: Pageable): Page<PurgatoryBook> {
		if (filter.isNullOrBlank()) {
			return purgatoryBookRepository.findAllByResolvedIsbnIsNullAndDeletedFalse(pageable)
		}
		return purgatoryBookRepository.findAllByTitleAndResolvedIsbnIsNullAndDeletedFalse(filter, pageable)
	}

	@PostMapping("/{id}/approve")
	fun approve(
		@PathVariable id: Long,
		@RequestBody isbn: String,
	): ResponseEntity<PurgatoryBook> {
		if (!IsbnValidator.isValid(isbn)) {
			return ResponseEntity.badRequest().build()
		}
		val result = purgatoryService.approve(id, isbn) ?: return ResponseEntity.notFound().build()
		return ResponseEntity.ok(result)
	}

	@DeleteMapping("/{id}")
	fun delete(@PathVariable id: Long): ResponseEntity<Void> {
		if (!purgatoryService.markDeleted(id)) return ResponseEntity.notFound().build()
		return ResponseEntity.noContent().build()
	}
}
