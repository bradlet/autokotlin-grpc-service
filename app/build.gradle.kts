plugins {
    kotlin("jvm")
    alias(libs.plugins.protobuf)
    alias(libs.plugins.jib)
    application
}

val main = "com.autokotlin.grpc.ServerKt"

application {
    mainClass = main
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))

    implementation(libs.guava) // Maybe remove but I kinda like just having it around
    implementation(libs.logback)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlin.coroutines)

    // GRPC & Protobuf
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.services)
    implementation(libs.protobuf)
    implementation(libs.protobuf.kotlin)

    // Test dependencies
    testImplementation(testFixtures(project(":lib")))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.kotest.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.grpc.testing)
    testImplementation(libs.grpc.inprocess)

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
        proto {
            setSrcDirs(
                mutableListOf(
                    "../protos",
                )
            )
        }
    }
    test {
        kotlin {
            srcDir("src/test/kotlin")
        }
        proto {
            setSrcDirs(
                mutableListOf(
                    "../protos",
                )
            )
        }
    }
}

protobuf {
    protoc {
        artifact = libs.protoc.asProvider().get().toString()
    }
    plugins {
        create("grpc") {
            artifact = libs.protoc.gen.grpc.java.get().toString()
        }
        create("grpckt") {
            artifact = "${libs.protoc.gen.grpc.kotlin.get().toString()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

jib {
    from {
        image = "eclipse-temurin:22"
    }
    to {
        image = "bradlet2/autokotlin-grpc-service"
    }
    container {
        mainClass = main
        ports = listOf("6565")
    }
}