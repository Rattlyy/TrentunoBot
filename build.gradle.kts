plugins {
    kotlin("jvm") version "1.9.23"
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
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}