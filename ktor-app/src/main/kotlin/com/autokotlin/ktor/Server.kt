package com.autokotlin.ktor

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*

//import mu.KotlinLogging

//private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        gson()
    }
    configureRouting()
}