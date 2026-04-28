package com.libri.api.repository

import com.libri.api.entity.Barcode
import com.libri.api.entity.BarcodeId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BarcodeRepository : JpaRepository<Barcode, BarcodeId> {
	fun findAllByIsbn(isbn: String): List<Barcode>
	fun findAllByIsbnIn(isbns: List<String>): List<Barcode>
	fun deleteAllByIsbn(isbn: String)
	fun findFirstByValue(value: String): Barcode?
}
