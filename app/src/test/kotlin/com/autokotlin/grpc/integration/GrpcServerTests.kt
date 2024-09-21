package com.autokotlin.grpc.integration

import com.autokotlin.grpc.GrpcServer
import com.autokotlin.grpc.HelloWorldService
import helloworld.HelloWorldGrpcKt
import helloworld.helloRequest
import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class GrpcServerIntegrationTest: StringSpec({
    val servicesUnderTest: MutableList<BindableService> = mutableListOf(HelloWorldService())

    lateinit var grpcServer: GrpcServer
    lateinit var inProcessChannel: ManagedChannel

    beforeEach {
        val testServerName = InProcessServerBuilder.generateName()
        val testServerBuilder = InProcessServerBuilder.forName(testServerName)
        grpcServer = GrpcServer(testServerBuilder) {
            servicesUnderTest.forEach { addService(it) }
        }
        grpcServer.start()
        inProcessChannel = InProcessChannelBuilder.forName(testServerName).usePlaintext().build()
    }

    afterEach {
        grpcServer.stop()
    }

    "test HelloWorldService response" {
        runTest {
            val stub = HelloWorldGrpcKt.HelloWorldCoroutineStub(inProcessChannel)

            val response = stub.sayHello(
                helloRequest {
                    name = "Test"
                }
            )

            response.message shouldBe "Hello Test"
        }
    }
})
