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
package io.jrb.labs.iotindexerms.service.ingester.mqtt

import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.iotindexerms.config.MqttBrokerConfig
import io.jrb.labs.iotindexerms.model.Message
import io.jrb.labs.iotindexerms.model.MessageType
import io.jrb.labs.iotindexerms.service.ingester.MessageIngester
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitResult
import reactor.core.publisher.Sinks.Many
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

class MqttMessageIngester(
    private val mqttBrokerConfig: MqttBrokerConfig,
    private val mqttClientFactory: io.jrb.labs.iotindexerms.service.ingester.mqtt.MqttClientFactory
) : MessageIngester, MqttCallback {

    private val allNessages = Predicate { _: Message -> true }

    private val log by LoggerDelegate()
    private val running: AtomicBoolean = AtomicBoolean()
    private val messageSink: Many<Message> = Sinks.many().multicast().onBackpressureBuffer()
    private var mqttClient: MqttClient? = null

    override fun stream(): Flux<Message> {
        return messageSink.asFlux()
    }

    override fun subscribe(handler: (Message) -> Unit): Disposable {
        return subscribe(allNessages, handler)
    }

    override fun subscribe(filter: Predicate<Message>, handler: (Message) -> Unit): Disposable {
        return messageSink.asFlux()
            .filter(filter)
            .subscribe(handler)
    }

    override fun connectionLost(cause: Throwable?) {
        log.info("Connection lost", cause)
        start()
    }

    override fun messageArrived(topic: String?, mqttMessage: MqttMessage?) {
        val type: MessageType = if (topic!!.startsWith("\$SYS")) MessageType.BROKER else MessageType.NORMAL
        val payload = String(mqttMessage!!.payload)
        val message = Message(type = type, topic = topic, payload = payload)
        messageSink.emitNext(message) { _: SignalType?, _: EmitResult? ->
            log.debug("Unable to emit message - {}", message)
            false
        }
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
    }

    override fun start() {
        try {
            log.info("Starting message handler - brokerName={}, topic={}", mqttBrokerConfig.brokerName, mqttBrokerConfig.topic)
            mqttClient = mqttClientFactory.connect()
            if (mqttBrokerConfig.topic != null) {
                mqttClient!!.subscribe(mqttBrokerConfig.topic)
            }
            mqttClient!!.setCallback(this)
            running.set(true)
        } catch (e: MqttException) {
            log.error("Unable to start message handler '{}' - {}", mqttBrokerConfig.brokerName, e.message, e)
        }
    }

    override fun stop() {
        try {
            log.info("Stopping message handler '{}'...", mqttBrokerConfig.brokerName)
            if (mqttClient != null) {
                mqttClient!!.disconnect()
                running.set(false)
            }
        } catch (e: MqttException) {
            log.error("Unable to stop message handler '{}' - {}", mqttBrokerConfig.brokerName, e.message, e)
        }
    }

    override fun isRunning(): Boolean {
        return running.get()
    }

}