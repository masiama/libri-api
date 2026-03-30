package com.libri.api.entity

import jakarta.persistence.*
import java.time.LocalDateTime

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
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CrawlStatus = CrawlStatus.RUNNING,

    @Column(name = "books_found")
    var booksFound: Int = 0,

    @Column(name = "error_message")
    var errorMessage: String? = null,
)
