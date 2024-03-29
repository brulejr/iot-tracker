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
package io.jrb.labs.module.ingester.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.module.event.Message
import io.jrb.labs.module.ingester.MessageIngester
import io.jrb.labs.module.ingester.websocket.correlator.WebSocketMessageCorrelator
import io.jrb.labs.module.ingester.websocket.message.ParsedMessage
import io.jrb.labs.module.ingester.websocket.message.outbound.AuthMessage
import io.jrb.labs.module.ingester.websocket.message.outbound.OutboundMessage
import io.jrb.labs.module.ingester.websocket.message.outbound.PingMessage
import io.jrb.labs.module.ingester.websocket.message.outbound.SubscribeEventsMessage
import io.jrb.labs.module.ingester.websocket.processor.WebSocketMessageProcessorManager
import org.eclipse.paho.client.mqttv3.MqttException
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import reactor.core.Disposable
import reactor.core.publisher.Flux
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Predicate

class
WebSocketMessageIngester(
    private val webSocketServerConfig: WebSocketServerConfig,
    private val webSocketClientFactory: WebSocketClientFactory,
    private val webSocketMessageCorrelator: WebSocketMessageCorrelator,
    private val webSocketMessageProcessorManager: WebSocketMessageProcessorManager,
    private val objectMapper: ObjectMapper
) : MessageIngester, TextWebSocketHandler() {

    private val allMessages = Predicate { _: Message -> true }

    private val log by LoggerDelegate()
    private val running: AtomicBoolean = AtomicBoolean()
    private var session: WebSocketSession? = null
    private var messageId: AtomicLong = AtomicLong()

    override fun isRunning(): Boolean {
        return running.get()
    }

    override fun stream(): Flux<Message> {
        return webSocketMessageProcessorManager.stream()
    }

    override fun start() {
        try {
            log.info("Starting message handler - brokerName={}", webSocketServerConfig.brokerName)
            session = webSocketClientFactory.connect(this)
            running.set(true)
        } catch (e: MqttException) {
            log.error("Unable to start message handler '{}' - {}", webSocketServerConfig.brokerName, e.message, e)
        }
    }

    override fun stop() {
        try {
            log.info("Stopping message handler '{}'...", webSocketServerConfig.brokerName)
            if (session != null) {
                session!!.close()
                running.set(false)
            }
        } catch (e: MqttException) {
            log.error("Unable to stop message handler '{}' - {}", webSocketServerConfig.brokerName, e.message, e)
        }
    }

    override fun subscribe(handler: (Message) -> Unit): Disposable {
        return subscribe(allMessages, handler)
    }

    override fun subscribe(filter: Predicate<Message>, handler: (Message) -> Unit): Disposable {
        return webSocketMessageProcessorManager.subscribe(filter, handler)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("afterConnected: session={}", session)
        authenticate(session)
        sendMessage(session, PingMessage())
        sendMessage(session, SubscribeEventsMessage(eventType = "state_changed"))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.debug("handleTextMessage: payloadLength={}, payload={}", message.payloadLength, message.payload)
        val pim = parseMessage(message.payload)
        val mc = webSocketMessageCorrelator.correlateInboundMessage(pim)
        webSocketMessageProcessorManager.processMessage(mc)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.info("handleTransportError: session={}", session, exception)
    }

    private fun authenticate(session: WebSocketSession) {
        val json = objectMapper.writeValueAsString(AuthMessage(accessToken = webSocketServerConfig.accessToken))
        session.sendMessage(TextMessage(json))
    }

    private fun parseMessage(payload: String): ParsedMessage {
        val json = objectMapper.readTree(payload)
        val id = json.get("id")?.asLong()
        val type = json.get("type").asText()
        return ParsedMessage(id, type, json)
    }

    private fun <T : OutboundMessage<T>> sendMessage(session: WebSocketSession, message: T) {
        webSocketMessageCorrelator.registerOutboundMessage(message)
        val json = objectMapper.writeValueAsString(message.copy(messageId.incrementAndGet()))
        session.sendMessage(TextMessage(json))
    }

}