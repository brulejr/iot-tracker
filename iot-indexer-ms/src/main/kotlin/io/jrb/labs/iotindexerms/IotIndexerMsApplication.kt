package io.jrb.labs.iotindexerms

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IotListenerMsApplication

fun main(args: Array<String>) {
	runApplication<IotListenerMsApplication>(*args)
}
