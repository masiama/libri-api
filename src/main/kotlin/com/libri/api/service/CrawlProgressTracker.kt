package com.libri.api.service

import com.libri.api.repository.CrawlJobRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

@Component
class CrawlProgressTracker(private val crawlJobRepository: CrawlJobRepository) {
	private val booksFoundState = ConcurrentHashMap<Long, Int>()
	private val booksFoundDirty = ConcurrentHashMap<Long, Boolean>()

	fun update(crawlId: Long, booksFound: Int) {
		booksFoundState[crawlId] = booksFound
		booksFoundDirty[crawlId] = true
	}

	fun get(crawlId: Long): Int? = booksFoundState[crawlId]

	fun clear(crawlId: Long) {
		booksFoundState.remove(crawlId)
		booksFoundDirty.remove(crawlId)
	}

	@Transactional
	@Scheduled(fixedDelay = 10_000)
	fun flushToDb() {
		booksFoundDirty.forEach { (crawlId, dirty) ->
			if (!dirty) return@forEach
			val count = booksFoundState[crawlId] ?: return@forEach
			booksFoundDirty[crawlId] = false
			crawlJobRepository.updateProgressHeartbeat(crawlId, count)
		}
	}
}
