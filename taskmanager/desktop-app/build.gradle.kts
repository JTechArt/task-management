plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    jacoco
}

val composeVersion: String by project
val coroutinesVersion: String by project
val kotlinLoggingVersion: String by project
val logbackVersion: String by project
val junitVersion: String by project

dependencies {
    // Core module
    implementation(project(":core"))

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Testing
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
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

compose.desktop {
    application {
        mainClass = "com.aitask.desktop.TaskManagerAppKt"
        javaHome = System.getProperty("java.home")
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "TaskManager"
            packageVersion = "1.0.0"
            description = "AI-assisted task workspace manager"
            vendor = "AiTask"
            
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
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

