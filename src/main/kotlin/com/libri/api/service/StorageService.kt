package com.libri.api.service

import com.libri.api.config.StorageConfig
import com.libri.api.exception.ImageNotFoundException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File

@Service
class StorageService(
	private val storageConfig: StorageConfig
) {

	fun store(isbn: String, file: MultipartFile) {
		if (file.isEmpty) {
			throw IllegalArgumentException("Empty file")
		}

		val contentType = file.contentType ?: throw IllegalArgumentException("Missing content type")

		val allowedTypes = setOf(
			"image/jpeg",
			"image/png",
			"image/gif",
			"image/webp"
		)

		if (contentType !in allowedTypes) {
			throw IllegalArgumentException("Unsupported file type: $contentType")
		}

		val destination = storageConfig.resolveImagePath(isbn)

		// Ensure directories exist
		destination.parentFile.mkdirs()

		file.inputStream.use { input ->
			destination.outputStream().use { output ->
				input.copyTo(output)
			}
		}
	}

	fun load(isbn: String): File {
		val file = storageConfig.resolveImagePath(isbn)
		if (!file.exists()) throw ImageNotFoundException(isbn)
		return file
	}
}
