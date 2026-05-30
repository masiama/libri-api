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
		jdbcTemplate.update { conn ->
			conn.prepareStatement(UPSERT_PURGATORY_SQL).apply {
				setArray(1, conn.createArrayOf("text", rows.map { it.invalidIsbn }.toTypedArray()))
				setArray(2, conn.createArrayOf("text", rows.map { it.title }.toTypedArray()))
				setArray(
					3,
					conn.createArrayOf("text", rows.map { objectMapper.writeValueAsString(it.authors) }.toTypedArray())
				)
				setArray(4, conn.createArrayOf("text", rows.map { it.url }.toTypedArray()))
				setArray(5, conn.createArrayOf("text", rows.map { it.sourceName }.toTypedArray()))
			}
		}
	}

	fun findStatesByInvalidIsbn(invalidIsbns: List<String>): Map<Pair<String, String>, PurgatoryBook> =
		purgatoryBookRepository.findAllByInvalidIsbnIn(invalidIsbns)
			.associateBy { (it.invalidIsbn to it.sourceName) }

	fun replaceBarcodes(rows: List<PurgatoryBarcodeReplacement>) {
		if (rows.isEmpty()) return

		val targets = rows.distinctBy { it.purgatoryId to it.sourceName }
		val barcodeRows = rows.flatMap { row ->
			row.barcodes.map { barcode ->
				PurgatoryBarcodeInsertRow(barcode.value, barcode.type, row.purgatoryId, row.sourceName)
			}
		}

		if (barcodeRows.isEmpty()) {
			jdbcTemplate.update { conn ->
				conn.prepareStatement(DELETE_PURGATORY_BARCODES_SQL).apply {
					setArray(1, conn.createArrayOf("int8", targets.map { it.purgatoryId }.toTypedArray()))
					setArray(2, conn.createArrayOf("text", targets.map { it.sourceName }.toTypedArray()))
				}
			}
			return
		}

		jdbcTemplate.update { conn ->
			conn.prepareStatement(DELETE_AND_INSERT_PURGATORY_BARCODES_SQL).apply {
				setArray(1, conn.createArrayOf("int8", targets.map { it.purgatoryId }.toTypedArray()))
				setArray(2, conn.createArrayOf("text", targets.map { it.sourceName }.toTypedArray()))
				setArray(3, conn.createArrayOf("text", barcodeRows.map { it.value }.toTypedArray()))
				setArray(4, conn.createArrayOf("text", barcodeRows.map { it.type }.toTypedArray()))
				setArray(5, conn.createArrayOf("int8", barcodeRows.map { it.purgatoryId }.toTypedArray()))
				setArray(6, conn.createArrayOf("text", barcodeRows.map { it.sourceName }.toTypedArray()))
			}
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
			SELECT t.invalid_isbn, t.title, t.authors::jsonb, t.url, t.source_name
			FROM unnest(?::text[], ?::text[], ?::text[], ?::text[], ?::text[])
				AS t(invalid_isbn, title, authors, url, source_name)
			ON CONFLICT (invalid_isbn, source_name) DO UPDATE SET
				title = EXCLUDED.title,
				authors = EXCLUDED.authors,
				url = EXCLUDED.url
			WHERE (SELECT priority FROM sources WHERE name = EXCLUDED.source_name)
				<= (SELECT priority FROM sources WHERE name = purgatory.source_name)
		"""

		private const val DELETE_PURGATORY_BARCODES_SQL = """
			DELETE FROM purgatory_barcodes pb
			USING unnest(?::bigint[], ?::text[]) AS d(purgatory_id, source_name)
			WHERE pb.purgatory_id = d.purgatory_id AND pb.source_name = d.source_name
		"""

		private const val DELETE_AND_INSERT_PURGATORY_BARCODES_SQL = """
			WITH deleted AS (
				DELETE FROM purgatory_barcodes pb
				USING unnest(?::bigint[], ?::text[]) AS d(purgatory_id, source_name)
				WHERE pb.purgatory_id = d.purgatory_id AND pb.source_name = d.source_name
				RETURNING pb.purgatory_id, pb.source_name
			)
			INSERT INTO purgatory_barcodes (value, type, purgatory_id, source_name)
			SELECT t.value, t.type, t.purgatory_id, t.source_name
			FROM unnest(?::text[], ?::text[], ?::bigint[], ?::text[])
				AS t(value, type, purgatory_id, source_name)
			LEFT JOIN deleted d ON t.purgatory_id = d.purgatory_id AND t.source_name = d.source_name
			ON CONFLICT (purgatory_id, source_name, value, type) DO NOTHING
		"""
	}
}
