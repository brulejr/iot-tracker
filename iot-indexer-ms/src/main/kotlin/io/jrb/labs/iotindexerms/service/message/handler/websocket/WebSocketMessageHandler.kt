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

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.iotindexerms.config.WebSocketServerConfig
import io.jrb.labs.iotindexerms.model.Message
import io.jrb.labs.iotindexerms.service.message.handler.MessageHandler
import org.eclipse.paho.client.mqttv3.MqttException
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.atomic.AtomicBoolean
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
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.info("handleTextMessage: payloadLength={}, payload={}", message.payloadLength, message.payload)
        val messageType = extractMessageType(message.payload)
        when (messageType) {
            "auth_ok" -> markAuthenticated()
            "auth_required" -> markUnauthenticated()
            "auth_invalid" -> markUnauthenticated()
            else -> {
                log.info("Unknown message type {}", messageType)
            }
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.info("handleTransportError: session={}", session, exception)
    }

    private fun authenticate(session: WebSocketSession) {
        val authMessage = objectMapper.writeValueAsString(AuthMessage(accessToken = webSocketServerConfig.accessToken))
        session.sendMessage(TextMessage(authMessage))
    }

    private fun extractMessageType(payload: String): String {
        val json = objectMapper.readTree(payload)
        return json.get("type").asText()
    }

    private fun markAuthenticated() {
        authenticated = true
        log.info("authenticated={}", authenticated)
    }

    private fun markUnauthenticated() {
        authenticated = false
        log.info("authenticated={}", authenticated)
    }

}