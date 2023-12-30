package io.jrb.labs.iotindexerms.service.message.handler.websocket.message.outbound

import com.fasterxml.jackson.annotation.JsonProperty

data class AuthMessage(
    val type: OutboundMessage.MessageType = OutboundMessage.MessageType.auth,
    @JsonProperty("access_token") val accessToken: String
)
