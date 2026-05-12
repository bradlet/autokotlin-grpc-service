package com.autokotlin.ktor

import com.autokotlin.ktor.model.Hello
import com.google.gson.Gson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class ServerTests: StringSpec({

    "test server responds hello world" {
        testApplication {
            application { module() }
            val response = createClient { }.get("/")
            response.bodyAsText() shouldBe "Hello World!"
        }
    }

    "test /hello serializes the generated Hello model" {
        testApplication {
            application { module() }
            val response = createClient { }.get("/hello")
            response.bodyAsText() shouldBe Gson().toJson(Hello(message = "Hello World!"))
        }
    }

    "test /openapi.yaml serves the spec" {
        testApplication {
            application { module() }
            val response = createClient { }.get("/openapi.yaml")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "title: Autokotlin Service API"
        }
    }
})
