plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    jacoco
}

val exposedVersion: String by project
val hikariVersion: String by project
val flywayVersion: String by project
val postgresVersion: String by project
val coroutinesVersion: String by project
val jgitVersion: String by project
val kotlinLoggingVersion: String by project
val logbackVersion: String by project
val slf4jVersion: String by project
val junitVersion: String by project
val mockkVersion: String by project
val serializationVersion: String by project
val ktorVersion: String by project

dependencies {
    // Kotlin
    api(kotlin("stdlib"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")

    // Logging (use multiplatform artifact for better IDE resolution; resolves to JVM for this module)
    api("io.github.oshai:kotlin-logging:$kotlinLoggingVersion")
    api("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Database
    api("com.zaxxer:HikariCP:$hikariVersion")
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    api("org.flywaydb:flyway-core:$flywayVersion")
    api("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    api("org.postgresql:postgresql:$postgresVersion")

    // HTTP (Slack webhooks, OAuth)
    api("io.ktor:ktor-client-core-jvm:$ktorVersion")
    api("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")

    // Git
    api("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    api("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:$jgitVersion")

    // Testing
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.mockk:mockk-jvm:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testRuntimeOnly("com.h2database:h2:2.2.224")
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src/main/kotlin")
        }
        test {
            kotlin.srcDir("src/test/kotlin")
        }
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
