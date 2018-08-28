package de.slg.leoapp

import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

const val defaultPort = 25565

fun main(args: Array<String>) {
    var port = defaultPort
    if (args.isNotEmpty() && args[0].toIntOrNull() != null) port = args[0].toInt()

    val server = embeddedServer(Netty, port = port, module = Application::module)
    server.start(wait = true)
}

