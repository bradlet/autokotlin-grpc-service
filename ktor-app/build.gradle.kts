plugins {
    kotlin("jvm")
    alias(libs.plugins.ktor)
    alias(libs.plugins.jib)
    alias(libs.plugins.openapi)
    application
}

// `projectName` mirrors the pattern used in the tcg-sandbox template that this one is modeled after.
// It's the single source of truth for the Kotlin package prefix used by both the application's
// main class and the OpenAPI-generated model / api packages. If you rename the `com.autokotlin`
// package across the repo, update this value to match.
val projectName = "autokotlin"
val main = "com.$projectName.ktor.ServerKt"

// Gradle properties that the CI workflow passes via `-PimageRepo=...` / `-PjibCommitHashTag=...`.
// Locally they're typically left unset — the jib `to.image` default + tag default kick in.
val imageRepo: String? by extra
val jibCommitHashTag: String? by extra

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
    implementation(libs.ktor.server.swagger) // Serves Swagger UI from openapi/api.yaml
    // Required because the openapi-generator `library: jvm-ktor` option produces client classes
    // that import ktor-client packages. We don't use the generated client at runtime, but the
    // generated `apiPackage` classes still need this on the classpath to compile.
    implementation(libs.ktor.client.content.negotation)

    testImplementation(libs.ktor.server.test.host)

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

// -----------------------------------------------------------------------------
// OpenAPI codegen
// -----------------------------------------------------------------------------
// The contract lives in `openapi/api.yaml` at the repo root. We hand-edit that file
// and let the generator produce Kotlin model + api classes under `build/generated/openapi/api`.
// `compileKotlin` depends on `generateBackendTypes` so the generated sources are always
// fresh before compilation. The generated source dir is wired into the main sourceSet below.

openApiValidate {
    inputSpec.set("$rootDir/openapi/api.yaml")
    recommend.set(true)
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateBackendTypes") {
    generatorName.set("kotlin")
    inputSpec.set("$rootDir/openapi/api.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated/openapi/api")
    apiPackage.set("com.$projectName.ktor.api")
    modelPackage.set("com.$projectName.ktor.model")
    configOptions.set(
        mapOf(
            "library" to "jvm-ktor", // Use Ktor client instead of OkHttp
            "serializationLibrary" to "gson",
            "dateLibrary" to "string", // Using string instead of java8 dates because of deprecation
        )
    )

    dependsOn(tasks.named("openApiValidate"))
}

tasks.compileKotlin {
    dependsOn(tasks.named("generateBackendTypes"))
}

sourceSets {
    main {
        kotlin {
            srcDirs(
                "src/main/kotlin",
                "${layout.buildDirectory.get()}/generated/openapi/api/src/main/kotlin",
            )
        }
    }
    test {
        kotlin {
            srcDirs(
                "src/test/kotlin",
                "${layout.buildDirectory.get()}/generated/openapi/api/src/main/kotlin",
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Jib: container image build
// -----------------------------------------------------------------------------
// Locally:   ./gradlew :ktor-app:jibDockerBuild
//            Builds the image straight into the local Docker daemon (no registry push).
//            The :latest tag is what docker-compose.yml references.
//
// CI:        ./gradlew :ktor-app:jib -PimageRepo=<gar-repo> -PjibCommitHashTag=<sha>
//            Pushes to your Artifact Registry repo with both :latest and the commit-hash tag.
//
// REPLACE_ME: After running scripts/bootstrap/setup-project.sh and creating an Artifact Registry
// Docker repository in GCP, update the fallback `to.image` host below to match your repo path
// (e.g. us-west1-docker.pkg.dev/<your-project>/<your-repo>). The CI workflow overrides this via
// -PimageRepo, so the default only matters for local jibDockerBuild and one-off pushes.
jib {
    from {
        image = "eclipse-temurin:22"
        platforms {
            // amd64 for Cloud Run / standard linux hosts; arm64 for local Apple-silicon Docker.
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    to {
        image = "${imageRepo ?: "REPLACE_ME-docker.pkg.dev/REPLACE_ME-project/REPLACE_ME-repo"}/ktor-app"
        tags = listOfNotNull("latest", jibCommitHashTag).toSet()
    }
    container {
        mainClass = main
        ports = listOf("8080")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
    extraDirectories {
        // Bake the OpenAPI spec into the image so the running container can serve Swagger UI
        // from /app/resources/openapi/api.yaml. Server.kt resolves this path at startup.
        paths {
            path {
                setFrom(file("$rootDir/openapi"))
                setInto("/app/resources/openapi")
            }
        }
    }
}
