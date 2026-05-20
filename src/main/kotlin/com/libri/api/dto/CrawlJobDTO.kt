package com.libri.api.dto

import com.libri.api.entity.CrawlStatus
import java.time.Instant

data class CrawlJobDTO(
	val id: Long,
	val sourceName: String,
	val startedAt: Instant,
	var finishedAt: Instant?,
	var status: CrawlStatus,
	var booksFound: Int,
	var errorMessage: String?,
)
