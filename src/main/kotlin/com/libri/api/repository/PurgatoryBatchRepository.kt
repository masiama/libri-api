package com.libri.api.repository

import com.libri.api.entity.PurgatoryBook
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

data class PurgatoryBarcodeReplacement(
	val purgatoryId: Long,
	val sourceName: String,
	val barcodes: List<BarcodeValueRow>,
)

@Repository
class PurgatoryBatchRepository(
	private val jdbcTemplate: JdbcTemplate,
	private val objectMapper: ObjectMapper,
	private val purgatoryBookRepository: PurgatoryBookRepository,
) {
	fun upsertBooks(rows: List<PurgatoryBook>) {
		if (rows.isEmpty()) return

		jdbcTemplate.batchUpdate(UPSERT_PURGATORY_SQL, rows, rows.size) { ps, row ->
			ps.setString(1, row.invalidIsbn)
			ps.setString(2, row.title)
			ps.setString(3, objectMapper.writeValueAsString(row.authors))
			ps.setString(4, row.url)
			ps.setString(5, row.sourceName)
		}
	}

	fun findStatesByInvalidIsbn(invalidIsbns: List<String>): Map<Pair<String, String>, PurgatoryBook> =
		purgatoryBookRepository.findAllByInvalidIsbnIn(invalidIsbns)
			.associateBy { (it.invalidIsbn to it.sourceName) }

	fun replaceBarcodes(rows: List<PurgatoryBarcodeReplacement>) {
		if (rows.isEmpty()) return

		val targets = rows.distinctBy { it.purgatoryId to it.sourceName }
		jdbcTemplate.batchUpdate(DELETE_PURGATORY_BARCODES_SQL, targets, targets.size) { ps, row ->
			ps.setLong(1, row.purgatoryId)
			ps.setString(2, row.sourceName)
		}

		val barcodeRows = rows.flatMap { row ->
			row.barcodes.map { barcode ->
				PurgatoryBarcodeInsertRow(barcode.value, barcode.type, row.purgatoryId, row.sourceName)
			}
		}
		if (barcodeRows.isEmpty()) return

		jdbcTemplate.batchUpdate(INSERT_PURGATORY_BARCODES_SQL, barcodeRows, barcodeRows.size) { ps, row ->
			ps.setString(1, row.value)
			ps.setString(2, row.type)
			ps.setLong(3, row.purgatoryId)
			ps.setString(4, row.sourceName)
		}
	}

	private data class PurgatoryBarcodeInsertRow(
		val value: String,
		val type: String,
		val purgatoryId: Long,
		val sourceName: String,
	)

	private companion object {
		private const val UPSERT_PURGATORY_SQL = """
			INSERT INTO purgatory (invalid_isbn, title, authors, url, source_name)
			VALUES (?, ?, ?::jsonb, ?, ?)
			ON CONFLICT (invalid_isbn, source_name) DO UPDATE SET
				title = EXCLUDED.title,
				authors = EXCLUDED.authors,
				url = EXCLUDED.url
			WHERE (SELECT priority FROM sources WHERE name = EXCLUDED.source_name)
				<= (SELECT priority FROM sources WHERE name = purgatory.source_name)
		"""

		private const val DELETE_PURGATORY_BARCODES_SQL = """
			DELETE FROM purgatory_barcodes
			WHERE purgatory_id = ? AND source_name = ?
		"""

		private const val INSERT_PURGATORY_BARCODES_SQL = """
			INSERT INTO purgatory_barcodes (value, type, purgatory_id, source_name)
			VALUES (?, ?, ?, ?)
			ON CONFLICT DO NOTHING
		"""
	}
}
