package com.autokotlin.ktor

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get

data class Response(val message: String)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/blob") {
            call.respond(Response("Hello World!"))
        }
    }
}