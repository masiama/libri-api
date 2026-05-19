package com.libri.api.repository

import com.libri.api.entity.CrawlJob
import com.libri.api.entity.CrawlStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface CrawlJobRepository : JpaRepository<CrawlJob, Long> {
	fun existsByStatusAndSourceName(status: CrawlStatus, sourceName: String): Boolean

	@Modifying
	@Transactional
	@Query("update CrawlJob c set c.booksFound = :booksFound where c.id = :id")
	fun updateBooksFound(id: Long, booksFound: Int): Int
}
