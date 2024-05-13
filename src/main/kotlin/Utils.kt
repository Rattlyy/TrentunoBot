package it.rattly

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.create.AbstractMessageCreateBuilder
import dev.kord.rest.builder.message.create.InteractionResponseCreateBuilder
import dev.kord.x.emoji.DiscordEmoji
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import it.rattly.services.RenderService
import me.jakejmattson.discordkt.dsl.listeners
import me.jakejmattson.discordkt.util.toPartialEmoji
import me.jakejmattson.discordkt.util.uuid
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

val Snowflake.mention: String
    get() = "<@$this>"

fun AbstractMessageCreateBuilder.addImage(img: BufferedImage) {
    addFile(
        "img.png",
        ChannelProvider {
            ByteReadChannel(ByteArrayOutputStream().use {
                ImageIO.write(img, "png", it)
                it.toByteArray()
            })
        }
    )
}

private val actions = mutableMapOf<String, suspend ButtonInteractionCreateEvent.() -> Unit>()
fun ActionRowBuilder.button(
    label: String?,
    emoji: DiscordEmoji? = null,
    style: ButtonStyle = ButtonStyle.Secondary,
    disabled: Boolean = false,
    expiration: Long,
    action: suspend ButtonInteractionCreateEvent.() -> Unit
) {
    val uuid = uuid()

    interactionButton(style, uuid) {
        this.label = label
        this.emoji = emoji?.toPartialEmoji()
        this.disabled = disabled
    }

    val expire = System.currentTimeMillis() + expiration
    actions[uuid] = {
        if (System.currentTimeMillis() > expire) {
            actions.remove(uuid)
            interaction.respondEphemeral { content = "This button has expired!" }
        } else action()
    }
}

fun miscListeners() = listeners {
    on<ButtonInteractionCreateEvent> {
        actions[interaction.component.customId]?.invoke(this) ?: interaction.respondEphemeral {
            content = "This button has expired!"
        }
    }
}