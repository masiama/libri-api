package com.libri.api.entity

import jakarta.persistence.*
import java.io.Serializable

class PurgatoryBarcodeId(
	val value: String = "",
	val type: String = "",
	val purgatoryId: Long = 0,
	val sourceName: String = "",
) : Serializable

@Entity
@IdClass(PurgatoryBarcodeId::class)
@Table(name = "purgatory_barcodes")
class PurgatoryBarcode(
	@Id
	@Column(nullable = false)
	val value: String,

	@Id
	@Column(nullable = false)
	val type: String,

	@Id
	@Column(name = "purgatory_id", nullable = false)
	val purgatoryId: Long,

	@Id
	@Column(name = "source_name", nullable = false)
	val sourceName: String,
)
