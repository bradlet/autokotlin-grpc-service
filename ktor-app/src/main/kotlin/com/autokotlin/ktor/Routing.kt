package com.autokotlin.ktor

import com.autokotlin.ktor.model.Hello
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

private val logger = KotlinLogging.logger { }

fun Application.configureRouting() {
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
