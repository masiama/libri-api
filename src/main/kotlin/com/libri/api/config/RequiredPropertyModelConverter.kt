package com.libri.api.config

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.providers.ObjectMapperProvider
import org.springframework.stereotype.Component
import kotlin.reflect.full.primaryConstructor

@Component
class RequiredPropertyModelConverter(
    private val objectMapperProvider: ObjectMapperProvider,
) : ModelConverter {
    override fun resolve(
        type: AnnotatedType,
        context: ModelConverterContext,
        chain: MutableIterator<ModelConverter>,
    ): Schema<*>? {
        if (!chain.hasNext()) return null
        val resolvedSchema = chain.next().resolve(type, context, chain)

        val javaType = objectMapperProvider.jsonMapper().constructType(type.type)
        val rawClass = javaType.rawClass

        val propertyNames =
            if (rawClass.isRecord) {
                rawClass.recordComponents.map { it.name }
            } else {
                val kotlinClass =
                    try {
                        rawClass.kotlin
                    } catch (_: Throwable) {
                        return resolvedSchema
                    }
                kotlinClass.primaryConstructor?.parameters?.mapNotNull { it.name } ?: emptyList()
            }
        if (propertyNames.isEmpty()) return resolvedSchema

        val targetSchema =
            resolvedSchema
                ?.`$ref`
                ?.let { context.definedModels[it.substringAfterLast('/')] }
                ?: resolvedSchema
        if (targetSchema?.properties == null) return resolvedSchema

        propertyNames.forEach { name ->
            if (targetSchema.properties.containsKey(name)) {
                targetSchema.addRequiredItem(name)
            }
        }

        return resolvedSchema
    }
}
