package com.libri.api.dto

import com.libri.api.entity.Book

data class BookDTO(
	val isbn: String,
	val title: String,
	val authors: List<String>,
	val url: String,
	val sourceName: String,
	val barcodes: List<BarcodeDTO>,
)

fun BookDTO.toEntity() = Book(isbn, title, authors, url, sourceName)
fun Book.toDTO(barcodes: List<BarcodeDTO>) = BookDTO(isbn, title, authors, url, sourceName, barcodes)
