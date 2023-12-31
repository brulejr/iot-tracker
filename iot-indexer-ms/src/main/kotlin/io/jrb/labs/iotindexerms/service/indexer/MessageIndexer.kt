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
package io.jrb.labs.iotindexerms.service.indexer

import io.jrb.labs.common.eventbus.EventBus
import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.iotindexerms.model.MessageEvent
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component

@Component
class MessageIndexer(
    private val eventBus: EventBus
) {

    private val log by LoggerDelegate()

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @PostConstruct
    fun init() {
        log.info("Starting {}...", javaClass.simpleName)
        _scope.launch {
            eventBus.events(MessageEvent::class).collectLatest { event -> processMessageEvent(event) }
        }
    }

    private fun processMessageEvent(event: MessageEvent) {
        log.info("{}::{}", event.source, event.data)
    }

}