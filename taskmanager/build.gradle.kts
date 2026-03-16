plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    id("org.jetbrains.compose") version "1.8.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}

allprojects {
    group = "com.aitask"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    val javaToolchainService = extensions.getByType<org.gradle.jvm.toolchain.JavaToolchainService>()
    tasks.withType<Test>().configureEach {
        javaLauncher.set(javaToolchainService.launcherFor {
            languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
        })
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

