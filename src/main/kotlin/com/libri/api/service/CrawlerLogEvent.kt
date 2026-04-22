package com.libri.api.service

enum class CrawlerLogEvent(val value: String) {
	CRAWL_PROGRESS("crawl_progress"),
	CRAWL_COMPLETED("crawl_completed");

	companion object {
		fun from(value: String): CrawlerLogEvent? =
			entries.firstOrNull { it.value == value }
	}
}
