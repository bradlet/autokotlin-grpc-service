package com.autokotlin.ktor

import com.google.gson.Gson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*

class ServerTests: StringSpec({

    "test server responds hello world" {
        testApplication {
            val response = createClient { }.get("/")
            response.bodyAsText() shouldBe "Hello World!"
        }
    }

    "test server serializes response" {
        testApplication {
            val response = createClient { }.get("/blob")
            response.bodyAsText() shouldBe Gson().toJson(Response("Hello World!"))
        }
    }
})