package com.libri.api.repository

import com.libri.api.entity.PurgatoryBook
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PurgatoryBookRepository : JpaRepository<PurgatoryBook, Long> {
	fun findAllByResolvedIsbnIsNullAndDeletedFalse(pageable: Pageable): Page<PurgatoryBook>
	fun findAllByInvalidIsbnIn(map: List<String>): List<PurgatoryBook>

	@NativeQuery(
		"""
			SELECT * FROM (
				SELECT *, source_name AS "sourcename" FROM purgatory
				WHERE
				 resolved_isbn IS NULL AND
				 deleted = false AND
				 (
				  title ILIKE (:title || '%')
				  OR title ILIKE ('% ' || :title || '%')
				  OR title % :title
				  OR EXISTS (
					 SELECT 1 FROM jsonb_array_elements_text(authors) AS author
					 WHERE author ILIKE ('%' || :title || '%')
				  )
				 )
				ORDER BY
				 (title ILIKE (:title || '%')) DESC,
				 similarity(title, :title) DESC
			) as results
		""",
		countQuery = """
			SELECT count(1) FROM purgatory
			WHERE
			 resolved_isbn IS NULL AND
			 deleted = false AND
			 (
			  title ILIKE (:title || '%')
			  OR title ILIKE ('% ' || :title || '%')
			  OR title % :title
			  OR EXISTS (
				 SELECT 1 FROM jsonb_array_elements_text(authors) AS author
				 WHERE author ILIKE ('%' || :title || '%')
			  )
			 )
		"""
	)
	fun findAllByTitleAndResolvedIsbnIsNullAndDeletedFalse(
		@Param("title") title: String,
		pageable: Pageable
	): Page<PurgatoryBook>
}
