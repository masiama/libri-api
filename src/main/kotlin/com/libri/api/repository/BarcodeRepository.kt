package com.libri.api.repository

import com.libri.api.entity.Barcode
import com.libri.api.entity.BarcodeId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface BarcodeRepository : JpaRepository<Barcode, BarcodeId> {
	fun findAllByIsbn(isbn: String): List<Barcode>
	fun findAllByIsbnIn(isbns: List<String>): List<Barcode>
	fun deleteAllByIsbn(isbn: String)
	fun findFirstByValue(value: String): Barcode?

	@Modifying
	@Transactional
	@NativeQuery(
		"""
			DELETE FROM barcodes
			WHERE (isbn, source_name) IN (SELECT * FROM unnest(:isbns, :sources))
		"""
	)
	fun internalDeleteAllByIsbnAndSourceNamePairs(isbns: Array<String>, sources: Array<String>)

	@Transactional
	fun deleteAllByIsbnAndSourceNamePairs(pairs: List<Pair<String, String>>) {
		if (pairs.isEmpty()) return
		internalDeleteAllByIsbnAndSourceNamePairs(
			isbns = pairs.map { it.first }.toTypedArray(),
			sources = pairs.map { it.second }.toTypedArray()
		)
	}
}
