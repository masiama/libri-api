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

    private fun resolveGitVersion(): String =
        try {
            runCommand("git", "describe", "--tags", "--exact-match").ifBlank { fallbackToSha() }
        } catch (_: Exception) {
            fallbackToSha()
        }

    private fun fallbackToSha(): String =
        try {
            "dev-${runCommand("git", "rev-parse", "--short", "HEAD")}"
        } catch (_: Exception) {
            "dev"
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
