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
import io.jrb.labs.common.scheduler.RunnableTask
import io.jrb.labs.module.event.Message
import io.jrb.labs.module.event.MessageType
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
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
        val handler = when ( restServerConfig.methodType) {
            MethodType.GET -> buildGet(responseClass)
            MethodType.POST -> buildPost(responseClass)
        }
        val response = handler
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

    private fun buildGet(responseClass: Class<*>): Flux<out Any> {
        val requestSpec = webClient.get()
            .uri(restServerConfig.url)
            .accept(MediaType.APPLICATION_JSON)
        if (restServerConfig.accessToken != null) {
            requestSpec.header("Authorization", "Bearer ${restServerConfig.accessToken}")

        }
        return requestSpec
            .retrieve()
            .bodyToFlux(responseClass)
    }

    private fun buildPost(responseClass: Class<*>): Flux<out Any> {
        val requestSpec = webClient.post()
            .uri(restServerConfig.url)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_PLAIN)
        if (restServerConfig.accessToken != null) {
            requestSpec.header("Authorization", "Bearer ${restServerConfig.accessToken}")
        }
        if (restServerConfig.requestBody != null) {
            requestSpec.body(BodyInserters.fromValue(restServerConfig.requestBody))
        }
        return requestSpec
            .retrieve()
            .bodyToFlux(responseClass)
    }

}