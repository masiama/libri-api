package com.libri.api.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.PROPERTY,
	property = "type"
)
@JsonSubTypes(
	JsonSubTypes.Type(value = CrawlerEvent.BookEvent::class, name = "book"),
	JsonSubTypes.Type(value = CrawlerEvent.ProgressEvent::class, name = "progress"),
	JsonSubTypes.Type(value = CrawlerEvent.CompletedEvent::class, name = "completed"),
	JsonSubTypes.Type(value = CrawlerEvent.ErrorEvent::class, name = "error"),
	JsonSubTypes.Type(value = CrawlerEvent.CrawlErrorEvent::class, name = "crawl_error"),
	JsonSubTypes.Type(value = CrawlerEvent.CancelledEvent::class, name = "cancelled")
)
sealed class CrawlerEvent {
	abstract val crawlId: Long

	data class BookEvent(
		override val crawlId: Long,
		val book: BookDTO
	) : CrawlerEvent()

	data class ProgressEvent(
		override val crawlId: Long,
		val booksFound: Int
	) : CrawlerEvent()

	data class CompletedEvent(
		override val crawlId: Long,
		val booksFound: Int
	) : CrawlerEvent()

	data class ErrorEvent(
		override val crawlId: Long,
		val error: String
	) : CrawlerEvent()

	data class CrawlErrorEvent(
		override val crawlId: Long,
		val error: String
	) : CrawlerEvent()

	data class CancelledEvent(
		override val crawlId: Long,
		val booksFound: Int
	) : CrawlerEvent()
}
