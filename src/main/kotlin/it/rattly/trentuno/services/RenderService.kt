package it.rattly.trentuno.services

import it.rattly.trentuno.games.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.*
import java.awt.image.BufferedImage

object RenderService {
    private const val LINE_WIDTH = 6
    private const val SPACE_BETWEEN_CARDS = 3

    suspend fun renderSingleCard(card: Card) = withContext(Dispatchers.IO) {
        card.image().let { img ->
            BufferedImage(
                img.width + LINE_WIDTH, img.height + LINE_WIDTH,
                BufferedImage.TYPE_INT_ARGB
            ).also { drawCard(card, makeGraphics(it), img, 0) }
        }
    }

    suspend fun renderCards(cards: List<Card>) = withContext(Dispatchers.IO) {
        if (cards.size == 1) return@withContext renderSingleCard(cards.first())
        val drawnImages = cards.associateBy { it.image() }.let { allCardImages ->
            allCardImages.toList().chunked(3).map { cardImages ->
                BufferedImage(
                    cardImages.sumOf { it.first.width } + LINE_WIDTH + (SPACE_BETWEEN_CARDS * (cardImages.size + 2)),
                    cardImages.minOf { it.first.height } + LINE_WIDTH,
                    BufferedImage.TYPE_INT_ARGB
                ).also { buff ->
                    makeGraphics(buff).also { g ->
                        cardImages.forEachIndexed { index, (img, card) ->
                            drawCard(
                                card,
                                g,
                                img,
                                cardImages.take(index).sumOf { it.first.width + LINE_WIDTH + SPACE_BETWEEN_CARDS })
                        }
                    }
                }
            }
        }

        return@withContext if (drawnImages.size == 1) drawnImages.first() else
            BufferedImage(
                drawnImages.first().width,
                drawnImages.sumOf { it.height },
                BufferedImage.TYPE_INT_ARGB
            ).also { buff ->
                makeGraphics(buff).also { g ->
                    drawnImages.forEachIndexed { index, card ->
                        g.drawImage(card, 0, index * card.height, null)
                    }
                }
            }
    }

    private fun drawCard(card: Card, g: Graphics2D, img: BufferedImage, x: Int) {
        val text = "${card.value}"

        g.drawRect(x, 0, img.width + (LINE_WIDTH / 2), img.height + (LINE_WIDTH / 2))
        g.drawImage(img, x + (LINE_WIDTH / 2), 0 + (LINE_WIDTH / 2), null)
        g.drawString(text, (x + img.width) - g.fontMetrics.stringWidth(text) - 5, 25)
    }

    private fun makeGraphics(buff: BufferedImage) = buff.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        stroke = BasicStroke(LINE_WIDTH.toFloat())
        color = Color.BLACK
        font = Font("Arial", Font.BOLD, 20)
    }
}