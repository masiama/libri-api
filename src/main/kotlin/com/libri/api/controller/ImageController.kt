package com.libri.api.controller

import com.libri.api.service.StorageService
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files

@RestController
@RequestMapping("/api/v1/images")
class ImageController(private val storageService: StorageService) {
	@GetMapping("/{isbn}.*")
	fun getImage(@PathVariable isbn: String): ResponseEntity<Resource> {
		val file = storageService.load(isbn)
		val resource = UrlResource(file.toURI())

		val contentType = Files.probeContentType(file.toPath())
			?: "application/octet-stream"

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(contentType))
			.body(resource)
	}
}
