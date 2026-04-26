package com.libri.api.controller

import com.libri.api.dto.BookDTO
import com.libri.api.service.BookService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class BookBatchRequest(val books: List<BookDTO>)
data class BookExistsResponse(val exists: Boolean)

@RestController
@RequestMapping("/api/v1/internal")
class InternalController(private val bookService: BookService) {
	@Value($$"${libri.internal.api-key}")
	lateinit var apiKey: String

	@PostMapping("/books/batch")
	fun batchUpsert(
		@RequestHeader("X-Internal-Key") key: String,
		@RequestBody request: BookBatchRequest,
	): ResponseEntity<String> {
		if (key != apiKey) return ResponseEntity.status(401).body("Unauthorized")
		bookService.upsertBatch(request.books)
		return ResponseEntity.ok("Saved ${request.books.size} books")
	}

	@GetMapping("/books/exists")
	fun bookExists(
		@RequestHeader("X-Internal-Key") key: String,
		@RequestParam url: String,
	): ResponseEntity<BookExistsResponse> {
		if (key != apiKey) return ResponseEntity.status(401).build()
		return ResponseEntity.ok(BookExistsResponse(bookService.existsByUrl(url)))
	}
}
