package com.libri.api.service

import com.libri.api.entity.CrawlJob
import com.libri.api.entity.CrawlStatus
import com.libri.api.repository.CrawlJobRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

private val objectMapper = jacksonObjectMapper()

@Service
class CrawlerService(
	private val crawlJobRepository: CrawlJobRepository,
	private val crawlJobEventService: CrawlJobEventService,
) {
	@Value($$"${libri.crawler.binary-path}")
	lateinit var crawlerBinaryPath: String

	@Value($$"${server.port}")
	lateinit var serverPort: String

	private fun buildApiUrl() = "http://localhost:$serverPort"

	fun isRunning(sourceName: String): Boolean =
		crawlJobRepository.existsByStatusAndSourceName(CrawlStatus.RUNNING, sourceName)

	@Async
	fun run(sourceName: String) {
		val job = crawlJobRepository.save(
			CrawlJob(sourceName = sourceName)
		)
		crawlJobEventService.publishStarted(job)

		val errorLogBuilder = StringBuilder()

		try {
			val process = ProcessBuilder(crawlerBinaryPath, "--source", sourceName)
				.redirectErrorStream(true)
				.apply { environment()["API_URL"] = buildApiUrl() }
				.start()

			process.inputStream.bufferedReader().use { reader ->
				reader.forEachLine { line ->
					if (line.isBlank()) return@forEachLine

					val log = objectMapper.readTree(line)

					val msg = log["msg"]?.asString() ?: ""
					val event = CrawlerLogEvent.from(msg)
					val level = log["level"]?.asString() ?: "INFO"

					if (level == "ERROR") {
						val err = log["error"]?.asString() ?: "Unknown Error"
						errorLogBuilder.appendLine("$msg: $err")
					}

					if (event == CrawlerLogEvent.CRAWL_PROGRESS) {
						val booksFound = log["books_found"]?.asInt() ?: 0
						if (booksFound != job.booksFound) {
							job.booksFound = booksFound
							crawlJobRepository.save(job)
								.also(crawlJobEventService::publishUpdated)
						}
					}

					if (event == CrawlerLogEvent.CRAWL_COMPLETED) {
						job.booksFound = log["books_found"]?.asInt() ?: 0
					}
				}
			}

			val exitCode = process.waitFor()

			if (exitCode == 0) {
				job.status = CrawlStatus.SUCCESS
			} else {
				job.status = CrawlStatus.FAILED
				errorLogBuilder.appendLine("Process exited with code $exitCode")
			}
		} catch (e: Exception) {
			job.status = CrawlStatus.FAILED

			val exceptionMsg = e.localizedMessage ?: e.javaClass.simpleName
			errorLogBuilder.appendLine("System Failure: $exceptionMsg")
		} finally {
			job.finishedAt = LocalDateTime.now()

			if (errorLogBuilder.isNotEmpty()) {
				job.errorMessage = errorLogBuilder.toString().take(2000)
			}

			crawlJobRepository.save(job)
				.also(crawlJobEventService::publishUpdated)
		}
	}
}
