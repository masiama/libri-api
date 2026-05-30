package com.libri.api.service

import com.libri.api.config.toJson
import com.libri.api.entity.CrawlJob
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

data class CrawlerCommand(
    val crawlId: Long,
    val source: String,
)

@Service
class RedisService(
    private val redisTemplate: StringRedisTemplate,
) {
    private val existingUrlsSet = "books:existing_urls"
    private val eventsQueue = "crawler:events"
    private val commandsQueue = "crawler:commands"

    private fun lockKey(sourceName: String) = "crawler:lock:$sourceName"

    private fun cancelKey(sourceName: String) = "crawler:cancel:$sourceName"

    fun readCrawlEvent(): String? = redisTemplate.opsForList().rightPop(eventsQueue, Duration.ofSeconds(5))

    fun startCancel(sourceName: String) = redisTemplate.opsForValue().set(cancelKey(sourceName), "1")

    fun stopCancel(sourceName: String): Boolean? = redisTemplate.delete(cancelKey(sourceName))

    fun resetExistingUrls(urls: List<String>) {
        redisTemplate.delete(existingUrlsSet)
        addExistingUrls(urls)
    }

    fun addExistingUrls(urls: List<String>) {
        if (urls.isEmpty()) return
        urls.chunked(1000).forEach { chunk ->
            redisTemplate.opsForSet().add(existingUrlsSet, *chunk.toTypedArray())
        }
    }

    fun deleteSourceLock(sourceName: String): Boolean? = redisTemplate.delete(lockKey(sourceName))

    fun pushCrawlJob(job: CrawlJob) {
        val command = CrawlerCommand(crawlId = job.id, source = job.sourceName)
        redisTemplate.opsForList().leftPush(commandsQueue, command.toJson())
    }
}
