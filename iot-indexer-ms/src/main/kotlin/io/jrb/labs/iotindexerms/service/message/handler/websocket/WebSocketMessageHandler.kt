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
package io.jrb.labs.iotindexerms.service.message.handler.websocket

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.iotindexerms.config.WebSocketServerConfig
import io.jrb.labs.iotindexerms.model.Message
import io.jrb.labs.iotindexerms.service.message.handler.MessageHandler
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.inbound.AuthInvalidMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.outbound.AuthMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.inbound.AuthOkMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.inbound.AuthRequiredMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.ParsedMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.inbound.EventMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.inbound.InboundMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.inbound.ResultMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.outbound.GetConfigMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.outbound.OutboundMessage
import io.jrb.labs.iotindexerms.service.message.handler.websocket.message.outbound.SubscribeEventsMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Predicate

class WebSocketMessageHandler(
    private val webSocketServerConfig: WebSocketServerConfig,
    private val webSocketClientFactory: WebSocketClientFactory,
    private val objectMapper: ObjectMapper
) : MessageHandler, TextWebSocketHandler() {

    private val allNessages = Predicate { _: Message -> true }

    private val log by LoggerDelegate()
    private val running: AtomicBoolean = AtomicBoolean()
    private val messageSink: Sinks.Many<Message> = Sinks.many().multicast().onBackpressureBuffer()
    private var session: WebSocketSession? = null
    private var authenticated: Boolean = false;
    private var messageId: AtomicLong = AtomicLong()

    override fun isRunning(): Boolean {
        return running.get()
    }

    override fun publish(message: Message) {
        TODO("Not yet implemented")
    }

    override fun stream(): Flux<Message> {
        return messageSink.asFlux()
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

    override fun subscribe(handler: (Message) -> Unit): Disposable? {
        return subscribe(allNessages, handler)
    }

    override fun subscribe(filter: Predicate<Message>, handler: (Message) -> Unit): Disposable? {
        return messageSink.asFlux()
            .filter(filter)
            .subscribe(handler)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("afterConnected: session={}", session)
        authenticate(session)
        sendMessage(session, GetConfigMessage())
        sendMessage(session, SubscribeEventsMessage(eventType = "state_changed"))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.debug("handleTextMessage: payloadLength={}, payload={}", message.payloadLength, message.payload)
        val pm = parseMessage(message.payload)
        when (pm.type) {
            "auth_ok" -> processMessage(parseMessage(pm.payload, AuthOkMessage::class.java))
            "auth_required" -> processMessage(parseMessage(pm.payload, AuthRequiredMessage::class.java))
            "auth_invalid" -> processMessage(parseMessage(pm.payload, AuthInvalidMessage::class.java))
            "event" -> processMessage(parseMessage(pm.payload, EventMessage::class.java))
            "result" -> processMessage(parseMessage(pm.payload, ResultMessage::class.java))
            else -> {
                log.info("Unknown {} message type - payload={}", pm.type, pm.payload)
            }
        }
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
        val type = json.get("type").asText()
        return ParsedMessage(type, json)
    }

    private fun <T> parseMessage(json: JsonNode, typeClass: Class<T>): T {
        return objectMapper.treeToValue(json, typeClass)
    }

    private fun processMessage(message: AuthOkMessage) {
        authenticated = true
        log.info("{} :: authenticated={}", message.type, authenticated)
    }

    private fun processMessage(message: AuthInvalidMessage) {
        authenticated = false
        log.info("{} :: authenticated={}, message={}", message.type, authenticated, message.message)
    }

    private fun processMessage(message: AuthRequiredMessage) {
        authenticated = false
        log.info("{} :: authenticated={}", message.type, authenticated)
    }

    private fun processMessage(message: InboundMessage) {
        log.debug("{} :: message={}", message.type, message)
    }

    private fun <T : OutboundMessage<T>> sendMessage(session: WebSocketSession, message: T) {
        val json = objectMapper.writeValueAsString(message.copy(messageId.incrementAndGet()))
        session.sendMessage(TextMessage(json))
    }

}