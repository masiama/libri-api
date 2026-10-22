package com.libri.api.service

import com.libri.api.entity.CrawlStatus
import com.libri.api.repository.CrawlJobRepository
import com.libri.api.repository.SourceRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component

@Component
class LibriMetrics(
    private val sourceRepository: SourceRepository,
    private val crawlJobRepository: CrawlJobRepository,
    private val redisService: RedisService,
) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        // Sources only change via migrations, so registering them once at startup is enough
        sourceRepository.findAll().forEach { source ->
            Gauge
                .builder("libri.crawl.running") {
                    if (crawlJobRepository.existsByStatusAndSourceName(CrawlStatus.RUNNING, source.name)) 1.0 else 0.0
                }.tag("source", source.name)
                .description("1 while a crawl of this source runs; drawn as crawl bands in Grafana")
                .register(registry)
        }

        Gauge
            .builder("libri.crawler.events.queue.length") { redisService.eventsQueueLength()?.toDouble() ?: Double.NaN }
            .description("Crawler events waiting for the API; growing during a crawl means the API can't keep up")
            .register(registry)
    }
}
