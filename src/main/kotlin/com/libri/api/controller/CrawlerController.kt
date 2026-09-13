package com.libri.api.controller

import com.libri.api.dto.CrawlJobDTO
import com.libri.api.dto.CrawlJobErrorDTO
import com.libri.api.service.CrawlJobEventService
import com.libri.api.service.CrawlerService
import com.libri.api.service.SourceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/admin/crawl")
class CrawlerController(
    private val crawlerService: CrawlerService,
    private val crawlJobEventService: CrawlJobEventService,
    private val sourceService: SourceService,
) {
    @Operation(operationId = "triggerAllCrawls")
    @ApiResponses(
        ApiResponse(responseCode = "202", content = [Content(schema = Schema(type = "string"))]),
        ApiResponse(responseCode = "409", content = [Content(schema = Schema(type = "string"))]),
    )
    @PostMapping(produces = [MediaType.TEXT_PLAIN_VALUE])
    fun triggerAll(): ResponseEntity<String> {
        val availableSources = sourceService.listEnabledNotRunning()

        if (availableSources.isEmpty()) {
            return ResponseEntity.status(409).body("All enabled sources are already running")
        }

        availableSources.forEach { crawlerService.run(it.name) }
        return ResponseEntity.accepted().body("Crawl started for ${availableSources.size} enabled sources")
    }

    @Operation(operationId = "triggerSourceCrawl")
    @ApiResponses(
        ApiResponse(responseCode = "202", content = [Content(schema = Schema(type = "string"))]),
        ApiResponse(responseCode = "404", content = [Content()]),
        ApiResponse(responseCode = "409", content = [Content(schema = Schema(type = "string"))]),
    )
    @PostMapping("/{source}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun triggerSource(
        @PathVariable source: String,
    ): ResponseEntity<String> {
        if (!sourceService.exists(source)) return ResponseEntity.notFound().build()
        if (crawlerService.isRunning(source)) {
            return ResponseEntity
                .status(409)
                .body("A crawl is already running for $source")
        }
        crawlerService.run(source)
        return ResponseEntity.accepted().body("Crawl started for $source")
    }

    @Operation(operationId = "listCrawlJobs")
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun list(
        @ParameterObject
        @PageableDefault(sort = ["startedAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): Page<CrawlJobDTO> = crawlerService.listJobs(pageable)

    @Operation(hidden = true)
    @GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun events(): SseEmitter = crawlJobEventService.subscribe()

    @Operation(operationId = "cancelCrawlJob")
    @ApiResponses(
        ApiResponse(responseCode = "202", content = [Content(schema = Schema(type = "string"))]),
        ApiResponse(responseCode = "404", content = [Content()]),
    )
    @PostMapping("/{id}/cancel", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun cancelJob(
        @PathVariable id: Long,
    ): ResponseEntity<String> {
        val sourceName = crawlerService.getRunningSourceNameById(id) ?: return ResponseEntity.notFound().build()
        crawlerService.startCancel(sourceName)
        return ResponseEntity.accepted().body("Cancel request for $sourceName sent")
    }

    @Operation(operationId = "listCrawlJobErrors")
    @GetMapping("/{id}/errors", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getErrors(
        @PathVariable id: Long,
        @ParameterObject pageable: Pageable,
    ): Page<CrawlJobErrorDTO> = crawlerService.getErrorsById(id, pageable)
}
