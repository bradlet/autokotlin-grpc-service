package com.autokotlin.ktor

import com.autokotlin.ktor.model.Hello
import com.google.gson.Gson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
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

    "test /downstream relays the body fetched from the downstream HttpClient" {
        // Use MockEngine so the test doesn't hit jsonplaceholder.typicode.com over the network.
        val fixture = Todo(userId = 1, id = 1, title = "mock", completed = false)
        val mockEngine = MockEngine { _ ->
            respond(
                content = Gson().toJson(fixture),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        testApplication {
            application {
                install(ContentNegotiation) { gson() }
                configureRouting(buildHttpClient(mockEngine))
            }
            val response = createClient { }.get("/downstream")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe Gson().toJson(fixture)
        }
    }
})
