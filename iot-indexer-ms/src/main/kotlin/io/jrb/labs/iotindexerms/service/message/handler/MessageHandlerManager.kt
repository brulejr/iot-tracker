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
package io.jrb.labs.iotindexerms.service.message.handler

import io.jrb.labs.common.eventbus.EventBus
import io.jrb.labs.common.eventbus.SystemEvent
import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.iotindexerms.config.MessageBrokersConfig
import io.jrb.labs.iotindexerms.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Service
import reactor.core.Disposable
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

@Service
class MessageHandlerManager(
    private val messageBrokersConfig: MessageBrokersConfig,
    private val messageHandlers: Map<String, MessageHandler>,
    private val eventBus: EventBus
) : SmartLifecycle {

    private val log by LoggerDelegate()

    private val _serviceName = javaClass.simpleName
    private val _running: AtomicBoolean = AtomicBoolean()

    private val _subscriptions: MutableMap<String, Disposable?> = mutableMapOf()
    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        log.info("Initializing {}...", _serviceName)
    }

    fun dispose(guid: String) {
        _subscriptions[guid]?.dispose()
    }

    fun dispose() {
        _subscriptions.forEach {
            it.value?.dispose()
        }
    }

    override fun isRunning(): Boolean {
        return _running.get()
    }

    fun publish(name: String, message: Message) {
        messageHandlers.get(name)?.publish(message)
    }

    override fun start() {
        log.info("Starting {}...", _serviceName)
        messageHandlers.forEach {
            _scope.launch {
                val messageHandler: MessageHandler = it.value
                messageHandler.start()
            }
        }
        eventBus.sendEvent(SystemEvent("service.start", _serviceName))
        _running.getAndSet(true)
    }

    override fun stop() {
        log.info("Stopping {}...", _serviceName)
        messageHandlers.forEach {
            it.value.stop()
        }
        eventBus.sendEvent(SystemEvent("service.stop", _serviceName))
        _running.getAndSet(true)
    }

    fun subscribe(name: String, filter: Predicate<Message>, handler: (String, Message) -> Unit): String {
        val guid: String = UUID.randomUUID().toString()
        _subscriptions[guid] = messageHandlers[name]?.subscribe(filter) { m -> handler(name, m) }
        return guid
    }

    fun subscribe(filter: Predicate<Message>, handler: (String, Message) -> Unit): Set<String> {
        messageHandlers.forEach {
            subscribe(it.key, filter, handler)
        }
        return _subscriptions.keys
    }

    fun subscribe(handler: (String, Message) -> Unit): Set<String> {
        return subscribe({ x -> true }, handler)
    }

}