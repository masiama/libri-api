package com.libri.api.service

import com.libri.api.dto.CrawlJobDTO
import com.libri.api.dto.CrawlJobErrorDTO
import com.libri.api.entity.CrawlJob
import com.libri.api.entity.CrawlStatus
import com.libri.api.repository.BookRepository
import com.libri.api.repository.CrawlJobErrorRepository
import com.libri.api.repository.CrawlJobRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class CrawlerService(
    private val crawlJobRepository: CrawlJobRepository,
    private val crawlJobErrorRepository: CrawlJobErrorRepository,
    private val crawlJobEventService: CrawlJobEventService,
    private val bookRepository: BookRepository,
    private val redisService: RedisService,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun populateUrlCacheOnStart() {
        val urls = bookRepository.findAllUrls()
        if (urls.isNotEmpty()) redisService.resetExistingUrls(urls)
    }

    fun listJobs(pageable: Pageable): Page<CrawlJobDTO> {
        val jobs = crawlJobRepository.findAll(pageable)
        val errorCounts =
            crawlJobErrorRepository
                .countsByCrawlJobIds(jobs.content.map { it.id })
                .associate { it.crawlJobId to it.count }
        return jobs.map { it.toDTO(errorCounts[it.id]) }
    }

    fun isRunning(sourceName: String): Boolean = crawlJobRepository.existsByStatusAndSourceName(CrawlStatus.RUNNING, sourceName)

    fun getRunningSourceNameById(id: Long): String? =
        crawlJobRepository
            .findByIdOrNull(id)
            ?.takeIf { it.status == CrawlStatus.RUNNING }
            ?.sourceName

    fun startCancel(sourceName: String) = redisService.startCancel(sourceName)

    fun getErrorsById(
        id: Long,
        pageable: Pageable,
    ): Page<CrawlJobErrorDTO> = crawlJobErrorRepository.findAllByCrawlJobId(id, pageable).map { it.toDTO() }

    @Async
    fun run(sourceName: String) {
        val job =
            crawlJobRepository.save(
                CrawlJob(sourceName = sourceName),
            )
        crawlJobEventService.publishStarted()

        try {
            redisService.pushCrawlJob(job)
        } catch (e: Exception) {
            job.status = CrawlStatus.FAILED
            job.errorMessage = "Failed to dispatch command to message queue: ${e.localizedMessage}"
            crawlJobRepository.save(job).also(crawlJobEventService::publishUpdated)
        }
    }
}
