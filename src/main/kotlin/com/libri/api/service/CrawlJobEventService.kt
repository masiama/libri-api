package com.libri.api.service

import com.libri.api.entity.CrawlJob
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

@Service
class CrawlJobEventService {
	private val emitters = CopyOnWriteArrayList<SseEmitter>()

	fun subscribe(): SseEmitter {
		val emitter = SseEmitter(120_000L)

		emitter.onCompletion { emitters.remove(emitter) }
		emitter.onTimeout { emitter.complete() }
		emitter.onError { emitters.remove(emitter) }

		emitters.add(emitter)

		try {
			emitter.send(SseEmitter.event().comment("prime"))
			emitter.send(SseEmitter.event().name("connected").data("connected"))
		} catch (e: IOException) {
			emitters.remove(emitter)
			emitter.completeWithError(e)
		}

		return emitter
	}

	fun publishStarted(job: CrawlJob) = publish("crawl-job-started", job)

	fun publishUpdated(job: CrawlJob) = publish("crawl-job-updated", job)

	private fun publish(eventName: String, job: CrawlJob) {
		emitters.forEach { emitter ->
			try {
				emitter.send(
					SseEmitter.event().name(eventName).data(job)
				)
			} catch (_: IOException) {
				// Do NOT call emitter.complete() here.
				// The browser refresh already triggered onCompletion/onTimeout.
				// We just catch this to prevent the background thread from crashing.
				emitters.remove(emitter)
			} catch (e: Exception) {
				emitter.completeWithError(e)
				emitters.remove(emitter)
			}
		}
	}

	@Scheduled(fixedDelay = 15_000)
	fun sendHeartbeat() {
		emitters.removeIf { emitter ->
			runCatching {
				emitter.send(SseEmitter.event().comment("heartbeat"))
				false
			}.isFailure
		}
	}
}
