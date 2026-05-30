package com.libri.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

data class PurgatoryBarcodeId(
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
