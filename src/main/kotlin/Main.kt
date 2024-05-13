package it.rattly

import io.github.cdimascio.dotenv.dotenv
import me.jakejmattson.discordkt.dsl.bot

val dotenv = dotenv()
fun main() {
    bot(dotenv["TOKEN"]) {
        presence {
            playing("trentuno [v3.0]")
        }
    }
}