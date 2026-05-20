package com.libri.api.entity

import com.libri.api.dto.CrawlJobDTO
import jakarta.persistence.*
import java.time.Instant

enum class CrawlStatus {
	RUNNING, SUCCESS, FAILED
}

@Entity
@Table(name = "crawl_jobs")
class CrawlJob(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,

	@Column(name = "source_name", nullable = false)
	val sourceName: String,

	@Column(name = "started_at", nullable = false)
	val startedAt: Instant = Instant.now(),

	@Column(name = "finished_at")
	var finishedAt: Instant? = null,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: CrawlStatus = CrawlStatus.RUNNING,

	@Column(name = "books_found")
	var booksFound: Int = 0,

	@Column(name = "error_message")
	var errorMessage: String? = null,

	@Column(name = "last_heartbeat_at")
	var lastHeartbeatAt: Instant? = null
) {
	fun toDTO() = CrawlJobDTO(id, sourceName, startedAt, finishedAt, status, booksFound, errorMessage)
}
