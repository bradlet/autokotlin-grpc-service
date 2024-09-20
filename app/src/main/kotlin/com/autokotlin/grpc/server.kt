package com.autokotlin.grpc

import io.grpc.BindableService
import io.grpc.ServerBuilder
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.protobuf.services.HealthStatusManager
import kotlin.coroutines.EmptyCoroutineContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger(GrpcServer::class.java.simpleName)

class GrpcServer(
    configurator: GrpcServerConfiguration.() -> Unit
) {
    class GrpcServerConfiguration {
        internal lateinit var healthStatusManager: HealthStatusManager
        private val bindableServices = mutableListOf<BindableService>()

        var port: Int = 6565
        var enableHealthChecks: Boolean = true
        var enableReflection: Boolean = true

        fun addService(service: BindableService) {
            bindableServices.add(service)
        }

        fun addServices(vararg services: BindableService) {
            bindableServices.addAll(services)
        }

        internal fun build() = ServerBuilder
            .forPort(port)
            .apply {
                if (enableReflection) {
                    addService(ProtoReflectionService.newInstance())
                }
                if (enableHealthChecks) {
                    healthStatusManager = HealthStatusManager()
                    addServices(healthStatusManager.healthService)
                }
            }
            .build()

        internal fun signalServerUp() {
            if (enableHealthChecks) {
                bindableServices.forEach {
                    healthStatusManager.setStatus(it.javaClass.simpleName, HealthCheckResponse.ServingStatus.SERVING)
                }
            }
        }

        internal fun signalServerDown() {
            if (enableHealthChecks) {
                bindableServices.forEach {
                    healthStatusManager.setStatus(it.javaClass.simpleName, HealthCheckResponse.ServingStatus.NOT_SERVING)
                }
            }
        }
    }
    private val configuration = GrpcServerConfiguration().apply(configurator)
    private val server = configuration.build()

    fun start() {
        server.start()
        println("Server started, listening on ${configuration.port}")
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
