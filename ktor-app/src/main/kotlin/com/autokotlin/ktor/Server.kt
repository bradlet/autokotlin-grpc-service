package com.autokotlin.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        gson()
    }
    // Shared HttpClient used by routes to call downstream APIs. Closed on shutdown so
    // the CIO engine releases its worker threads cleanly.
    val httpClient = buildHttpClient(CIO.create())
    monitor.subscribe(ApplicationStopped) { httpClient.close() }

    configureRouting(httpClient)
}

/**
 * Builds the HttpClient used for outbound calls. Factored out so tests can swap in
 * `MockEngine` without spinning up a real network engine.
 */
fun buildHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ClientContentNegotiation) {
        gson()
    }
    // Exceptions callers of this client may need to handle:
    //   - io.ktor.client.plugins.RedirectResponseException (3xx, via expectSuccess)
    //   - io.ktor.client.plugins.ClientRequestException (4xx, via expectSuccess)
    //   - io.ktor.client.plugins.ServerResponseException (5xx, via expectSuccess)
    //   - java.nio.channels.UnresolvedAddressException
    //   - io.ktor.serialization.JsonConvertException (from body<T>() deserialization)
    expectSuccess = true
}
