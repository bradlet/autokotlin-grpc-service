package com.autokotlin.grpc

import helloworld.HelloWorldGrpcKt
import helloworld.Service.HelloRequest
import helloworld.Service.HelloReply
import helloworld.helloReply
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class HelloWorldService(
    coroutineContext: CoroutineContext = EmptyCoroutineContext
): HelloWorldGrpcKt.HelloWorldCoroutineImplBase(coroutineContext) {
    override suspend fun sayHello(request: HelloRequest): HelloReply {
        return helloReply {
            message = "Hello ${request.name}"
        }
    }
}