package com.libri.api.dto

import com.libri.api.entity.Book
import com.libri.api.entity.PurgatoryBook

data class BookDTO(
	val isbn: String,
	val title: String,
	val authors: List<String>,
	val url: String,
	val sourceName: String,
	val barcodes: List<BarcodeDTO>,
) {
	fun toEntity() = Book(isbn, title, authors, url, sourceName)
	fun toPurgatoryEntity() = PurgatoryBook(
		invalidIsbn = isbn, title = title, authors = authors, url = url, sourceName = sourceName
	)
}
