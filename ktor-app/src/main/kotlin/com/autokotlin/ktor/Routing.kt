package com.autokotlin.ktor

import com.autokotlin.ktor.model.Hello
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

private val logger = KotlinLogging.logger { }

// Free public API used by the /downstream example. Returns a small JSON todo object,
// which we deserialize into [Todo] below.
private const val DOWNSTREAM_TODO_URL = "https://jsonplaceholder.typicode.com/todos/1"

/**
 * Shape of the JSONPlaceholder todo response — kept here next to the route so the
 * example is fully self-contained. Gson handles the JSON ↔ data-class conversion
 * via the ContentNegotiation plugin installed on the HttpClient.
 */
data class Todo(
    val userId: Int,
    val id: Int,
    val title: String,
    val completed: Boolean,
)

fun Application.configureRouting(httpClient: HttpClient) {
    // Locate the OpenAPI spec across the environments this app actually runs in:
    //   - `openapi/api.yaml`              local `./gradlew :ktor-app:run` from repo root
    //   - `resources/openapi/api.yaml`    when packaged as resources
    //   - `/app/resources/openapi/...`    inside the jib-built container (extraDirectories puts it here)
    //   - `../openapi/api.yaml`           gradle test/run working dir is the subproject dir
    val openApiFile = File("openapi/api.yaml").takeIf { it.exists() }
        ?: File("resources/openapi/api.yaml").takeIf { it.exists() }
        ?: File("/app/resources/openapi/api.yaml").takeIf { it.exists() }
        ?: File("../openapi/api.yaml").takeIf { it.exists() }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        // Example endpoint demonstrating the OpenAPI codegen flow:
        // `Hello` is generated from openapi/api.yaml into com.autokotlin.ktor.model.Hello
        // and serialized by the Gson content-negotiation plugin installed in Server.kt.
        get("/hello") {
            call.respond(Hello(message = "Hello World!"))
        }

        // Example of calling a downstream HTTP API with Ktor's HttpClient. The client
        // is created once in Server.kt#module() and shared across requests — do NOT
        // construct a new HttpClient per request; each one spins up its own engine.
        get("/downstream") {
            val todo = httpClient.get(DOWNSTREAM_TODO_URL).body<Todo>()
            logger.info { "fetched downstream todo id=${todo.id} title=${todo.title}" }
            call.respond(todo)
        }

        if (openApiFile != null) {
            swaggerUI(path = "docs", swaggerFile = openApiFile.canonicalPath)
            get("/openapi.yaml") {
                call.respondFile(openApiFile)
            }
        } else {
            logger.warn { "OpenAPI file not found; /docs and /openapi.yaml will be unavailable." }
        }
    }
}
