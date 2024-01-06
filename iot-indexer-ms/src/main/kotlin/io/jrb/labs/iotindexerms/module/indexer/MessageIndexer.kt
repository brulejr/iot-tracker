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
package io.jrb.labs.iotindexerms.module.indexer

import io.jrb.labs.common.eventbus.EventBus
import io.jrb.labs.common.logging.LoggerDelegate
import io.jrb.labs.iotindexerms.model.EntityStateChange
import io.jrb.labs.iotindexerms.model.MessageEvent
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Instant

@Component
class MessageIndexer(
    private val eventBus: EventBus,
    private val deviceEntityRepository: DeviceEntityRepository
) {

    private val log by LoggerDelegate()

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @PostConstruct
    @OptIn(ExperimentalCoroutinesApi::class)
    fun init() {
        log.info("Starting {}...", javaClass.simpleName)
        _scope.launch {
            eventBus.events(MessageEvent::class)
                .flatMapConcat { processEvent(it) }
                .collectLatest { log.debug("Indexed: {}", it) }
        }
    }

    private fun processEvent(event: MessageEvent): Flow<Any> {
        val payload = event.data.payload
        return when (payload.javaClass) {
            EntityStateChange::class.java -> index(payload as EntityStateChange)
            else -> flowOf()
        }
    }

    private fun index(entityStateChange: EntityStateChange): Flow<DeviceEntity> {
        val oldState = entityStateChange.data.oldState
        val newState = entityStateChange.data.newState
        val timestamp = Instant.now()
        return deviceEntityRepository.findByEntityId(oldState.entityId)
            .switchIfEmpty { Mono.just(DeviceEntity(entityId = newState.entityId, createdOn = timestamp)) }
            .map { it.copy(
                entityId = newState.entityId,
                state = newState.state,
                stateClass = newState.attributes?.stateClass,
                unitOfMeasurement = newState.attributes?.unitOfMeasurement,
                deviceClass = newState.attributes?.deviceClass,
                modifiedOn = timestamp
            ) }
            .flatMap { deviceEntityRepository.save(it) }
            .asFlow()
    }

}