package com.libri.api.config

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

enum class ImageSide(
    @get:JsonValue val value: String,
) {
    FRONT("front"),
    BACK("back"),
    SPINE("spine"),
    ;

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value }
    }
}

@Component
class ImageSideConverter : Converter<String, ImageSide> {
    override fun convert(source: String) = ImageSide.fromValue(source) ?: throw IllegalArgumentException("Unknown image side: $source")
}
