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
package io.jrb.labs.module.indexer.entityStateChange

import io.jrb.labs.module.event.EntityStateChange
import io.jrb.labs.module.indexer.MessageIndexer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Instant

@Component
class EntityStateChangeIndexer(private val deviceEntityDocumentRepository: DeviceEntityDocumentRepository) :
    MessageIndexer<EntityStateChange> {

    override fun index(message: EntityStateChange): Flow<Any> {
        val oldState = message.data.oldState
        val newState = message.data.newState
        val timestamp = Instant.now()
        return deviceEntityDocumentRepository.findByEntityId(oldState.entityId)
            .switchIfEmpty { Mono.just(DeviceEntityDocument(entityId = newState.entityId, createdOn = timestamp)) }
            .map { it.copy(
                entityId = newState.entityId,
                state = newState.state,
                stateClass = newState.attributes?.stateClass,
                unitOfMeasurement = newState.attributes?.unitOfMeasurement,
                deviceClass = newState.attributes?.deviceClass,
                modifiedOn = timestamp
            ) }
            .flatMap { deviceEntityDocumentRepository.save(it) }
            .asFlow()
    }

}