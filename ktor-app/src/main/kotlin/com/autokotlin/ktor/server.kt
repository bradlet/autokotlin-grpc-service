package com.autokotlin.ktor

import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    logger.info { args.joinToString { " | " } }
}
