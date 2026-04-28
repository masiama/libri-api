package com.libri.api.entity

import com.libri.api.converter.StringListConverter
import com.libri.api.dto.BarcodeDTO
import com.libri.api.dto.BookDTO
import jakarta.persistence.*

@Entity
@Table(name = "books")
class Book(
	@Id
	val isbn: String,

	@Column(nullable = false)
	val title: String,

	@Column(columnDefinition = "jsonb", nullable = false)
	@Convert(converter = StringListConverter::class)
	val authors: List<String> = emptyList(),

	@Column(nullable = false)
	val url: String,

	@Column(name = "source_name", nullable = false)
	val sourceName: String,
) {
	fun toDTO(barcodes: List<BarcodeDTO>) = BookDTO(isbn, title, authors, url, sourceName, barcodes)
}
