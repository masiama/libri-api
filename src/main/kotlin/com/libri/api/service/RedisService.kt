package com.libri.api.service

import com.libri.api.config.toJson
import com.libri.api.entity.CrawlJob
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

data class CrawlerCommand(val crawlId: Long, val source: String)

@Service
class RedisService(private val redisTemplate: StringRedisTemplate) {
	private val cacheKeyExistingUrls = "books:existing_urls"

	fun readCrawlEvent(): String? =
		redisTemplate.opsForList().rightPop("crawl:events", Duration.ofSeconds(5))

	fun resetExistingUrls(urls: List<String>) {
		redisTemplate.delete(cacheKeyExistingUrls)
		redisTemplate.opsForSet().add(cacheKeyExistingUrls, *urls.toTypedArray())
	}

	fun deleteSourceLock(sourceName: String): Boolean? =
		redisTemplate.delete("lock:crawler:${sourceName}")

	fun pushCrawlJob(job: CrawlJob) {
		val command = CrawlerCommand(crawlId = job.id, source = job.sourceName)
		redisTemplate.opsForList().leftPush("crawler:commands", command.toJson())
	}
}
