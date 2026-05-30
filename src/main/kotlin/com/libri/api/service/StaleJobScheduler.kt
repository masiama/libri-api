package com.libri.api.service

import com.libri.api.entity.CrawlStatus
import com.libri.api.repository.CrawlJobRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class StaleJobScheduler(
    private val crawlJobRepository: CrawlJobRepository,
    private val crawlJobEventService: CrawlJobEventService,
    private val redisService: RedisService,
    private val progressTracker: CrawlProgressTracker,
) {
    private val logger = LoggerFactory.getLogger(StaleJobScheduler::class.java)

    @Scheduled(fixedDelay = 300_000)
    fun killStaleJobs() {
        val now = Instant.now()
        val threshold = now.minus(2, ChronoUnit.MINUTES)
        val startedThreshold = now.minus(5, ChronoUnit.MINUTES)

        val staleJobs = crawlJobRepository.findStaleJobs(threshold, startedThreshold)
        if (staleJobs.isEmpty()) return

        staleJobs.forEach { job ->
            job.status = CrawlStatus.FAILED
            job.errorMessage = "Job timed out — no heartbeat received"
            job.finishedAt = Instant.now()
            job.booksFound = progressTracker.get(job.id) ?: job.booksFound
            crawlJobRepository.save(job).also(crawlJobEventService::publishUpdated)
            redisService.deleteSourceLock(job.sourceName)
            progressTracker.clear(job.id)
            logger.info("Killed stale job ${job.id} for source ${job.sourceName}, lock released")
        }
    }
}
