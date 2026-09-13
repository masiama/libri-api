package com.libri.api.controller

import com.libri.api.dto.BookDTO
import com.libri.api.service.BookService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

data class BookBatchDeleteRequest(
    val isbns: List<String>,
)

@RestController
@RequestMapping("/api/v1/books", produces = [MediaType.APPLICATION_JSON_VALUE])
class BookController(
    private val bookService: BookService,
) {
    @Operation(operationId = "listBooks")
    @GetMapping
    fun list(
        @RequestParam(required = false) filter: String?,
        @ParameterObject pageable: Pageable,
    ): Page<BookDTO> = bookService.list(pageable, filter)

    @Operation(operationId = "getBookByIsbn")
    @ApiResponses(
        ApiResponse(responseCode = "200"),
        ApiResponse(responseCode = "404", content = [Content()]),
    )
    @GetMapping("/{code}")
    fun getByIsbn(
        @PathVariable code: String,
    ): ResponseEntity<BookDTO> =
        bookService.getByCode(code)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
}

@RestController
@RequestMapping("/api/v1/admin/books", produces = [MediaType.APPLICATION_JSON_VALUE])
class AdminBookController(
    private val bookService: BookService,
) {
    @Operation(operationId = "createBook")
    @ApiResponses(
        ApiResponse(responseCode = "200"),
        ApiResponse(responseCode = "404", content = [Content()]),
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createBook(
        @RequestPart("book") newBook: BookDTO,
        @RequestPart("file") image: MultipartFile,
    ): ResponseEntity<BookDTO> =
        bookService.createBook(newBook, image)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()

    @Operation(operationId = "updateBook")
    @ApiResponses(
        ApiResponse(responseCode = "200"),
        ApiResponse(responseCode = "404", content = [Content()]),
    )
    @PutMapping("/{isbn}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateBook(
        @PathVariable isbn: String,
        @RequestPart("book") newBook: BookDTO,
        @RequestPart("file", required = false) image: MultipartFile?,
    ): ResponseEntity<BookDTO> =
        bookService.updateBook(isbn, newBook, image)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()

    @Operation(operationId = "deleteBook")
    @ApiResponses(
        ApiResponse(responseCode = "204", content = [Content()]),
        ApiResponse(responseCode = "404", content = [Content()]),
    )
    @DeleteMapping("/{isbn}")
    fun deleteBook(
        @PathVariable isbn: String,
    ): ResponseEntity<Void> =
        if (bookService.deleteBook(isbn)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }

    @Operation(operationId = "deleteBooksBulk")
    @ApiResponses(
        ApiResponse(responseCode = "204", content = [Content()]),
        ApiResponse(responseCode = "404", content = [Content()]),
    )
    @DeleteMapping("/bulk")
    fun deleteBooks(
        @RequestBody request: BookBatchDeleteRequest,
    ): ResponseEntity<Void> =
        if (bookService.deleteBooks(request.isbns)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
}
