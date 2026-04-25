package com.libri.api.util

object IsbnValidator {
	fun isValid(isbn: String) = when (isbn.length) {
		10 -> isValidIsbn10(isbn)
		13 -> isValidIsbn13(isbn)
		else -> false
	}

	private fun isValidIsbn10(isbn: String): Boolean {
		if (!isbn.all { it.isDigit() }) return false
		val sum = isbn.dropLast(1)
			.mapIndexed { i, c -> (10 - i) * c.digitToInt() }
			.sum()
		val check = (11 - (sum % 11)) % 11
		return check == isbn.last().digitToInt()
	}

	private fun isValidIsbn13(isbn: String): Boolean {
		if (!isbn.all { it.isDigit() }) return false
		val sum = isbn.dropLast(1)
			.mapIndexed { i, c -> if (i % 2 == 0) c.digitToInt() else c.digitToInt() * 3 }
			.sum()
		val check = (10 - (sum % 10)) % 10
		return check == isbn.last().digitToInt()
	}
}
