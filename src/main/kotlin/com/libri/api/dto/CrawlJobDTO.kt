package com.libri.api.dto

import com.libri.api.entity.CrawlJob
import com.libri.api.entity.CrawlStatus
import java.time.LocalDateTime

data class CrawlJobDTO(
	val id: Long,
	val sourceName: String,
	val startedAt: LocalDateTime,
	var finishedAt: LocalDateTime?,
	var status: CrawlStatus,
	var booksFound: Int,
	var errorMessage: String?,
)

fun CrawlJob.toDTO() = CrawlJobDTO(id, sourceName, startedAt, finishedAt, status, booksFound, errorMessage)
