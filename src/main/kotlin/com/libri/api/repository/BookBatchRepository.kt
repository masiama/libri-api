package com.libri.api.repository

import com.libri.api.entity.Book
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

data class BarcodeValueRow(
	val value: String,
	val type: String,
)

data class BookBarcodeReplacement(
	val isbn: String,
	val sourceName: String,
	val barcodes: List<BarcodeValueRow>,
)

@Repository
class BookBatchRepository(
	private val jdbcTemplate: JdbcTemplate,
	private val objectMapper: ObjectMapper,
) {
	fun upsertBooks(rows: List<Book>) {
		if (rows.isEmpty()) return

		jdbcTemplate.batchUpdate(UPSERT_BOOKS_SQL, rows, rows.size) { ps, row ->
			ps.setString(1, row.isbn)
			ps.setString(2, row.title)
			ps.setString(3, objectMapper.writeValueAsString(row.authors))
			ps.setString(4, row.url)
			ps.setString(5, row.sourceName)
		}
	}

	fun replaceBarcodes(rows: List<BookBarcodeReplacement>) {
		if (rows.isEmpty()) return

		val targets = rows.distinctBy { it.isbn to it.sourceName }
		jdbcTemplate.batchUpdate(DELETE_BARCODES_SQL, targets, targets.size) { ps, row ->
			ps.setString(1, row.isbn)
			ps.setString(2, row.sourceName)
		}

		val barcodeRows = rows.flatMap { row ->
			row.barcodes.map { barcode ->
				BookBarcodeInsertRow(barcode.value, barcode.type, row.isbn, row.sourceName)
			}
		}
		if (barcodeRows.isEmpty()) return

		jdbcTemplate.batchUpdate(INSERT_BARCODES_SQL, barcodeRows, barcodeRows.size) { ps, row ->
			ps.setString(1, row.value)
			ps.setString(2, row.type)
			ps.setString(3, row.isbn)
			ps.setString(4, row.sourceName)
		}
	}

	private data class BookBarcodeInsertRow(
		val value: String,
		val type: String,
		val isbn: String,
		val sourceName: String,
	)

	private companion object {
		private const val UPSERT_BOOKS_SQL = """
			INSERT INTO books (isbn, title, authors, url, source_name)
			VALUES (?, ?, ?::jsonb, ?, ?)
			ON CONFLICT (isbn) DO UPDATE SET
				title = EXCLUDED.title,
				authors = EXCLUDED.authors,
				url = EXCLUDED.url,
				source_name = EXCLUDED.source_name
			WHERE (SELECT priority FROM sources WHERE name = EXCLUDED.source_name)
				<= (SELECT priority FROM sources WHERE name = books.source_name)
		"""

		private const val DELETE_BARCODES_SQL = """
			DELETE FROM barcodes
			WHERE isbn = ? AND source_name = ?
		"""

		private const val INSERT_BARCODES_SQL = """
			INSERT INTO barcodes (value, type, isbn, source_name)
			VALUES (?, ?, ?, ?)
			ON CONFLICT DO NOTHING
		"""
	}
}
