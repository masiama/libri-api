package com.libri.api.controller

import com.libri.api.entity.Book
import com.libri.api.repository.BookRepository
import com.libri.api.service.BookService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/books")
class BookController(private val bookRepository: BookRepository, private val bookService: BookService) {
	@GetMapping
	fun list(@RequestParam(required = false) filter: String?, pageable: Pageable): Page<Book> {
		if (filter.isNullOrBlank()) {
			return bookRepository.findAll(pageable)
		}
		return bookRepository.searchByTitle(filter, pageable)
	}

	@GetMapping("/{isbn}")
	fun getByIsbn(@PathVariable isbn: String): ResponseEntity<Book> {
		val book = bookRepository.findByIdOrNull(isbn) ?: return ResponseEntity.notFound().build()
		return ResponseEntity.ok(book)
	}

	@PostMapping
	fun createBook(
		@RequestPart("book") newBook: Book,
		@RequestPart("file") image: MultipartFile
	): ResponseEntity<Book> {
		val createdBook = bookService.createBook(newBook, image) ?: return ResponseEntity.status(409).build()
		return ResponseEntity.ok(createdBook)
	}

	@PutMapping("/{isbn}")
	fun updateBook(
		@PathVariable isbn: String,
		@RequestPart("book") newBook: Book,
		@RequestPart("file", required = false) image: MultipartFile?
	): ResponseEntity<Book> {
		val updatedBook = bookService.updateBook(isbn, newBook, image) ?: return ResponseEntity.notFound().build()
		return ResponseEntity.ok(updatedBook)
	}

	@DeleteMapping("/{isbn}")
	fun deleteBook(@PathVariable isbn: String): ResponseEntity<Void> {
		if (!bookService.deleteBook(isbn)) {
			return ResponseEntity.notFound().build()
		}

		return ResponseEntity.noContent().build()
	}
}
