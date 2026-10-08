package com.libri.api.controller

import com.libri.api.config.ImageSide
import com.libri.api.service.StorageService
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files

@RestController
@RequestMapping("/api/v1/images")
class ImageController(
    private val storageService: StorageService,
) {
    @GetMapping("/{isbn}.jpg")
    fun getImage(
        @PathVariable isbn: String,
        @RequestParam(defaultValue = "front") side: String,
    ): ResponseEntity<Resource> {
        val imageSide = ImageSide.fromValue(side) ?: return ResponseEntity.badRequest().build()
        val file = storageService.load(isbn, imageSide)
        val resource = UrlResource(file.toURI())

        val contentType =
            Files.probeContentType(file.toPath())
                ?: "application/octet-stream"

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource)
    }
}
