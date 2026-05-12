package com.autokotlin.grpc

import com.autokotlin.grpc.HelloWorldService
import com.lib.BaseGrpcServerIntegrationTests
import helloworld.HelloWorldGrpcKt
import helloworld.helloRequest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

/**
 * Test class that uses the in-process transport mechanism to emulate real rpc interactions, allowing for simplified
 * test setup and execution.
 */
class HelloWorldServiceTests: BaseGrpcServerIntegrationTests(
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
