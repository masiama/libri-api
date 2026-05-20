package com.libri.api.repository

import com.libri.api.entity.CrawlJob
import com.libri.api.entity.CrawlStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface CrawlJobRepository : JpaRepository<CrawlJob, Long> {
	fun existsByStatusAndSourceName(status: CrawlStatus, sourceName: String): Boolean

	@Modifying
	@Transactional
	@Query(
		"""
			UPDATE CrawlJob j SET j.booksFound = :count, j.lastHeartbeatAt = :now 
			WHERE j.id = :id
		"""
	)
	fun updateProgressHeartbeat(id: Long, count: Int, now: Instant = Instant.now())

	@Query(
		"""
			SELECT j FROM CrawlJob j
			WHERE j.status = :status
			AND (
				(j.lastHeartbeatAt IS NOT NULL AND j.lastHeartbeatAt < :threshold)
				OR
				(j.lastHeartbeatAt IS NULL AND j.startedAt < :startedThreshold)
			)
		"""
	)
	fun findStaleJobs(
		threshold: Instant,
		startedThreshold: Instant,
		status: CrawlStatus = CrawlStatus.RUNNING
	): List<CrawlJob>
}
