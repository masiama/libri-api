package com.libri.api.controller

import com.libri.api.dto.SourceDTO
import com.libri.api.service.SourceService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sources", produces = [MediaType.APPLICATION_JSON_VALUE])
class SourceController(
    private val sourceService: SourceService,
) {
    @Operation(operationId = "listSources")
    @GetMapping
    fun list(): List<SourceDTO> = sourceService.list()
}
