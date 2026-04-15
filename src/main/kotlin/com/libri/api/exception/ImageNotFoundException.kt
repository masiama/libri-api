package com.libri.api.exception

class ImageNotFoundException(isbn: String) :
	RuntimeException("Image not found for ISBN $isbn")
