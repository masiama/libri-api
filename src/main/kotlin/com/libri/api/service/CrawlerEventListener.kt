package com.libri.api.service

import com.libri.api.dto.BookDTO
import com.libri.api.dto.CrawlerEvent
import com.libri.api.entity.CrawlStatus
import com.libri.api.repository.CrawlJobRepository
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

@Component
class CrawlerEventListener(
	private val redisTemplate: StringRedisTemplate,
	private val crawlJobRepository: CrawlJobRepository,
	private val crawlJobEventService: CrawlJobEventService,
	private val bookService: BookService,
) {
	private var running = true
	private val batchSizeThreshold = 300
	private val bookBuffer = mutableListOf<BookDTO>()
	private var lastDbProgressWrite = ConcurrentHashMap<Long, Long>()
	private val dbWriteInternalMs = 10_000L

	@PostConstruct
	fun startListening() {
		thread(start = true, isDaemon = true, name = "crawler-event-consumer") {
			while (running) {
				try {
					val result = redisTemplate.opsForList().rightPop("crawl:events", Duration.ofSeconds(5)) ?: continue
					val event = objectMapper.readValue(result, CrawlerEvent::class.java)
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

				val now = System.currentTimeMillis()
				val last = lastDbProgressWrite[event.crawlId] ?: 0L
				if (now - last >= dbWriteInternalMs) {
					lastDbProgressWrite[event.crawlId] = now
					crawlJobRepository.updateBooksFound(event.crawlId, event.booksFound)
				}
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
				job.finishedAt = LocalDateTime.now()
				crawlJobRepository.save(job).also(crawlJobEventService::publishUpdated)
			}

			is CrawlerEvent.ErrorEvent -> {
				flushBooks()

				val job = crawlJobRepository.findById(event.crawlId).orElse(null) ?: return
				job.status = CrawlStatus.FAILED
				job.errorMessage = event.error.take(2000)
				job.finishedAt = LocalDateTime.now()
				crawlJobRepository.save(job).also(crawlJobEventService::publishUpdated)
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
