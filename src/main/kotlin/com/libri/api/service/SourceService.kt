package com.libri.api.service

import com.libri.api.dto.SourceDTO
import com.libri.api.entity.Source
import com.libri.api.repository.SourceRepository
import org.springframework.stereotype.Service

@Service
class SourceService(private val sourceRepository: SourceRepository, private val crawlerService: CrawlerService) {
	fun list(): List<SourceDTO> = sourceRepository.findAll().map { it.toDTO() }

	fun listEnabledNotRunning(): List<Source> {
		val enabledSources = sourceRepository.findAllByEnabledTrue()
		return enabledSources.filterNot { crawlerService.isRunning(it.name) }
	}

	fun exists(sourceName: String): Boolean = sourceRepository.existsById(sourceName)
}
