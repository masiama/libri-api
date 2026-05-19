package com.libri.api.service

import com.libri.api.dto.CrawlJobDTO
import com.libri.api.entity.CrawlJob
import com.libri.api.entity.CrawlStatus
import com.libri.api.repository.BookRepository
import com.libri.api.repository.CrawlJobRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.jacksonObjectMapper

val objectMapper = jacksonObjectMapper()

data class CrawlerCommand(val crawlId: Long, val source: String) {
	fun toJson(): String = objectMapper.writeValueAsString(this)
}

@Service
class CrawlerService(
	private val crawlJobRepository: CrawlJobRepository,
	private val crawlJobEventService: CrawlJobEventService,
	private val bookRepository: BookRepository,
	private val redisTemplate: StringRedisTemplate,
) {
	private val cacheKeyExistingUrls = "books:existing_urls"

	@EventListener(ApplicationReadyEvent::class)
	fun populateUrlCacheOnStart() {
		val urls = bookRepository.findAllUrls()
		if (urls.isNotEmpty()) {
			redisTemplate.delete(cacheKeyExistingUrls)
			redisTemplate.opsForSet().add(cacheKeyExistingUrls, *urls.toTypedArray())
		}
	}

	fun listJobs(pageable: Pageable): Page<CrawlJobDTO> =
		crawlJobRepository.findAll(pageable).map { it.toDTO() }

	fun isRunning(sourceName: String): Boolean =
		crawlJobRepository.existsByStatusAndSourceName(CrawlStatus.RUNNING, sourceName)

	@Async
	fun run(sourceName: String) {
		val job = crawlJobRepository.save(
			CrawlJob(sourceName = sourceName)
		)
		crawlJobEventService.publishStarted()

		try {
			val command = CrawlerCommand(crawlId = job.id, source = sourceName)
			redisTemplate.opsForList().leftPush("crawler:commands", command.toJson())
		} catch (e: Exception) {
			job.status = CrawlStatus.FAILED
			job.errorMessage = "Failed to dispatch command to message queue: ${e.localizedMessage}"
			crawlJobRepository.save(job).also(crawlJobEventService::publishUpdated)
		}
	}
}
