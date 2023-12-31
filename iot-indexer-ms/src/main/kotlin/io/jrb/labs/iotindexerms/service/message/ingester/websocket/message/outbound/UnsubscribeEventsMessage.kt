package io.jrb.labs.iotindexerms.service.message.ingester.websocket.message.outbound

data class UnsubscribeEventsMessage(
    override val id: Number = 0,
    override val type: OutboundMessage.MessageType = OutboundMessage.MessageType.unsubscribe_events,
    val subscription: Number
) : OutboundMessage<UnsubscribeEventsMessage> {
    override fun copy(newId: Long): UnsubscribeEventsMessage = copy(id = newId)
}
