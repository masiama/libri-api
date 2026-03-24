package com.libri.api.controller

import com.libri.api.entity.Book
import com.libri.api.repository.BookRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/books")
class BookController(private val bookRepository: BookRepository) {
    @GetMapping
    fun list(pageable: Pageable): Page<Book> = bookRepository.findAll(pageable)

    @GetMapping("/{isbn}")
    fun getByIsbn(@PathVariable isbn: String): ResponseEntity<Book> {
        val book = bookRepository.findByIdOrNull(isbn) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(book)
    }
}
