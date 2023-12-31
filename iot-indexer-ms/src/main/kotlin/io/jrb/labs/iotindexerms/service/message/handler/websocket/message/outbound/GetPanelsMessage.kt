package io.jrb.labs.iotindexerms.service.message.handler.websocket.message.outbound

data class GetPanelsMessage(
    override val id: Number = 0,
    override val type: OutboundMessage.MessageType = OutboundMessage.MessageType.get_panels
) : OutboundMessage<GetPanelsMessage> {
    override fun copy(newId: Long): GetPanelsMessage = copy(id = newId)
}
