package com.libri.api.config

enum class ImageSide(
    val value: String,
) {
    FRONT("front"),
    BACK("back"),
    SPINE("spine"),
    ;

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value }
    }
}
