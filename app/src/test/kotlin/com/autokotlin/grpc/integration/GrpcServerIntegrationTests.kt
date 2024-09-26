package com.autokotlin.grpc.integration

import com.autokotlin.grpc.HelloWorldService
import com.lib.BaseGrpcServerIntegrationTests
import helloworld.HelloWorldGrpcKt
import helloworld.helloRequest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class GrpcServerIntegrationTests: BaseGrpcServerIntegrationTests(
    listOf(HelloWorldService()),
    {
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
    }
)
