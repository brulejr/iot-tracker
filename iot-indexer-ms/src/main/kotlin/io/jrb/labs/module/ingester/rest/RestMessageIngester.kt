/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.module.ingester.rest

import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.common.scheduler.TaskSchedulerService
import io.jrb.labs.module.event.Message
import io.jrb.labs.module.ingester.MessageIngester
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

class RestMessageIngester(
    private val restServerConfig: RestServerConfig,
    private val taskSchedulerService: TaskSchedulerService,
    private val webClient: WebClient
) : MessageIngester {

    private val allNessages = Predicate { _: Message -> true }

    private val log by LoggerDelegate()
    private val running: AtomicBoolean = AtomicBoolean()
    private val messageSink: Sinks.Many<Message> = Sinks.many().multicast().onBackpressureBuffer()

    override fun isRunning(): Boolean {
        return running.get()
    }

    override fun stream(): Flux<Message> {
        return messageSink.asFlux()
    }

    override fun start() {
        try {
            val brokerName = restServerConfig.brokerName
            val period = Duration.ofMinutes(restServerConfig.pollRateInMins)
            log.info("Starting message handler - brokerName={}", brokerName)
            taskSchedulerService.scheduleTaskAtFixedRate(
                brokerName,
                RestCallHandler(restServerConfig, webClient, messageSink),
                period
            )
            running.set(true)
        } catch (e: Exception) {
            log.error("Unable to start message handler '{}' - {}", restServerConfig.brokerName, e.message, e)
        }
    }

    override fun stop() {
        try {
            val brokerName = restServerConfig.brokerName
            log.info("Stopping message handler '{}'...", brokerName)
            taskSchedulerService.cancelTask(brokerName)
            running.set(false)
        } catch (e: Exception) {
            log.error("Unable to stop message handler '{}' - {}", restServerConfig.brokerName, e.message, e)
        }
    }

    override fun subscribe(handler: (Message) -> Unit): Disposable? {
        return subscribe(allNessages, handler)
    }

    override fun subscribe(filter: Predicate<Message>, handler: (Message) -> Unit): Disposable {
        return messageSink.asFlux()
            .filter(filter)
            .subscribe(handler)
    }

}