package io.jrb.labs.iotindexerms.service.message.handler.websocket.message.outbound

import com.fasterxml.jackson.annotation.JsonProperty

data class SubscribeEventsMessage(
    override val id: Number = 0,
    override val type: OutboundMessage.MessageType = OutboundMessage.MessageType.subscribe_events,
    @JsonProperty("event_type") val eventType: String? = null
) : OutboundMessage<SubscribeEventsMessage> {
    override fun copy(newId: Long): SubscribeEventsMessage = copy(id = newId)
}
