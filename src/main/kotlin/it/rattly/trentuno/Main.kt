package it.rattly.trentuno

import io.github.cdimascio.dotenv.dotenv
import me.jakejmattson.discordkt.dsl.bot

val dotenv = dotenv { ignoreIfMissing = true }
fun main() {
    bot(dotenv["TOKEN"] ?: throw IllegalStateException("Token not set")) {
        presence {
            playing("trentuno [v3.0]")
        }
    }
}