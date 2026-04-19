package com.libri.api.service

import com.libri.api.config.StorageConfig
import com.libri.api.exception.ImageNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.io.File

@Service
class StorageService(
	private val storageConfig: StorageConfig
) {

	fun store(isbn: String, file: MultipartFile) {
		storeInternal(storageConfig.resolveImagePath(isbn), file)
	}

	fun storeTransactional(isbn: String, file: MultipartFile) {
		val destination = storageConfig.resolveImagePath(isbn)
		val previousContents = destination.takeIf(File::exists)?.readBytes()

		storeInternal(destination, file)

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return
		}

		TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
			override fun afterCompletion(status: Int) {
				if (status != TransactionSynchronization.STATUS_ROLLED_BACK) {
					return
				}

				destination.parentFile?.mkdirs()
				if (previousContents == null) {
					destination.delete()
					return
				}

				destination.writeBytes(previousContents)
			}
		})
	}

	fun load(isbn: String): File {
		val file = storageConfig.resolveImagePath(isbn)
		if (!file.exists()) throw ImageNotFoundException(isbn)
		return file
	}

	fun deleteTransactional(isbn: String) {
		val destination = storageConfig.resolveImagePath(isbn)
		val previousContents = destination.takeIf(File::exists)?.readBytes()

		destination.delete()

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return
		}

		TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
			override fun afterCompletion(status: Int) {
				if (status != TransactionSynchronization.STATUS_ROLLED_BACK || previousContents == null) {
					return
				}

				destination.parentFile?.mkdirs()
				destination.writeBytes(previousContents)
			}
		})
	}

	private fun storeInternal(destination: File, file: MultipartFile) {
		validateFile(file)

		// Ensure directories exist
		destination.parentFile.mkdirs()

		file.inputStream.use { input ->
			destination.outputStream().use { output ->
				input.copyTo(output)
			}
		}
	}

	private fun validateFile(file: MultipartFile) {
		if (file.isEmpty) {
			throw IllegalArgumentException("Empty file")
		}

		val contentType = file.contentType ?: throw IllegalArgumentException("Missing content type")
		if (contentType !in allowedTypes) {
			throw IllegalArgumentException("Unsupported file type: $contentType")
		}
	}

	companion object {
		private val allowedTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
	}
}
