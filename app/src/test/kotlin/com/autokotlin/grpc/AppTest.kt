package com.autokotlin.grpc

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull

class AppTest {
    @Test
    fun appHasAGreeting() {
        val classUnderTest = App()
        assertNotNull(classUnderTest.greeting, "app should have a greeting")
    }
}
