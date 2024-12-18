plugins {
    kotlin("jvm")
    alias(libs.plugins.ktor)
    alias(libs.plugins.jib)
    application
}

val main = "com.autokotlin.ktor.ServerKt"
val imageRepo: String? by extra // Specify different repo from the default with -PimageRepo=...

application {
    mainClass = main
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation(libs.guava) // Maybe remove but I kinda like just having it around
    implementation(libs.logback)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlin.coroutines)

    // Ktor dependencies
    implementation(libs.ktor.server)
    implementation(libs.ktor.netty)
    implementation(libs.ktor.content.negotation)
    implementation(libs.ktor.gson)
    // implementation(libs.ktor.status.pages
    testImplementation(libs.ktor.server.tests)

    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.kotest.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.runner)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

tasks.named<Test>("test") {
    // Gradle will cache task results if the task inputs & outputs are unchanged, meaning `test` won't really run every
    // time you execute the `test` task. The following line disables that caching behavior so that the tests always run.
    outputs.upToDateWhen { false }

    // Use JUnit Platform for unit tests.
    useJUnitPlatform()

    // Show standard output for tests
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }

    afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
        if (desc.parent == null) { // will match the outermost suite
            println("Result: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
        }
    }, this, this))
}

sourceSets {
    main {
        kotlin {
            srcDir("src/main/kotlin")
        }
    }
    test {
        kotlin {
            srcDir("src/test/kotlin")
        }
    }
}

jib {
    from {
        image = "eclipse-temurin:22"
    }
    to {
        image = imageRepo ?: "bradlet2/autokotlin-ktor-service"
    }
    container {
        mainClass = main
        ports = listOf("8080")
    }
}
