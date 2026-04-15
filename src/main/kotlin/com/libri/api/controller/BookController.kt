package com.libri.api.controller

import com.libri.api.entity.Book
import com.libri.api.repository.BookRepository
import com.libri.api.service.StorageService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/books")
class BookController(private val bookRepository: BookRepository, private val storageService: StorageService) {
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

	@PutMapping("/{isbn}")
	fun updateBook(
		@PathVariable isbn: String,
		@RequestPart("book") newBook: Book,
		@RequestPart("file", required = false) image: MultipartFile?
	): ResponseEntity<Book> {
		if (!bookRepository.existsById(isbn)) {
			return ResponseEntity.notFound().build()
		}

		if (image != null && !image.isEmpty) {
			storageService.store(isbn, image)
		}

		val updatedBook = Book(
			isbn = isbn,
			title = newBook.title,
			authors = newBook.authors,
			url = newBook.url,
			sourceName = newBook.sourceName
		)
		bookRepository.save(updatedBook)
		return ResponseEntity.ok(updatedBook)
	}
}
