package com.libri.api.repository

import com.libri.api.entity.Book
import jakarta.annotation.PostConstruct
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
	private val sourceRepository: SourceRepository,
	private val jdbcTemplate: JdbcTemplate,
	private val objectMapper: ObjectMapper,
) {
	private lateinit var sourcePriorities: Map<String, Short>

	@PostConstruct
	fun loadPriorities() {
		sourcePriorities = sourceRepository.findAll().associate { it.name to it.priority }
	}

	fun upsertBooks(rows: List<Book>) {
		if (rows.isEmpty()) return

		val deduped = rows
			.groupBy { it.isbn }
			.values
			.map { duplicates ->
				duplicates.minByOrNull { sourcePriorities[it.sourceName] ?: Short.MIN_VALUE }!!
			}

		jdbcTemplate.update { conn ->
			conn.prepareStatement(UPSERT_BOOKS_SQL).apply {
				setArray(1, conn.createArrayOf("text", deduped.map { it.isbn }.toTypedArray()))
				setArray(2, conn.createArrayOf("text", deduped.map { it.title }.toTypedArray()))
				setArray(
					3,
					conn.createArrayOf(
						"text",
						deduped.map { objectMapper.writeValueAsString(it.authors) }.toTypedArray()
					)
				)
				setArray(4, conn.createArrayOf("text", deduped.map { it.url }.toTypedArray()))
				setArray(5, conn.createArrayOf("text", deduped.map { it.sourceName }.toTypedArray()))
			}
		}
	}

	fun replaceBarcodes(rows: List<BookBarcodeReplacement>) {
		if (rows.isEmpty()) return

		val targets = rows.distinctBy { it.isbn to it.sourceName }
		val barcodeRows = rows.flatMap { row ->
			row.barcodes.map { barcode ->
				BookBarcodeInsertRow(barcode.value, barcode.type, row.isbn, row.sourceName)
			}
		}

		if (barcodeRows.isEmpty()) {
			jdbcTemplate.update { conn ->
				conn.prepareStatement(DELETE_BARCODES_SQL).apply {
					setArray(1, conn.createArrayOf("text", targets.map { it.isbn }.toTypedArray()))
					setArray(2, conn.createArrayOf("text", targets.map { it.sourceName }.toTypedArray()))
				}
			}
			return
		}

		jdbcTemplate.update { conn ->
			conn.prepareStatement(DELETE_AND_INSERT_BARCODES_SQL).apply {
				setArray(1, conn.createArrayOf("text", targets.map { it.isbn }.toTypedArray()))
				setArray(2, conn.createArrayOf("text", targets.map { it.sourceName }.toTypedArray()))
				setArray(3, conn.createArrayOf("text", barcodeRows.map { it.value }.toTypedArray()))
				setArray(4, conn.createArrayOf("text", barcodeRows.map { it.type }.toTypedArray()))
				setArray(5, conn.createArrayOf("text", barcodeRows.map { it.isbn }.toTypedArray()))
				setArray(6, conn.createArrayOf("text", barcodeRows.map { it.sourceName }.toTypedArray()))
			}
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
            SELECT 
                unnest(?::text[]),
                unnest(?::text[]),
                unnest(?::text[])::jsonb,
                unnest(?::text[]),
                unnest(?::text[])
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
            WHERE (isbn, source_name) = ANY(
                SELECT unnest(?::text[]), unnest(?::text[])
            )
		"""

		private const val DELETE_AND_INSERT_BARCODES_SQL = """
			WITH deleted AS (
				DELETE FROM barcodes
				WHERE (isbn, source_name) = ANY(
					SELECT unnest(?::text[]), unnest(?::text[])
				)
			)
			INSERT INTO barcodes (value, type, isbn, source_name)
			SELECT * FROM unnest(?::text[], ?::text[], ?::text[], ?::text[])
			ON CONFLICT DO NOTHING
		"""
	}
}
