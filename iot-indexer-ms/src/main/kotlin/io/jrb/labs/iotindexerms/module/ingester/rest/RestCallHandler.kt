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
package io.jrb.labs.iotindexerms.module.ingester.rest

import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.common.scheduler.RunnableTask
import io.jrb.labs.iotindexerms.config.RestServerConfig
import io.jrb.labs.iotindexerms.model.Message
import io.jrb.labs.iotindexerms.model.MessageType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks

class RestCallHandler(
    private val restServerConfig: RestServerConfig,
    private val webClient: WebClient,
    private val messageSink: Sinks.Many<Message>
): RunnableTask {

    private val log by LoggerDelegate()

    override fun id(): String {
        return restServerConfig.brokerName
    }

    override fun run() {
        log.debug("RestCallHandler<{}>:: Running", restServerConfig.brokerName)

        val responseClass = Class.forName(restServerConfig.responseClass)
        val response = webClient.get()
            .uri(restServerConfig.url)
            .retrieve()
            .bodyToFlux(responseClass)
            .collectList()
            .block()

        response?.forEach {
            val message = Message(type = MessageType.NORMAL, topic = responseClass.simpleName, payload = it)
            messageSink.emitNext(message) { _: SignalType?, _: Sinks.EmitResult? ->
                log.debug("Unable to emit message - {}", message)
                false
            }
        }
    }

}