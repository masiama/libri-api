package com.libri.api.dto

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
