package com.autokotlin.grpc

import io.grpc.BindableService
import io.grpc.ServerBuilder
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.protobuf.services.HealthStatusManager
import mu.KotlinLogging

private val logger = KotlinLogging.logger(GrpcServer::class.java.simpleName)

class GrpcServer(
    configurator: GrpcServerConfiguration.() -> Unit
) {
    class GrpcServerConfiguration {
        private lateinit var healthStatusManager: HealthStatusManager
        private val bindableServices = mutableListOf<BindableService>()

        var port: Int = 6565
        var enableHealthChecks: Boolean = false
        var enableReflection: Boolean = false

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
                    addService(healthStatusManager.healthService)
                }
                bindableServices.forEach { addService(it) }
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
        logger.info("Server started, listening on ${configuration.port}")
        configuration.signalServerUp()
        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.info("*** shutting down gRPC server since JVM is shutting down")
                stop()
                configuration.signalServerDown()
                logger.info("*** server shut down")
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
