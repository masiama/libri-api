package com.libri.api.util

object IsbnValidator {
    fun isValid(isbn: String) =
        when (isbn.length) {
            10 -> isValidIsbn10(isbn)
            13 -> isValidIsbn13(isbn)
            else -> false
        }

    fun otherForm(isbn: String): String? {
        if (!isValid(isbn)) return null
        if (isbn.length == 10) {
            val first12 = "978" + isbn.dropLast(1)
            return first12 + isbn13Check(first12)
        }
        if (!isbn.startsWith("978")) return null
        val first9 = isbn.substring(3, 12)
        val check = isbn10Check(first9)
        return if (check == 10) null else first9 + check
    }

    private fun isValidIsbn10(isbn: String): Boolean =
        isbn.all { it.isDigit() } && isbn10Check(isbn.dropLast(1)) == isbn.last().digitToInt()

    private fun isValidIsbn13(isbn: String): Boolean =
        isbn.all { it.isDigit() } && isbn13Check(isbn.dropLast(1)) == isbn.last().digitToInt()

    private fun isbn10Check(first9: String): Int {
        val sum = first9.mapIndexed { i, c -> (10 - i) * c.digitToInt() }.sum()
        return (11 - (sum % 11)) % 11
    }

    private fun isbn13Check(first12: String): Int {
        val sum = first12.mapIndexed { i, c -> if (i % 2 == 0) c.digitToInt() else c.digitToInt() * 3 }.sum()
        return (10 - (sum % 10)) % 10
    }
}
