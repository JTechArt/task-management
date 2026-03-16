plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    jacoco
}

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

val composeVersion: String by project
val coroutinesVersion: String by project
val kotlinLoggingVersion: String by project
val logbackVersion: String by project
val junitVersion: String by project
val mockkVersion: String by project
val sourceSets = the<SourceSetContainer>()

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
    testImplementation("io.mockk:mockk-jvm:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
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

val localDevelopmentEnvironment = mapOf(
    "DB_HOST" to "localhost",
    "DB_PORT" to "5433",
    "DB_NAME" to "taskmanager",
    "DB_USER" to "taskmanager",
    "DB_PASSWORD" to "taskmanager_local",
    "APP_NAME" to "TaskManager"
)

fun JavaExec.configureDesktopLaunch(debugEnabled: Boolean) {
    group = "application"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("com.aitask.desktop.TaskManagerAppKt")
    workingDir = rootProject.projectDir
    environment(localDevelopmentEnvironment)
    standardInput = System.`in`

    if (debugEnabled) {
        jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
    }
}

tasks.register<JavaExec>("runDesktopApp") {
    description = "Runs the desktop app with local development defaults."
    configureDesktopLaunch(debugEnabled = false)
}

tasks.register<JavaExec>("debugDesktopApp") {
    description = "Runs the desktop app and waits for a debugger on port 5005."
    configureDesktopLaunch(debugEnabled = true)
}

tasks.register<JavaExec>("runDesktopAppNoDb") {
    description = "Runs the desktop app without database bootstrap."
    configureDesktopLaunch(debugEnabled = false)
    environment("BOOTSTRAP_DATABASE", "false")
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
