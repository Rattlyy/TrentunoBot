package it.rattly.services

import it.rattly.objects.Card
import java.awt.*
import java.awt.image.BufferedImage

object RenderService {
    private const val LINE_WIDTH = 6
    private const val SPACE_BETWEEN_CARDS = 3

    fun renderSingleCard(card: Card) =
        card.image().let { img ->
            BufferedImage(
                img.width + LINE_WIDTH, img.height + LINE_WIDTH,
                BufferedImage.TYPE_INT_ARGB
            ).also { drawCard(card, makeGraphics(it), img, 0) }
        }

    fun renderCards(cards: List<Card>): BufferedImage =
        cards.map { it.image() }.let { cardImages ->
            BufferedImage(
                cardImages.sumOf { it.width } + LINE_WIDTH + (SPACE_BETWEEN_CARDS * (cardImages.size + 2)),
                cardImages.minOf { it.height } + LINE_WIDTH,
                BufferedImage.TYPE_INT_ARGB
            ).also { buff ->
                makeGraphics(buff).also { g ->
                    cards.forEachIndexed { index, card ->
                        drawCard(
                            card,
                            g,
                            cardImages[index],
                            cardImages.take(index).sumOf { it.width + LINE_WIDTH + SPACE_BETWEEN_CARDS })
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