package com.libri.api.repository

import com.libri.api.entity.PurgatoryBook
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PurgatoryBookRepository : JpaRepository<PurgatoryBook, Long> {
	fun findAllByResolvedIsbnIsNullAndDeletedFalse(pageable: Pageable): Page<PurgatoryBook>
	fun findAllByInvalidIsbnIn(map: List<String>): List<PurgatoryBook>
}
