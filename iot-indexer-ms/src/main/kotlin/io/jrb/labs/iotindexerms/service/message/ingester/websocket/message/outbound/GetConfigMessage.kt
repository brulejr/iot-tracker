package io.jrb.labs.iotindexerms.service.message.ingester.websocket.message.outbound

data class GetConfigMessage(
    override val id: Number = 0,
    override val type: OutboundMessage.MessageType = OutboundMessage.MessageType.get_config
) : OutboundMessage<GetConfigMessage> {
    override fun copy(newId: Long): GetConfigMessage = copy(id = newId)
}
