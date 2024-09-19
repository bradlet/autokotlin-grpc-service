package com.autokotlin.grpc

import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import kotlin.coroutines.EmptyCoroutineContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class GrpcServer(
    private val port: Int = 6565,
    private val devMode: Boolean = true,
    serverConfiguration: ServerBuilder<*>.() -> Unit
) {
    private val server = ServerBuilder
        .forPort(port)
        .apply(serverConfiguration)
        .apply {
            if (devMode) {
                addService(ProtoReflectionService.newInstance())
            }
        }
        .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                stop()
                println("*** server shut down")
            },
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }
}

fun main(args: Array<String>) {
    logger.info { args.joinToString { " | " } }

    GrpcServer {
        addService(HelloWorldService(EmptyCoroutineContext))
    }.let { server ->
        server.start()
        server.blockUntilShutdown()
    }
}
