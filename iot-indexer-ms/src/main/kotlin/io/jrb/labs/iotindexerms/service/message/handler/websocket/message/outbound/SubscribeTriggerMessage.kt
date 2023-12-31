package io.jrb.labs.iotindexerms.service.message.handler.websocket.message.outbound

import com.fasterxml.jackson.annotation.JsonProperty

data class SubscribeTriggerMessage(
    override val id: Number = 0,
    override val type: OutboundMessage.MessageType = OutboundMessage.MessageType.subscribe_trigger,
    val trigger: Trigger
) : OutboundMessage<SubscribeTriggerMessage> {
    override fun copy(newId: Long): SubscribeTriggerMessage = copy(id = newId)

    data class Trigger(
        val platform: String,
        @JsonProperty("entity_id") val entityId: String,
        val from: String,
        val to: String
    )

}
