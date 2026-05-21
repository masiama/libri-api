package com.libri.api.config

import tools.jackson.module.kotlin.jacksonObjectMapper

object Jackson {
	val objectMapper = jacksonObjectMapper()
}

inline fun <reified T> String.fromJson(): T = Jackson.objectMapper.readValue(this, T::class.java)
fun Any.toJson(): String = Jackson.objectMapper.writeValueAsString(this)
