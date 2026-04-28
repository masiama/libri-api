package com.libri.api.repository

import com.libri.api.entity.BarcodeId
import com.libri.api.entity.PurgatoryBarcode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PurgatoryBarcodeRepository : JpaRepository<PurgatoryBarcode, BarcodeId> {
	fun findAllByPurgatoryId(id: Long): List<PurgatoryBarcode>
	fun deleteAllByPurgatoryId(id: Long)
}
