package com.libri.api.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(ImageNotFoundException::class)
	fun handleImageNotFound(): ResponseEntity<Void> {
		return ResponseEntity.notFound().build()
	}
}
