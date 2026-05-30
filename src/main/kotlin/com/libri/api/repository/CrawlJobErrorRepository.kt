package com.libri.api.repository

import com.libri.api.entity.CrawlJobError
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CrawlJobErrorRepository : JpaRepository<CrawlJobError, Long> {
    fun findAllByCrawlJobId(
        crawlJobId: Long,
        pageable: Pageable,
    ): Page<CrawlJobError>

    fun countByCrawlJobId(crawlJobId: Long): Long

    interface CrawlJobErrorCount {
        val crawlJobId: Long
        val count: Long
    }

    @Query(
        """
			SELECT e.crawlJobId AS crawlJobId, COUNT(e) AS count FROM CrawlJobError e 
			WHERE e.crawlJobId IN :ids 
			GROUP BY e.crawlJobId
		""",
    )
    fun countsByCrawlJobIds(ids: List<Long>): List<CrawlJobErrorCount>
}
