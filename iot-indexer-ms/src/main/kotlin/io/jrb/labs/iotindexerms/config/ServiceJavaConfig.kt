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
package io.jrb.labs.iotindexerms.config

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import io.jrb.labs.common.eventbus.EventBus
import io.jrb.labs.common.eventbus.EventLogger
import io.jrb.labs.iotindexerms.service.message.handler.MessageHandler
import io.jrb.labs.iotindexerms.service.message.handler.mqtt.MqttClientFactory
import io.jrb.labs.iotindexerms.service.message.handler.mqtt.MqttMessageHandler
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    MessageBrokersConfig::class,
    MessageRoutingConfig::class
)
class ServiceJavaConfig {

    @Bean
    fun eventBus() = EventBus()

    @Bean
    fun eventLogger(eventBus: EventBus) = EventLogger(eventBus)

    @Bean
    fun messageHandlers(messageBrokersConfig: MessageBrokersConfig): Map<String, MessageHandler> {
        return messageBrokersConfig.mqtt.mapValues { createMqttMessageHandler(it.value) }
    }

    private fun createMqttMessageHandler(brokerConfig: MqttBrokerConfig): MessageHandler {
        val connectionFactory = MqttClientFactory(brokerConfig)
        return MqttMessageHandler(brokerConfig, connectionFactory)
    }

}