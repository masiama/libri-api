package com.libri.api.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.io.File
import java.security.MessageDigest

@Configuration
class StorageConfig {
    @Value("\${storage.images-dir}")
    lateinit var imagesDir: String

    @PostConstruct
    fun validate() {
        val dir = File(imagesDir)
        if (!dir.exists() || !dir.isDirectory) {
            throw IllegalStateException("Images directory not found: $imagesDir — set IMAGES_DIR in your environment")
        }
    }

    fun resolveImagePath(isbn: String): File {
        val hash = MessageDigest.getInstance("MD5")
            .digest(isbn.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val shard1 = hash.substring(0, 2)
        val shard2 = hash.substring(2, 4)

        return File(imagesDir, "$shard1/$shard2/$isbn.jpg")
    }
}
