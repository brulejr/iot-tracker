package io.jrb.labs.iotindexerms.service.message.handler.websocket.message.outbound

data class GetServicesMessage(
    override val id: Number = 0,
    override val type: OutboundMessage.MessageType = OutboundMessage.MessageType.get_services
) : OutboundMessage<GetServicesMessage> {
    override fun copy(newId: Long): GetServicesMessage = copy(id = newId)
}
