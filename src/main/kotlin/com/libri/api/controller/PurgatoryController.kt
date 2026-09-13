package com.libri.api.controller

import com.libri.api.dto.PurgatoryBookDTO
import com.libri.api.service.PurgatoryService
import com.libri.api.util.IsbnValidator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class PurgatoryApproveRequest(
    val isbn: String,
)

data class PurgatoryBulkDeleteRequest(
    val ids: List<Long>,
)

@RestController
@RequestMapping("/api/v1/admin/purgatory", produces = [MediaType.APPLICATION_JSON_VALUE])
class PurgatoryController(
    private val purgatoryService: PurgatoryService,
) {
    @Operation(operationId = "listPurgatoryBooks")
    @GetMapping
    fun list(
        @RequestParam(required = false) filter: String?,
        @ParameterObject pageable: Pageable,
    ): Page<PurgatoryBookDTO> = purgatoryService.list(pageable, filter)

    @Operation(operationId = "approvePurgatoryBook")
    @ApiResponses(
        ApiResponse(responseCode = "200"),
        ApiResponse(responseCode = "400", content = [Content()]),
        ApiResponse(responseCode = "404", content = [Content()]),
    )
    @PostMapping("/{id}/approve")
    fun approve(
        @PathVariable id: Long,
        @RequestBody request: PurgatoryApproveRequest,
    ): ResponseEntity<PurgatoryBookDTO> {
        if (!IsbnValidator.isValid(request.isbn)) return ResponseEntity.badRequest().build()
        return purgatoryService.approve(id, request.isbn)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }

    @Operation(operationId = "deletePurgatoryBook")
    @ApiResponses(
        ApiResponse(responseCode = "204", content = [Content()]),
        ApiResponse(responseCode = "404", content = [Content()]),
    )
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        if (!purgatoryService.markDeleted(id)) return ResponseEntity.notFound().build()
        return ResponseEntity.noContent().build()
    }

    @Operation(operationId = "deletePurgatoryBooksBulk")
    @ApiResponses(
        ApiResponse(responseCode = "204", content = [Content()]),
        ApiResponse(responseCode = "404", content = [Content()]),
    )
    @DeleteMapping("/bulk")
    fun deleteBulk(
        @RequestBody request: PurgatoryBulkDeleteRequest,
    ): ResponseEntity<Void> =
        if (purgatoryService.markDeletedBulk(request.ids)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
}
