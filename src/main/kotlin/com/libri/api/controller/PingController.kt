package com.libri.api.controller

import com.libri.api.config.AppInfo
import com.libri.api.service.RedisService
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class CrawlerStatus(
    @field:Schema(allowableValues = ["online", "offline"])
    val status: String,
    val version: String?,
)

data class PingResponse(
    val status: String,
    val version: String,
    val crawler: CrawlerStatus,
)

@RestController
@RequestMapping("/api/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
class PingController(
    private val appInfo: AppInfo,
    private val redisService: RedisService,
) {
    @GetMapping("/ping")
    fun ping(): PingResponse {
        val crawlerVersion = runCatching { redisService.crawlerVersion() }.getOrNull()
        val crawler = CrawlerStatus(if (crawlerVersion != null) "online" else "offline", crawlerVersion)
        return PingResponse("ok", appInfo.version, crawler)
    }
}
