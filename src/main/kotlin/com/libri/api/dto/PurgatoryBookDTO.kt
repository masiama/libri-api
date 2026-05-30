package com.libri.api.dto

import java.time.Instant

data class PurgatoryBookDTO(
    val id: Long,
    val invalidIsbn: String,
    var title: String,
    var authors: List<String> = emptyList(),
    var url: String,
    val sourceName: String,
    val createdAt: Instant = Instant.now(),
)
