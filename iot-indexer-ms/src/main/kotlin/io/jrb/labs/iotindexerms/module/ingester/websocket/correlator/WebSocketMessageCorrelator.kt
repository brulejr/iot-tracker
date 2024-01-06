/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Jon Brule <brulejr@gmail.com>
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
package io.jrb.labs.iotindexerms.module.ingester.websocket.correlator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.iotindexerms.module.ingester.websocket.message.ParsedMessage
import io.jrb.labs.iotindexerms.module.ingester.websocket.message.inbound.AuthInvalidMessage
import io.jrb.labs.iotindexerms.module.ingester.websocket.message.inbound.AuthOkMessage
import io.jrb.labs.iotindexerms.module.ingester.websocket.message.inbound.AuthRequiredMessage
import io.jrb.labs.iotindexerms.module.ingester.websocket.message.inbound.EventMessage
import io.jrb.labs.iotindexerms.module.ingester.websocket.message.inbound.InboundMessage
import io.jrb.labs.iotindexerms.module.ingester.websocket.message.inbound.PongMessage
import io.jrb.labs.iotindexerms.module.ingester.websocket.message.inbound.ResultMessage
import io.jrb.labs.iotindexerms.module.ingester.websocket.message.outbound.OutboundMessage
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class WebSocketMessageCorrelator(private val objectMapper: ObjectMapper) {

    private val log by LoggerDelegate()
    private val messages: Map<Number, OutboundMessage<*>> = ConcurrentHashMap()

    fun registerOutboundMessage(outbound: OutboundMessage<*>) {
        messages.plus(outbound.id to outbound)
    }

    fun correlateInboundMessage(pim: ParsedMessage): MessageCorrelation {
        val outboundMessage = if (pim.id != null) messages[pim.id] else null
        val inboundMessage: InboundMessage = when (pim.type) {
            "auth_ok" -> parseMessage(pim.payload, AuthOkMessage::class.java)
            "auth_required" -> parseMessage(pim.payload, AuthRequiredMessage::class.java)
            "auth_invalid" -> parseMessage(pim.payload, AuthInvalidMessage::class.java)
            "event" -> parseMessage(pim.payload, EventMessage::class.java)
            "pong" -> parseMessage(pim.payload, PongMessage::class.java)
            "result" -> parseMessage(pim.payload, ResultMessage::class.java)
            else -> {
                log.info("Unknown {} message type - payload={}", pim.type, pim.payload)
                throw IllegalArgumentException("Unknown parsed message type [${pim.type}]!")
            }
        }
        return MessageCorrelation(outboundMessage?.id, outboundMessage?.type, inboundMessage)
    }


    private fun <T> parseMessage(json: JsonNode, typeClass: Class<T>): T {
        return objectMapper.treeToValue(json, typeClass)
    }

}