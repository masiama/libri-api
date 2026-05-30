package com.libri.api.service

import com.libri.api.config.fromJson
import com.libri.api.dto.BookDTO
import com.libri.api.dto.CrawlerEvent
import com.libri.api.entity.CrawlJobError
import com.libri.api.entity.CrawlStatus
import com.libri.api.repository.CrawlJobErrorRepository
import com.libri.api.repository.CrawlJobRepository
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.concurrent.thread

@Component
class CrawlerEventListener(
    private val redisService: RedisService,
    private val crawlJobRepository: CrawlJobRepository,
    private val crawlJobErrorRepository: CrawlJobErrorRepository,
    private val crawlJobEventService: CrawlJobEventService,
    private val bookService: BookService,
    private val progressTracker: CrawlProgressTracker,
) {
    private var running = true
    private val batchSizeThreshold = 300
    private val bookBuffer = mutableListOf<BookDTO>()
    private val logger = LoggerFactory.getLogger(CrawlerEventListener::class.java)

    @PostConstruct
    fun startListening() {
        thread(start = true, isDaemon = true, name = "crawler-event-consumer") {
            while (running) {
                try {
                    val result = redisService.readCrawlEvent() ?: continue
                    val event = result.fromJson<CrawlerEvent>()
                    processUnifiedEvent(event)
                } catch (e: Exception) {
                    logger.error("Error while reading event", e)
                    Thread.sleep(2000)
                }
            }
        }
    }

    fun processUnifiedEvent(event: CrawlerEvent) {
        when (event) {
            is CrawlerEvent.BookEvent -> {
                bookBuffer.add(event.book)
                if (bookBuffer.size >= batchSizeThreshold) {
                    flushBooks()
                }
            }

            is CrawlerEvent.ProgressEvent -> {
                crawlJobEventService.publishProgress(event.crawlId, event.booksFound)
                progressTracker.update(event.crawlId, event.booksFound)
            }

            is CrawlerEvent.CrawlErrorEvent -> {
                crawlJobErrorRepository.save(
                    CrawlJobError(crawlJobId = event.crawlId, message = event.error, url = event.url),
                )
            }

            is CrawlerEvent.CompletedEvent -> {
                flushBooks()

                val job = crawlJobRepository.findById(event.crawlId).orElse(null) ?: return
                val errorCount = crawlJobErrorRepository.countByCrawlJobId(event.crawlId)
                job.status = CrawlStatus.SUCCESS
                job.booksFound = event.booksFound
                job.finishedAt = Instant.now()
                crawlJobRepository.save(job).also {
                    crawlJobEventService.publishUpdated(it, errorCount)
                }
                progressTracker.clear(job.id)
            }

            is CrawlerEvent.ErrorEvent -> {
                flushBooks()

                val job = crawlJobRepository.findById(event.crawlId).orElse(null) ?: return
                val errorCount = crawlJobErrorRepository.countByCrawlJobId(event.crawlId)
                job.status = CrawlStatus.FAILED
                job.errorMessage = event.error.take(2000)
                job.finishedAt = Instant.now()
                crawlJobRepository.save(job).also {
                    crawlJobEventService.publishUpdated(it, errorCount)
                }
                progressTracker.clear(job.id)
            }

            is CrawlerEvent.CancelledEvent -> {
                flushBooks()

                val job = crawlJobRepository.findById(event.crawlId).orElse(null) ?: return
                val errorCount = crawlJobErrorRepository.countByCrawlJobId(event.crawlId)
                job.status = CrawlStatus.CANCELLED
                job.booksFound = event.booksFound
                job.finishedAt = Instant.now()
                crawlJobRepository.save(job).also {
                    crawlJobEventService.publishUpdated(it, errorCount)
                }
                progressTracker.clear(job.id)
                redisService.stopCancel(job.sourceName)
            }

            is CrawlerEvent.HeartbeatEvent -> {
                crawlJobRepository.updateHeartbeat(event.crawlId)
            }
        }
    }

    private fun flushBooks() {
        val batchToSave =
            synchronized(bookBuffer) {
                if (bookBuffer.isEmpty()) return
                val copy = ArrayList(bookBuffer)
                bookBuffer.clear()
                copy
            }

        bookService.upsertBatch(batchToSave)
    }

    @PreDestroy
    fun stop() {
        running = false
    }
}
