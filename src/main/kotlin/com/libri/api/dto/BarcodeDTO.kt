package com.libri.api.dto

import com.libri.api.entity.Barcode

data class BarcodeDTO(
	val value: String,
	val type: String
) {
	fun toEntity(isbn: String, sourceName: String): Barcode =
		Barcode(value, type, isbn, sourceName)
}
