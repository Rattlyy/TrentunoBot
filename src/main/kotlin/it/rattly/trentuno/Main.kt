package it.rattly.trentuno

import io.github.cdimascio.dotenv.dotenv
import me.jakejmattson.discordkt.dsl.bot
import org.slf4j.bridge.SLF4JBridgeHandler


@Suppress("unused") // needed to purge pgjdbc logging
val install = SLF4JBridgeHandler.install()
val dotenv = dotenv { ignoreIfMissing = true }

fun main() = bot( dotenv["TOKEN"] ?: error("Token not set")) {
    presence {
        playing("trentuno [v3.0]")
    }
}
//TODO: forcestart
//TODO: i18n