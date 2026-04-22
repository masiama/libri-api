package com.libri.api.service

import com.libri.api.entity.CrawlJob
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

@Service
class CrawlJobEventService {
	private val emitters = CopyOnWriteArrayList<SseEmitter>()

	fun subscribe(): SseEmitter {
		val emitter = SseEmitter(Long.MAX_VALUE)

		emitter.onCompletion { emitters.remove(emitter) }
		emitter.onTimeout {
			emitter.complete()
			emitters.remove(emitter)
		}
		emitter.onError { emitters.remove(emitter) }

		try {
			emitter.send(SseEmitter.event().name("connected").data("connected"))
			emitters.add(emitter)
		} catch (_: IOException) {
			// Handled silently; listener will be removed by callbacks
		}

		return emitter
	}

	fun publish(job: CrawlJob) {
		emitters.forEach { emitter ->
			try {
				emitter.send(
					SseEmitter.event().name("crawl-job-updated").data(job)
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
}
