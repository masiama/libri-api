package com.libri.api.repository

import com.libri.api.entity.Book
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BookRepository : JpaRepository<Book, String> {
	fun existsByUrl(url: String): Boolean

	@NativeQuery(
		"""
			SELECT * FROM books
			WHERE
			 title ILIKE (:title || '%')
			 OR title ILIKE ('% ' || :title || '%')
			 OR title % :title
			 OR EXISTS (
			 	SELECT 1 FROM jsonb_array_elements_text(authors) AS author
			 	WHERE author ILIKE ('%' || :title || '%')
			 )
			ORDER BY
			 (title ILIKE (:title || '%')) DESC,
			 similarity(title, :title) DESC
		""",
		countQuery = """
			SELECT count(1) FROM books
			WHERE
			 title ILIKE (:title || '%')
			 OR title ILIKE ('% ' || :title || '%')
			 OR title % :title
			 OR EXISTS (
			 	SELECT 1 FROM jsonb_array_elements_text(authors) AS author
			 	WHERE author ILIKE ('%' || :title || '%')
			 )
		"""
	)
	fun searchByTitle(@Param("title") title: String, pageable: Pageable): Page<Book>
}
