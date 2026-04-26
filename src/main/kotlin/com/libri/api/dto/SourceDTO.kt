package com.libri.api.dto

import com.libri.api.entity.Source

data class SourceDTO(
	val name: String,
	val enabled: Boolean,
)

fun Source.toDto() = SourceDTO(name, enabled)
