package com.libri.api.entity

import com.libri.api.dto.SourceDTO
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "sources")
class Source(
	@Id
	val name: String,

	@Column(nullable = false)
	val priority: Short,

	@Column(nullable = false)
	val enabled: Boolean = true,
) {
	fun toDTO() = SourceDTO(name, enabled)
}
