package com.libri.api.controller

import com.libri.api.dto.CrawlJobDTO
import com.libri.api.service.CrawlJobEventService
import com.libri.api.service.CrawlerService
import com.libri.api.service.SourceService
import org.springframework.data.domain.Page
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
	private val crawlJobEventService: CrawlJobEventService,
	private val sourceService: SourceService,
) {
	@PostMapping
	fun triggerAll(): ResponseEntity<String> {
		val availableSources = sourceService.listEnabledNotRunning()

		if (availableSources.isEmpty()) {
			return ResponseEntity.status(409).body("All enabled sources are already running")
		}

		availableSources.forEach { crawlerService.run(it.name) }
		return ResponseEntity.accepted().body("Crawl started for ${availableSources.size} enabled sources")
	}

	@PostMapping("/{source}")
	fun triggerSource(@PathVariable source: String): ResponseEntity<String> {
		if (!sourceService.exists(source)) return ResponseEntity.notFound().build()
		if (crawlerService.isRunning(source)) return ResponseEntity.status(409)
			.body("A crawl is already running for $source")
		crawlerService.run(source)
		return ResponseEntity.accepted().body("Crawl started for $source")
	}

	@GetMapping
	fun list(
		@PageableDefault(sort = ["startedAt"], direction = Sort.Direction.DESC) pageable: Pageable
	): Page<CrawlJobDTO> = crawlerService.listJobs(pageable)

	@GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
	fun events(): SseEmitter = crawlJobEventService.subscribe()
}
