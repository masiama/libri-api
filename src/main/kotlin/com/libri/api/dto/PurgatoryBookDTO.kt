package com.libri.api.dto

import com.libri.api.entity.PurgatoryBook
import java.time.LocalDateTime

data class PurgatoryBookDTO(
	val id: Long,
	val invalidIsbn: String,
	var title: String,
	var authors: List<String> = emptyList(),
	var url: String,
	val sourceName: String,
	val createdAt: LocalDateTime = LocalDateTime.now(),
)

fun PurgatoryBook.toDTO() = PurgatoryBookDTO(id, invalidIsbn, title, authors, url, sourceName, createdAt)
