package com.libri.api.repository

import com.libri.api.entity.BarcodeId
import com.libri.api.entity.PurgatoryBarcode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface PurgatoryBarcodeRepository : JpaRepository<PurgatoryBarcode, BarcodeId> {
	fun findAllByPurgatoryId(id: Long): List<PurgatoryBarcode>
	fun deleteAllByPurgatoryId(id: Long)

	@Modifying
	@Transactional
	@NativeQuery(
		"""
			DELETE FROM purgatory_barcodes
			WHERE (purgatory_id, source_name) IN (SELECT * FROM unnest(:ids, :sources))
		"""
	)
	fun internalDeleteAllByPurgatoryIdAndSourceNamePairs(ids: Array<Long>, sources: Array<String>)

	@Transactional
	fun deleteAllByPurgatoryIdAndSourceNamePairs(pairs: List<Pair<Long, String>>) {
		if (pairs.isEmpty()) return
		internalDeleteAllByPurgatoryIdAndSourceNamePairs(
			ids = pairs.map { it.first }.toTypedArray(),
			sources = pairs.map { it.second }.toTypedArray()
		)
	}
}
