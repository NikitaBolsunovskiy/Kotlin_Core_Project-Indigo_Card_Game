package indigo

import kotlin.random.Random

fun main() {
    val game = Game()
    game.play()
}

class Game {

    private val deck: Deck = Deck()
    private var playerIsFirst = false
    private val cardsOnTable = mutableListOf<Card>()
    private val playerCards = mutableListOf<Card>()
    private val computerCards = mutableListOf<Card>()
    private var playerRequestedExit = false
    private val wonByPlayer = mutableListOf<Card>()
    private val wonByComputer = mutableListOf<Card>()
    private var lastWinnerIsPlayer = false
    private var lastWinnerIsComputer = false

    init {
        println("Indigo Card Game")
        requestWhoPlaysFirst()
        cardsOnTable += deck.draw(4)
        println("Initial cards on the table: ${cardsOnTable.joinToString (" ")}\n")
        playerCards += deck.draw(6)
        computerCards += deck.draw(6)
    }

    private fun score(endgame: Boolean = false): Pair<Int, Int> {

        var playerScore = 0
        var computerScore = 0

        if (endgame) {
            when {
                wonByPlayer.size > wonByComputer.size -> playerScore += 3
                wonByPlayer.size < wonByComputer.size -> computerScore += 3
                else -> {
                    playerScore += if (playerIsFirst) 3 else 0
                    computerScore += if (!playerIsFirst) 3 else 0
                }
            }
        }

        wonByPlayer.forEach {
            playerScore += it.score()
        }

        wonByComputer.forEach {
            computerScore += it.score()
        }

        return Pair(playerScore, computerScore)

    }

    fun play() {

        var playersMove = playerIsFirst

        while (true) {

            if (cardsOnTable.size == 0) println("No cards on the table") else
                println("${cardsOnTable.size} cards on the table, and the top card is ${cardsOnTable.last()}")

            if (deck.isEmpty() && computerCards.isEmpty() && playerCards.isEmpty()) break

            when (playersMove) {
                true -> {
                    playersMove()
                    if (playerRequestedExit) break
                    println()
                }
                else -> {
                    computersMove()
                    println()
                }
            }

            playersMove = !playersMove

        }

        if (!playerRequestedExit) {
            if (cardsOnTable.isNotEmpty()) {
                when {
                    lastWinnerIsPlayer -> {
                        wonByPlayer += cardsOnTable
                    }

                    lastWinnerIsComputer -> {
                        wonByComputer += cardsOnTable
                    }

                    playerIsFirst -> {
                        wonByPlayer += cardsOnTable
                    }

                    else -> {
                        wonByComputer += cardsOnTable
                    }
                }
            }
            showScore(true)
        }
        println("Game Over")

    }

    private fun playersMove() {
        println("Cards in hand: ${playerCards.joinToString(" ") { "${playerCards.lastIndexOf(it) + 1})$it" }}")
        while (true) {
            println("Choose a card to play (1-${playerCards.size}):")
            when (val input = readln()) {
                "exit" -> {
                    playerRequestedExit = true
                    break
                }
                else -> {
                    try {
                        val cardNumber = input.toInt()
                        if (cardNumber in 1..playerCards.size) {
                            val moveCard = playerCards.removeAt(cardNumber - 1)
                            if (cardsOnTable.isNotEmpty() && moveCard.wins(cardsOnTable.last())) {
                                println("Player wins cards")
                                wonByPlayer += cardsOnTable
                                wonByPlayer += moveCard
                                cardsOnTable.clear()
                                lastWinnerIsPlayer = true
                                lastWinnerIsComputer = false
                                showScore()
                            } else {
                                cardsOnTable += moveCard
                            }
                            if (playerCards.size == 0) {
                                try {
                                    playerCards += deck.draw(6)
                                } catch (_: NotEnoughCardsException) {

                                }
                            }
                            break
                        }
                    } catch (_: NumberFormatException) {

                    }
                }
            }
        }
    }

    private fun computersMove() {
        println(computerCards.joinToString(" "))

        val moveCard: Card
        when {
            computerCards.size == 1 -> moveCard = computerCards.removeFirst()
            else -> {

                val candidates = try {
                    computerCards.filter { it.wins(cardsOnTable.last()) }
                } catch (_: NoSuchElementException) {
                    listOf()
                }

                when {
                    candidates.size == 1 -> moveCard = computerCards.removeAt(computerCards.indexOf(candidates[0]))
                    else -> {
                        when {
                            cardsOnTable.isEmpty() || candidates.isEmpty() -> {
                                moveCard = when {
                                    hasCardsWithSameSuit(computerCards) ->
                                        popRandomCardWithSameSuit(computerCards)

                                    hasCardsWithSameRank(computerCards) ->
                                        popRandomCardWithSameRank(computerCards)

                                    else ->
                                        popRandomCard(computerCards)
                                }
                            }
                            else -> {
                                moveCard = when {
                                    hasCardsWithSameSuit(candidates.toMutableList()) -> {
                                        val chosenCandidate = popRandomCardWithSameSuit(candidates.toMutableList())
                                        computerCards.removeAt(computerCards.indexOf(chosenCandidate))
                                    }
                                    hasCardsWithSameRank(candidates.toMutableList()) -> {
                                        val chosenCandidate = popRandomCardWithSameRank(candidates.toMutableList())
                                        computerCards.removeAt(computerCards.indexOf(chosenCandidate))
                                    }
                                    else -> {
                                        val chosenCandidate = popRandomCard(candidates.toMutableList())
                                        computerCards.removeAt(computerCards.indexOf(chosenCandidate))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        println("Computer plays $moveCard")

        if (cardsOnTable.isNotEmpty() && moveCard.wins(cardsOnTable.last())) {
            println("Computer wins cards")
            wonByComputer += cardsOnTable
            wonByComputer += moveCard
            cardsOnTable.clear()
            lastWinnerIsPlayer = false
            lastWinnerIsComputer = true
            showScore()
        } else {
            cardsOnTable += moveCard
        }

        if (computerCards.size == 0) {
            try {
                computerCards += deck.draw(6)
            } catch (_: NotEnoughCardsException) {

            }
        }
    }

    private fun popRandomCard(cards: MutableList<Card>): Card {
        val randomCard = cards.random()
        cards.removeAt(cards.indexOf(randomCard))
        return randomCard
    }

    private fun hasCardsWithSameRank(cards: MutableList<Card>): Boolean {
        val grouped = cards.groupBy { it.rank }
        return grouped.filter { it.value.size > 1 }.isNotEmpty()
    }

    private fun popRandomCardWithSameRank(cards: MutableList<Card>): Card {
        val grouped = cards.groupBy { it.rank }
        val filtered = grouped.filter { it.value.size > 1 }
        val randomRank = filtered.keys.random()
        val randomCard = filtered.getValue(randomRank).random()
        cards.removeAt(cards.indexOf(randomCard))
        return randomCard
    }

    private fun hasCardsWithSameSuit(cards: MutableList<Card>): Boolean {
        val grouped = cards.groupBy { it.suit }
        return grouped.filter { it.value.size > 1 }.isNotEmpty()
    }

    private fun popRandomCardWithSameSuit(cards: MutableList<Card>): Card {
        val grouped = cards.groupBy { it.suit }
        val filtered = grouped.filter { it.value.size > 1 }
        val randomSuit = filtered.keys.random()
        val randomCard = filtered.getValue(randomSuit).random()
        cards.removeAt(cards.indexOf(randomCard))
        return randomCard
    }

    private fun requestWhoPlaysFirst() {
        while (true) {
            println("Play first?")
            val input = readln()
            when (input) {
                "yes" -> {
                    playerIsFirst = true
                    break
                }
                "no" -> {
                    playerIsFirst = false
                    break
                }
            }
        }
    }

    private fun showScore(endgame: Boolean = false) {
        val (playerScore, computerScore) = score(endgame)
        println("""
            Score: Player $playerScore - Computer $computerScore
            Cards: Player ${wonByPlayer.size} - Computer ${wonByComputer.size}
        """.trimIndent())
    }

}

class NotEnoughCardsException: Exception("The remaining cards are insufficient to meet the request.")

class Deck {

    private val cards = mutableListOf<Card>()
    private val randomizer = Random

    init {
        reset()
    }

    private fun reset() {

        cards.clear()
        Ranks.values().forEach { rank ->
            Suits.values().forEach { suit ->
                cards.add(Card(rank, suit))
            }
        }
        cards.shuffle(randomizer)
    }

    fun draw(numberOfCards: Int): MutableList<Card> {
        if (numberOfCards > cards.size) {
            throw NotEnoughCardsException()
        }
        val result = mutableListOf<Card>()
        for (i in 1..numberOfCards) {

            result.add(cards.removeAt(0))
        }
        return result
    }

    fun isEmpty(): Boolean = cards.isEmpty()

}

class Card (val rank: Ranks, val suit: Suits) {
    override fun toString(): String {
        return "${rank.description}${suit.description}"
    }

    fun score(): Int {
        return this.rank.score
    }

    fun wins(other: Card):Boolean = this.rank == other.rank || this.suit == other.suit
}

enum class Ranks(val description: String, val score: Int) {
    TWO("2", 0),
    THREE("3", 0),
    FOUR("4", 0),
    FIVE("5", 0),
    SIX("6", 0),
    SEVEN("7", 0),
    EIGHT("8", 0),
    NINE("9", 0),
    TEN("10", 1),
    JACK("J", 1),
    QUEEN("Q", 1),
    KING("K", 1),
    ACE("A", 1),
}

enum class Suits(val description: String) {
    SPADES("♠"), HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣"),
}
