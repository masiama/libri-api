package com.libri.api.controller

import com.libri.api.config.AppInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class PingResponse(val status: String, val version: String)

@RestController
@RequestMapping("/api/v1")
class PingController(private val appInfo: AppInfo) {
	@GetMapping("/ping")
	fun ping(): PingResponse = PingResponse("ok", appInfo.version)
}
