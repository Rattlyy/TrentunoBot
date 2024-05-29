plugins {
    kotlin("jvm") version "2.0.0"
    id("com.google.cloud.tools.jib") version "3.4.2"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
}

group = "it.rattly"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("ch.qos.logback:logback-core:1.5.6")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.tuxdude.logback.extensions:logback-colorizer:1.0.1")
    implementation("org.slf4j:jul-to-slf4j:2.1.0-alpha1")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("me.jakejmattson:DiscordKt:0.24.1-SNAPSHOT") {
        exclude(module = "slf4j-simple")
    }

    val ktormVersion = "4.0.0"
    implementation("org.flywaydb:flyway-core:10.13.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.13.0")
    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.ktorm:ktorm-core:$ktormVersion")
    implementation("org.ktorm:ktorm-support-postgresql:$ktormVersion")
    implementation("org.ktorm:ktorm-ksp-annotations:$ktormVersion")
    implementation("org.testcontainers:postgresql:1.19.3")
    ksp("org.ktorm:ktorm-ksp-compiler:$ktormVersion")
}

ksp {
    arg("ktorm.dbNamingStrategy", "lower-snake-case")
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