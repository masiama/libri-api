package com.libri.api.controller

import com.libri.api.config.StorageConfig
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/images")
class ImageController(private val storageConfig: StorageConfig) {
	@GetMapping("/{isbn}.jpg")
	fun getImage(@PathVariable isbn: String): ResponseEntity<Resource> {
		val file = storageConfig.resolveImagePath(isbn)
		if (!file.exists()) return ResponseEntity.notFound().build()
		return ResponseEntity.ok()
			.contentType(MediaType.IMAGE_JPEG)
			.body(FileSystemResource(file))
	}
}
