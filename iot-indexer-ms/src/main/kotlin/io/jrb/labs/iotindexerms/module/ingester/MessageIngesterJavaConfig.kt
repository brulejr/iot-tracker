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
package io.jrb.labs.iotindexerms.module.ingester

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.common.scheduler.TaskSchedulerService
import io.jrb.labs.iotindexerms.module.ingester.mqtt.MqttBrokerConfig
import io.jrb.labs.iotindexerms.module.ingester.mqtt.MqttClientFactory
import io.jrb.labs.iotindexerms.module.ingester.mqtt.MqttMessageIngester
import io.jrb.labs.iotindexerms.module.ingester.rest.RestMessageIngester
import io.jrb.labs.iotindexerms.module.ingester.rest.RestServerConfig
import io.jrb.labs.iotindexerms.module.ingester.websocket.WebSocketClientFactory
import io.jrb.labs.iotindexerms.module.ingester.websocket.WebSocketMessageIngester
import io.jrb.labs.iotindexerms.module.ingester.websocket.WebSocketServerConfig
import io.jrb.labs.iotindexerms.module.ingester.websocket.correlator.WebSocketMessageCorrelator
import io.jrb.labs.iotindexerms.module.ingester.websocket.processor.WebSocketMessageProcessorManager
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(
    MessageIngesterConfig::class
)
class MessageIngesterJavaConfig {

    @Bean
    fun taskSchedulerService() = TaskSchedulerService()

    @Bean
    fun webClient() : WebClient {
        return WebClient.builder()
            .codecs { configurer ->
                val objectMapper = configurer.readers.stream()
                    .filter { it is Jackson2JsonDecoder }
                    .map { it as Jackson2JsonDecoder }
                    .map { it.objectMapper }
                    .findFirst()
                    .orElseGet { Jackson2ObjectMapperBuilder.json().build() }
                val decoder = Jackson2JsonDecoder(objectMapper, MediaType.TEXT_PLAIN)
                configurer.customCodecs().registerWithDefaultConfig(decoder)
            }
            .build()
    }

    @Bean
    fun messageHandlers(
        messageIngesterConfig: MessageIngesterConfig,
        webSocketMessageCorrelator: WebSocketMessageCorrelator,
        webSocketMessageProcessorManager: WebSocketMessageProcessorManager,
        objectMapper: ObjectMapper
    ): Map<String, MessageIngester> {
        val mqttHandlers = messageIngesterConfig.mqtt.mapValues { createMqttMessageHandler(it.value) }
        val restHandlers = messageIngesterConfig.rest.mapValues { createRestMessageHandler(it.value) }
        val websocketHandlers = messageIngesterConfig.websocket.mapValues {
            createWebsocketMessageHandler(it.value, webSocketMessageCorrelator, webSocketMessageProcessorManager, objectMapper)
        }
        return mqttHandlers + restHandlers + websocketHandlers
    }

    private fun createMqttMessageHandler(brokerConfig: MqttBrokerConfig): MessageIngester {
        val connectionFactory = MqttClientFactory(brokerConfig)
        return MqttMessageIngester(brokerConfig, connectionFactory)
    }

    private fun createRestMessageHandler(restServerConfig: RestServerConfig) : RestMessageIngester {
        return RestMessageIngester(restServerConfig, taskSchedulerService(), webClient())
    }

    private fun createWebsocketMessageHandler(
        brokerConfig: WebSocketServerConfig,
        webSocketMessageCorrelator: WebSocketMessageCorrelator,
        webSocketMessageProcessorManager: WebSocketMessageProcessorManager,
        objectMapper: ObjectMapper
    ): MessageIngester {
        val connectionFactory = WebSocketClientFactory(brokerConfig)
        return WebSocketMessageIngester(
            brokerConfig,
            connectionFactory,
            webSocketMessageCorrelator,
            webSocketMessageProcessorManager,
            objectMapper
        )
    }

}