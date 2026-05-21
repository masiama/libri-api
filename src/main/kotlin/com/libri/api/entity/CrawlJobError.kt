package com.libri.api.entity

import com.libri.api.dto.CrawlJobErrorDTO
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "crawl_job_errors")
class CrawlJobError(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,

	@Column(name = "crawl_job_id", nullable = false)
	val crawlJobId: Long,

	@Column(nullable = false)
	val message: String,

	@Column
	val url: String? = null,

	@Column(name = "occurred_at", nullable = false)
	val occurredAt: Instant = Instant.now()
) {
	fun toDTO() = CrawlJobErrorDTO(id, message, url, occurredAt)
}
