package io.jrb.labs.iotindexerms.service.message.handler.websocket.message

import com.fasterxml.jackson.annotation.JsonProperty

data class AuthMessage(
    val type: String = "auth",
    @JsonProperty("access_token") val accessToken: String
)
