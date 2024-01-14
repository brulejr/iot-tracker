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
package io.jrb.labs.iotindexerms.module.ingester.mqtt

import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.iotindexerms.module.ingester.MessageIngesterException
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttClientPersistence
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

private const val DEFAULT_PASSWORD = ""
private const val DEFAULT_USERNAME = "anonymous"

class MqttClientFactory(private val mqttBrokerConfig: MqttBrokerConfig) {

    private val log by LoggerDelegate()

    fun connect(): MqttClient {
        val mqttBrokerUrl: String = mqttBrokerConfig.tcpUrl
        val mqttClientId: String = MqttClient.generateClientId()
        val mqttPersistence: MqttClientPersistence = MemoryPersistence(

        )
        val mqttConnectionOptions = MqttConnectOptions()
        return try {

            log.info("Connecting to MQTT broker - url={}", mqttBrokerUrl)

            val mqttClient = MqttClient(mqttBrokerUrl, mqttClientId, mqttPersistence)

            mqttConnectionOptions.isCleanSession = true
            mqttConnectionOptions.userName = mqttBrokerConfig.username ?: DEFAULT_USERNAME
            mqttConnectionOptions.password = (mqttBrokerConfig.password ?: DEFAULT_PASSWORD).toCharArray()

            mqttClient.connect(mqttConnectionOptions)

            mqttClient

        } catch (e: MqttException) {
            log.error("Failed to connect to MQTT broker - url={}", mqttBrokerUrl)
            throw MessageIngesterException(e.message, e)
        }
    }

}