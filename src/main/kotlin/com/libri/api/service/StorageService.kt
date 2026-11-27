package com.libri.api.service

import com.libri.api.config.ImageSide
import com.libri.api.config.StorageConfig
import com.libri.api.exception.ImageNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.io.File

@Service
class StorageService(
    private val storageConfig: StorageConfig,
) {
    fun store(
        isbn: String,
        file: MultipartFile,
    ) {
        storeInternal(storageConfig.resolveImagePath(isbn), file)
    }

    fun storeTransactional(
        isbn: String,
        file: MultipartFile,
        side: ImageSide = ImageSide.FRONT,
    ) {
        val destination = storageConfig.resolveImagePath(isbn, side)
        val previousContents = destination.takeIf(File::exists)?.readBytes()

        storeInternal(destination, file)
        registerRollback(destination, previousContents)
    }

    fun load(
        isbn: String,
        side: ImageSide = ImageSide.FRONT,
    ): File {
        val file = storageConfig.resolveImagePath(isbn, side)
        if (!file.exists()) throw ImageNotFoundException(isbn)
        return file
    }

    fun deleteTransactional(isbn: String) {
        ImageSide.entries.forEach { side ->
            val destination = storageConfig.resolveImagePath(isbn, side)
            val previousContents = destination.takeIf(File::exists)?.readBytes()

            destination.delete()
            registerRollback(destination, previousContents)
        }
    }

    fun copyImageTransactional(
        fromIsbn: String,
        toIsbn: String,
    ) {
        ImageSide.entries.forEach { side ->
            val source = storageConfig.resolveImagePath(fromIsbn, side)
            // Back and spine are optional; a missing front still fails loudly
            if (side != ImageSide.FRONT && !source.exists()) return@forEach

            val destination = storageConfig.resolveImagePath(toIsbn, side)
            val previousContents = destination.takeIf(File::exists)?.readBytes()

            destination.parentFile?.mkdirs()
            source.copyTo(destination, overwrite = true)

            registerRollback(destination, previousContents)
        }
    }

    private fun storeInternal(
        destination: File,
        file: MultipartFile,
    ) {
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

    private fun registerRollback(
        destination: File,
        previousContents: ByteArray?,
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    if (status != TransactionSynchronization.STATUS_ROLLED_BACK) return

                    destination.parentFile?.mkdirs()
                    if (previousContents == null) {
                        destination.delete()
                        return
                    }
                    destination.writeBytes(previousContents)
                }
            },
        )
    }

    companion object {
        private val allowedTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/avif")
    }
}
