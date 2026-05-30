package com.libri.api.dto

import java.time.Instant

data class CrawlJobErrorDTO(
    val id: Long,
    val message: String,
    val url: String?,
    val occurredAt: Instant,
)
