package com.libri.api.entity

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
)
