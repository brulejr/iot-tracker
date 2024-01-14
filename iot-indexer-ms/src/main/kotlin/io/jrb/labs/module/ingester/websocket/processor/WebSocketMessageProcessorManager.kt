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
package io.jrb.labs.module.ingester.websocket.processor

import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.module.event.Message
import io.jrb.labs.module.ingester.websocket.correlator.MessageCorrelation
import io.jrb.labs.module.ingester.websocket.message.inbound.InboundMessage
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import java.util.function.Predicate

@Service
class WebSocketMessageProcessorManager(
    private val messageProcessors: Map<String, MessageProcessor<*>>,
) {
    private val log by LoggerDelegate()

    private val messageSink: Sinks.Many<Message> = Sinks.many().multicast().onBackpressureBuffer()

    fun processMessage(messageCorrelation: MessageCorrelation) {
        val mp = findMessageProcessor(messageCorrelation.inbound)
        mp.processMessage(messageCorrelation.inbound)?.let {
            messageSink.emitNext(it) { _: SignalType?, _: Sinks.EmitResult? ->
                log.debug("Unable to emit message - {}", it)
                false
            }
        }
    }

    fun stream(): Flux<Message> {
        return messageSink.asFlux()
    }

    fun subscribe(filter: Predicate<Message>, handler: (Message) -> Unit): Disposable {
        return messageSink.asFlux()
            .filter(filter)
            .subscribe(handler)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findMessageProcessor(inboundMessage: T): MessageProcessor<T>  where T: InboundMessage {
        val processorKey = "${inboundMessage.javaClass.simpleName}Processor".replaceFirstChar { it.lowercase() }
        return if (messageProcessors.containsKey(processorKey)) {
            messageProcessors[processorKey] as MessageProcessor<T>
        } else {
            messageProcessors["inboundMessageProcessor"] as MessageProcessor<InboundMessage>
        }
    }

}