package com.libri.api.controller

import com.libri.api.repository.SourceRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sources")
class SourceController(private val sourceRepository: SourceRepository) {
	@GetMapping
	fun list() = sourceRepository.findAll()
}
