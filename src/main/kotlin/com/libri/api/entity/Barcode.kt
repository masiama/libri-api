package com.libri.api.entity

import com.libri.api.dto.BarcodeDTO
import jakarta.persistence.*
import java.io.Serializable

class BarcodeId(
	val value: String = "",
	val type: String = "",
	val isbn: String = "",
	val sourceName: String = "",
) : Serializable

@Entity
@IdClass(BarcodeId::class)
@Table(name = "barcodes")
class Barcode(
	@Id
	@Column(nullable = false)
	val value: String,

	@Id
	@Column(nullable = false)
	val type: String,

	@Id
	@Column(nullable = false)
	val isbn: String,

	@Id
	@Column(name = "source_name", nullable = false)
	val sourceName: String,
) {
	fun toDTO(): BarcodeDTO = BarcodeDTO(value, type)
}
