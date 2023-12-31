package io.jrb.labs.iotindexerms.service.message.ingester.websocket.message.outbound

data class PingMessage(
    override val id: Number = 0,
    override val type: OutboundMessage.MessageType = OutboundMessage.MessageType.ping
) : OutboundMessage<PingMessage> {
    override fun copy(newId: Long): PingMessage = copy(id = newId)
}
