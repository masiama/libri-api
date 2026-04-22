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
		emitter.onTimeout { emitters.remove(emitter) }
		emitter.onError { emitters.remove(emitter) }

		try {
			emitter.send(SseEmitter.event().name("connected").data("connected"))
			emitters.add(emitter)
		} catch (_: IOException) {
			emitter.complete()
			emitters.remove(emitter)
		}

		return emitter
	}

	fun publish(job: CrawlJob) {
		emitters.forEach { emitter ->
			try {
				emitter.send(
					SseEmitter.event().name("crawl-job-updated").data(job)
				)
			} catch (e: Exception) {
				emitter.completeWithError(e)
			}
		}
	}
}
