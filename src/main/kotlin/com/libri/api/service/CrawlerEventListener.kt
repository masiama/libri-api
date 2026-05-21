package com.libri.api.service

import com.libri.api.config.fromJson
import com.libri.api.dto.BookDTO
import com.libri.api.dto.CrawlerEvent
import com.libri.api.entity.CrawlStatus
import com.libri.api.repository.CrawlJobRepository
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.concurrent.thread

@Component
class CrawlerEventListener(
	private val redisService: RedisService,
	private val crawlJobRepository: CrawlJobRepository,
	private val crawlJobEventService: CrawlJobEventService,
	private val bookService: BookService,
	private val progressTracker: CrawlProgressTracker
) {
	private var running = true
	private val batchSizeThreshold = 300
	private val bookBuffer = mutableListOf<BookDTO>()

	@PostConstruct
	fun startListening() {
		thread(start = true, isDaemon = true, name = "crawler-event-consumer") {
			while (running) {
				try {
					val result = redisService.readCrawlEvent() ?: continue
					val event = result.fromJson<CrawlerEvent>()
					processUnifiedEvent(event)
				} catch (_: Exception) {
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
				val job = crawlJobRepository.findById(event.crawlId).orElse(null) ?: return
				val updated = if (job.errorMessage.isNullOrBlank()) event.error
				else "${job.errorMessage}\n${event.error}"
				job.errorMessage = updated.take(10000)
				crawlJobRepository.save(job).also(crawlJobEventService::publishUpdated)
			}

			is CrawlerEvent.CompletedEvent -> {
				flushBooks()

				val job = crawlJobRepository.findById(event.crawlId).orElse(null) ?: return
				job.status = CrawlStatus.SUCCESS
				job.booksFound = event.booksFound
				job.finishedAt = Instant.now()
				crawlJobRepository.save(job).also(crawlJobEventService::publishUpdated)
				progressTracker.clear(job.id)
			}

			is CrawlerEvent.ErrorEvent -> {
				flushBooks()

				val job = crawlJobRepository.findById(event.crawlId).orElse(null) ?: return
				job.status = CrawlStatus.FAILED
				job.errorMessage = event.error.take(2000)
				job.finishedAt = Instant.now()
				crawlJobRepository.save(job).also(crawlJobEventService::publishUpdated)
				progressTracker.clear(job.id)
			}
		}

	}

	private fun flushBooks() {
		val batchToSave = synchronized(bookBuffer) {
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
