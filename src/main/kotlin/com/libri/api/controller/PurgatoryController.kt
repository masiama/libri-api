package com.libri.api.controller

import com.libri.api.dto.PurgatoryBookDTO
import com.libri.api.service.PurgatoryService
import com.libri.api.util.IsbnValidator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class PurgatoryApproveRequest(val isbn: String)

@RestController
@RequestMapping("/api/v1/admin/purgatory")
class PurgatoryController(private val purgatoryService: PurgatoryService) {
	@GetMapping
	fun list(@RequestParam(required = false) filter: String?, pageable: Pageable): Page<PurgatoryBookDTO> =
		purgatoryService.list(pageable, filter)

	@PostMapping("/{id}/approve")
	fun approve(
		@PathVariable id: Long,
		@RequestBody request: PurgatoryApproveRequest,
	): ResponseEntity<PurgatoryBookDTO> {
		if (!IsbnValidator.isValid(request.isbn)) return ResponseEntity.badRequest().build()
		return purgatoryService.approve(id, request.isbn)?.let {
			ResponseEntity.ok(it)
		} ?: ResponseEntity.notFound().build()
	}

	@DeleteMapping("/{id}")
	fun delete(@PathVariable id: Long): ResponseEntity<Void> {
		if (!purgatoryService.markDeleted(id)) return ResponseEntity.notFound().build()
		return ResponseEntity.noContent().build()
	}
}
