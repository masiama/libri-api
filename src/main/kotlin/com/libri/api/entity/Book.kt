package com.libri.api.entity

import com.libri.api.converter.StringListConverter
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
)
