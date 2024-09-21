package com.autokotlin.grpc.integration

import com.autokotlin.grpc.GrpcServer
import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult

abstract class BaseGrpcServerIntegrationTests(
    private val servicesUnderTest: List<BindableService>,
    body: StringSpec.() -> Unit
) : StringSpec(body) { // TODO: support other specs
    private lateinit var grpcServer: GrpcServer

    override suspend fun beforeEach(testCase: TestCase) {
        val testServerBuilder = InProcessServerBuilder.forName(testServerName)
        grpcServer = GrpcServer(testServerBuilder) {
            servicesUnderTest.forEach { addService(it) }
        }
        grpcServer.start()

        super.beforeEach(testCase)
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        grpcServer.stop()
        super.afterEach(testCase, result)
    }

    companion object {
        private val testServerName = InProcessServerBuilder.generateName()
        @JvmStatic
        protected val inProcessChannel: ManagedChannel = InProcessChannelBuilder
            .forName(testServerName)
            .usePlaintext()
            .build()
    }
}
