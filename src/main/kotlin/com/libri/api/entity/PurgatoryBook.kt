package com.libri.api.entity

import com.libri.api.converter.StringListConverter
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "purgatory")
class PurgatoryBook(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,

	@Column(name = "invalid_isbn", nullable = false)
	val invalidIsbn: String,

	@Column(nullable = false)
	var title: String,

	@Column(columnDefinition = "jsonb", nullable = false)
	@Convert(converter = StringListConverter::class)
	var authors: List<String> = emptyList(),

	@Column(nullable = false)
	var url: String,

	@Column(name = "source_name", nullable = false)
	val sourceName: String,

	@Column(name = "resolved_isbn")
	var resolvedIsbn: String? = null,

	@Column(name = "created_at", nullable = false, updatable = false)
	val createdAt: LocalDateTime = LocalDateTime.now(),

	@Column(nullable = false)
	var deleted: Boolean = false
)
