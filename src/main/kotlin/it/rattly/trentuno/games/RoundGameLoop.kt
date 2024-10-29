package it.rattly.trentuno.games

import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import it.rattly.trentuno.addImage
import it.rattly.trentuno.mention
import it.rattly.trentuno.services.GameType
import kotlinx.coroutines.*
import kotlin.properties.Delegates
import kotlin.random.Random.Default.nextInt

abstract class RoundGameLoop(
    channel: TopGuildMessageChannel,
    players: List<Player>,
    deck: MutableList<Card>,
    type: GameType
) : Game(type, channel, players, deck) {
    var playerPoints = mutableMapOf<Player, Int>().also { e -> players.forEach { e[it] = 0 } }
    var waitingAction: WaitingStatus = WaitingStatus.WAITING
    var roundsPassed = 0
    var currentPlayer: Player by Delegates.notNull()
    private var currentTurn = 0
    var secondsPassed = 0

    override suspend fun startGameLoop(kord: Kord): Player? {
        // Game loop runs until someone knocks or the deck is empty
        beforeGameStart()

        try {
            while (deck.isNotEmpty()) {
                roundsPassed += 1
                currentTurn += 1 // Started the turn, take new player or fall back to first player
                currentPlayer = players.getOrElse(
                    if (roundsPassed == 1) nextInt(0, players.size) else currentTurn
                ) {
                    currentTurn = 0
                    players[currentTurn]
                }

                val isOver = CompletableDeferred<Unit>()
                withContext(Dispatchers.Default) {
                    launch { gameLoop(isOver) }.invokeOnCompletion { isOver.complete(Unit) } // Run gameLoop in the background
                }

                // Waiting action: while the player is choosing the action in the background we wait for 60s or skip the turn
                while (!isOver.isCompleted && (secondsPassed < 60)) {
                    println(waitingAction)
                    delay(1000)
                    secondsPassed += 1

                    // Every 10 sec send remaining time alert
                    if (secondsPassed % 10 == 0) {
                        channel.createMessage("${currentPlayer.mention} hai ${60 - secondsPassed} secondi per fare la tua mossa!")
                    }
                }

                // if time is up either pick a card or/and skip the turn
                if (waitingAction.shouldWait) {
                    timeIsUpAction()
                    channel.createMessage("Il tempo è finito ${currentPlayer.mention}! Passo al prossimo giocatore!")
                }

                playerPoints = recalculatePlayerPoints()
                if (winCheck()) throw GameOverException()
            }

            return terminateGame()
        } catch (e: GameOverException) {
            return terminateGame()
        }
    }

    open suspend fun beforeGameStart() {}

    private suspend fun terminateGame(): Player? {
        for (player in players) {
            channel.createMessage {
                content = "Carte di ${player.id.mention}"
                addImage(player.renderDeck())
            }
        }

        return calculateWinner()?.also {
            channel.createMessage {
                content = winMessage(it)
            }
        }
    }

    /* The game loop, must be suspending */
    abstract suspend fun gameLoop(isOver: CompletableDeferred<Unit>)

    /* What to do once the time of 60s per action is expired */
    abstract suspend fun timeIsUpAction()

    /* Recalculate the points of all players. Done at the end of every game loop tick, to update pointsMap */
    abstract fun recalculatePlayerPoints(): MutableMap<Player, Int>

    /* Ran at the end of every game loop tick, it checks if someone won abruptly the game and ends it if true */
    abstract fun winCheck(): Boolean

    /* Return the winner, ran at game end */
    abstract fun calculateWinner(): Player?
    abstract fun winMessage(winner: Player): String

    // Checks if the clicker is the player who is currently playing the turn
    suspend fun ButtonInteractionCreateEvent.turnCheck(): Boolean {
        if (currentPlayer.id != this.interaction.user.id) {
            interaction.respondEphemeral { content = "Non è il tuo turno!" }
            return false
        } else return true
    }

    suspend fun ButtonInteractionCreateEvent.statusCheck(status: WaitingStatus): Boolean {
        if (waitingAction == status) return true else {
            interaction.respondEphemeral { content = "Non puoi fare quest'azione! $status" }
            return false
        }
    }
}

class GameOverException : Exception()