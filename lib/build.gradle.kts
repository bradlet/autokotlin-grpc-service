plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.guava) // Maybe remove but I kinda like just having it around
    implementation(libs.logback)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlin.coroutines)

    // GRpc dependencies
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.services)
    testFixturesImplementation(libs.grpc.testing)
    testFixturesImplementation(libs.grpc.inprocess)

    // Test Fixtures Dependencies
    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.kotlin.coroutines.test)
    testFixturesImplementation(libs.kotest.core)
    testFixturesImplementation(libs.kotest.property)
    testFixturesImplementation(libs.kotest.runner)

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
    testFixtures {
        kotlin {
            srcDir("src/testFixtures/kotlin")
        }
    }
}
