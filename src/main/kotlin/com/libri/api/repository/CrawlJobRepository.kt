package com.libri.api.repository

import com.libri.api.entity.CrawlJob
import com.libri.api.entity.CrawlStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CrawlJobRepository : JpaRepository<CrawlJob, Long> {
	fun existsByStatusAndSourceName(status: CrawlStatus, sourceName: String): Boolean
}
