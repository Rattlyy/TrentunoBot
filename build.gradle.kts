plugins {
    kotlin("jvm") version "1.9.23"
    id("com.google.cloud.tools.jib") version "3.4.2"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
}

group = "it.rattly"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("me.jakejmattson:DiscordKt:0.24.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation(kotlin("reflect"))

    val exposedVersion = "0.46.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.h2database:h2:1.3.148")
    implementation("org.postgresql:postgresql:42.5.1")
    implementation("com.zaxxer:HikariCP:4.0.2")
}

jib {
    from {
        image = "gcr.io/distroless/java17-debian12"
        platforms {
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }

    to {
        image = "${env.REGISTRY_HOSTNAME.value}/${env.REGISTRY_NAMESPACE.value}/trentunobot"
        tags = setOf("latest")
        auth {
            username = env.REGISTRY_USERNAME.orNull() ?: throw IllegalStateException("Registry username not set")
            password = env.REGISTRY_PASSWORD.orNull() ?: throw IllegalStateException("Registry password not set")
        }
    }

    container {
        jvmFlags = listOf(
            "-Djava.awt.headless=true",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=100",
            "-XX:+UseStringDeduplication"
        )

        mainClass = "it.rattly.trentuno.MainKt"
        labels = mapOf(
            "org.opencontainers.image.source" to "https://github.com/Rattlyy/TrentunoBot",
        )
    }
}

kotlin {
    jvmToolchain(17)
}