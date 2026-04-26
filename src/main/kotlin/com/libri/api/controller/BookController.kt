package com.libri.api.controller

import com.libri.api.dto.BookDTO
import com.libri.api.service.BookService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

data class BookBatchDeleteRequest(val isbns: List<String>)

@RestController
@RequestMapping("/api/v1/books")
class BookController(private val bookService: BookService) {
	@GetMapping
	fun list(@RequestParam(required = false) filter: String?, pageable: Pageable): Page<BookDTO> =
		bookService.list(pageable, filter)

	@GetMapping("/{isbn}")
	fun getByIsbn(@PathVariable isbn: String): ResponseEntity<BookDTO> =
		bookService.getByIsbn(isbn)?.let {
			ResponseEntity.ok(it)
		} ?: ResponseEntity.notFound().build()
}

@RestController
@RequestMapping("/api/v1/admin/books")
class AdminBookController(private val bookService: BookService) {
	@PostMapping
	fun createBook(
		@RequestPart("book") newBook: BookDTO,
		@RequestPart("file") image: MultipartFile
	): ResponseEntity<BookDTO> =
		bookService.createBook(newBook, image)?.let {
			ResponseEntity.ok(it)
		} ?: ResponseEntity.notFound().build()

	@PutMapping("/{isbn}")
	fun updateBook(
		@PathVariable isbn: String,
		@RequestPart("book") newBook: BookDTO,
		@RequestPart("file", required = false) image: MultipartFile?
	): ResponseEntity<BookDTO> =
		bookService.updateBook(isbn, newBook, image)?.let {
			ResponseEntity.ok(it)
		} ?: ResponseEntity.notFound().build()

	@DeleteMapping("/{isbn}")
	fun deleteBook(@PathVariable isbn: String): ResponseEntity<Void> =
		if (bookService.deleteBook(isbn)) ResponseEntity.noContent().build()
		else ResponseEntity.notFound().build()

	@DeleteMapping("/bulk")
	fun deleteBooks(@RequestBody request: BookBatchDeleteRequest): ResponseEntity<Void> =
		if (bookService.deleteBooks(request.isbns)) ResponseEntity.noContent().build()
		else ResponseEntity.notFound().build()
}
