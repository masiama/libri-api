package com.libri.api.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {
    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<String>?): String = mapper.writeValueAsString(attribute ?: emptyList<String>())

    override fun convertToEntityAttribute(dbData: String?): List<String> =
        if (dbData == null) {
            emptyList()
        } else {
            mapper.readValue(dbData)
        }
}
