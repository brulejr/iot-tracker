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
package io.jrb.labs.iotindexerms.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class EntityStateChange(

    @JsonProperty("event_type")
    val eventType: String,

    val data: EntityStateChangeData,

    val origin: String,

    @JsonProperty("time_fired")
    val timeFired: Instant

)

data class EntityStateChangeData(

    @JsonProperty("old_state")
    val oldState: EntityState,

    @JsonProperty("new_state")
    val newState: EntityState

)

data class EntityState(

    @JsonProperty("entity_id")
    val entityId: String,

    val state: String,

    val attributes: EntityStateAttributes?,

    @JsonProperty("last_changed")
    val lastChanged: Instant,

    @JsonProperty("last_updated")
    val lastUpdated: Instant

)

data class EntityStateAttributes(

    @JsonProperty("state_class")
    val stateClass: String?,

    @JsonProperty("unit_of_measurement")
    val unitOfMeasurement: String,

    @JsonProperty("device_class")
    val deviceClass: String?

)