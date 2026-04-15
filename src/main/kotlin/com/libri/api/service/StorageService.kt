package com.libri.api.service

import com.libri.api.config.StorageConfig
import com.libri.api.exception.ImageNotFoundException
import org.springframework.stereotype.Service
import java.io.File

@Service
class StorageService(
	private val storageConfig: StorageConfig
) {
	fun load(isbn: String): File {
		val file = storageConfig.resolveImagePath(isbn)
		if (!file.exists()) throw ImageNotFoundException(isbn)
		return file
	}
}
