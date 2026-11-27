package com.libri.api.repository

import com.libri.api.entity.Book
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BookRepository : JpaRepository<Book, String> {
    @Query("select b.url from Book b")
    fun findAllUrls(): List<String>

    fun findBySourceName(
        sourceName: String,
        pageable: Pageable,
    ): Page<Book>

    @NativeQuery(
        """
			SELECT * FROM (
				SELECT *, source_name AS "sourcename" FROM books
				WHERE
				 (title ILIKE (:title || '%')
				 OR title ILIKE ('% ' || :title || '%')
				 OR title % :title
				 OR authors_text ILIKE ('%' || :title || '%'))
				 AND (CAST(:source AS text) IS NULL OR source_name = :source)
				ORDER BY
				 (title ILIKE (:title || '%')) DESC,
				 similarity(title, :title) DESC
			) as results
		""",
        countQuery = """
			SELECT count(1) FROM books
			WHERE
			 (title ILIKE (:title || '%')
			 OR title ILIKE ('% ' || :title || '%')
			 OR title % :title
			 OR authors_text ILIKE ('%' || :title || '%'))
			 AND (CAST(:source AS text) IS NULL OR source_name = :source)
		""",
    )
    fun searchByTitle(
        @Param("title") title: String,
        @Param("source") source: String?,
        pageable: Pageable,
    ): Page<Book>
}
