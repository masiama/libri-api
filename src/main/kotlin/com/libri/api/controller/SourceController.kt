package com.libri.api.controller

import com.libri.api.dto.SourceDTO
import com.libri.api.service.SourceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sources")
class SourceController(private val sourceService: SourceService) {
	@GetMapping
	fun list(): List<SourceDTO> = sourceService.list()
}
