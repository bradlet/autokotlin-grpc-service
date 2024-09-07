plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
//    alias(libs.plugins.jib)
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(libs.guava) // Maybe remove but I kinda like just having it around

    // GRPC & Protobuf
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.protobuf.kotlin)

    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
        // TODO: If jdk 8 is actually needed because of grpc-kotlin, change here
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
        proto {
            setSrcDirs(
                mutableListOf(
                    "../protos",
                )
            )
        }
    }
    test {
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