package io.jrb.labs.iotindexerms.service.message.ingester.websocket.message.outbound

data class GetStatesMessage(
    override val id: Number = 0,
    override val type: OutboundMessage.MessageType = OutboundMessage.MessageType.get_states
) : OutboundMessage<GetStatesMessage> {
    override fun copy(newId: Long): GetStatesMessage = copy(id = newId)
}
