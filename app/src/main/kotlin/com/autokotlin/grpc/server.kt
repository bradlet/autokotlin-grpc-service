package com.autokotlin.grpc

import com.lib.GrpcServer

fun main() {
    GrpcServer {
        port = 6565
        enableReflection = true
        enableHealthChecks = true
        addServices(
            HelloWorldService(),
        )
    }.let { server ->
        server.start()
        server.blockUntilShutdown()
    }
}
