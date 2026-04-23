package com.libri.api.controller

import com.libri.api.repository.CrawlJobRepository
import com.libri.api.repository.SourceRepository
import com.libri.api.service.CrawlJobEventService
import com.libri.api.service.CrawlerService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/admin/crawl")
class CrawlerController(
	private val crawlerService: CrawlerService,
	private val crawlJobRepository: CrawlJobRepository,
	private val sourceRepository: SourceRepository,
	private val crawlJobEventService: CrawlJobEventService,
) {
	@PostMapping
	fun triggerAll(): ResponseEntity<String> {
		val enabledSources = sourceRepository.findAllByEnabledTrue()
		val availableSources = enabledSources.filterNot { crawlerService.isRunning(it.name) }

		if (availableSources.isEmpty()) {
			return ResponseEntity.status(409).body("All enabled sources are already running")
		}

		availableSources.forEach { crawlerService.run(it.name) }
		return ResponseEntity.accepted().body("Crawl started for ${availableSources.size} enabled sources")
	}

	@PostMapping("/{source}")
	fun triggerSource(@PathVariable source: String): ResponseEntity<String> {
		if (!sourceRepository.existsById(source)) return ResponseEntity.notFound().build()
		if (crawlerService.isRunning(source)) return ResponseEntity.status(409)
			.body("A crawl is already running for $source")
		crawlerService.run(source)
		return ResponseEntity.accepted().body("Crawl started for $source")
	}

	@GetMapping
	fun list(@PageableDefault(sort = ["startedAt"], direction = Sort.Direction.DESC) pageable: Pageable) =
		crawlJobRepository.findAll(pageable)

	@GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
	fun events(): SseEmitter = crawlJobEventService.subscribe()
}
