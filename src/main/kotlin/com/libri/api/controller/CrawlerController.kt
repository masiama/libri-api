package com.libri.api.controller

import com.libri.api.entity.CrawlJob
import com.libri.api.repository.CrawlJobRepository
import com.libri.api.repository.SourceRepository
import com.libri.api.service.CrawlerService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/crawl")
class CrawlerController(
	private val crawlerService: CrawlerService,
	private val crawlJobRepository: CrawlJobRepository,
	private val sourceRepository: SourceRepository,
) {
	@PostMapping
	fun triggerAll(): ResponseEntity<String> {
		if (crawlerService.isRunning()) return ResponseEntity.status(409).body("A crawl is already running")
		crawlerService.runAll()
		return ResponseEntity.accepted().body("Crawl started for all enabled sources")
	}

	@PostMapping("/{source}")
	fun triggerSource(@PathVariable source: String): ResponseEntity<String> {
		if (!sourceRepository.existsById(source)) return ResponseEntity.notFound().build()
		if (crawlerService.isRunning()) return ResponseEntity.status(409).body("A crawl is already running")
		crawlerService.runAsync(source)
		return ResponseEntity.accepted().body("Crawl started for $source")
	}

	@GetMapping("/status")
	fun status(): List<CrawlJob> {
		return crawlJobRepository.findAll(
			PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startedAt"))
		).content
	}
}
