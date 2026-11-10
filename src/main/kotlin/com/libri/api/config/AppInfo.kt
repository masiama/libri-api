package com.libri.api.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AppInfo(
    @Value($$"${APP_VERSION:#{null}}")
    private val envVersion: String?,
) {
    val version: String = resolveVersion()

    private fun resolveVersion(): String = if (!envVersion.isNullOrBlank()) envVersion else resolveGitVersion()

    private fun resolveGitVersion(): String {
        val tag =
            runCatching { runCommand("git", "describe", "--tags", "--abbrev=0") }.getOrDefault("").ifBlank { "dev" }
        val sha = runCatching { runCommand("git", "rev-parse", "--short", "HEAD") }.getOrDefault("")
        return if (sha.isBlank()) tag else "$tag-$sha"
    }

    private fun runCommand(vararg command: String): String =
        ProcessBuilder(*command)
            .directory(java.io.File(System.getProperty("user.dir")))
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()
}
