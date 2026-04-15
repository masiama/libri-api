package com.libri.api.controller

import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class PingController(private val buildProperties: BuildProperties) {
	@GetMapping("/ping")
	fun ping(): Map<String, String> =
		mapOf("status" to "ok", "version" to (buildProperties.version ?: "unknown"))
}
