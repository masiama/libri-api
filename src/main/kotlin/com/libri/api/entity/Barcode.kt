package com.libri.api.entity

import com.libri.api.dto.BarcodeDTO
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

data class BarcodeId(
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
