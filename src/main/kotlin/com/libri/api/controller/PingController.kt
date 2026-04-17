package com.libri.api.controller

import com.libri.api.config.AppInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class PingController(private val appInfo: AppInfo) {
	@GetMapping("/ping")
	fun ping(): Map<String, String> = mapOf("status" to "ok", "version" to appInfo.version)
}
