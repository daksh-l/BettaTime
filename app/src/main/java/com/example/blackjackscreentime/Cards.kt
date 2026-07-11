package com.example.blackjackscreentime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

private val CardWidth = 56.dp
private val CardHeight = 80.dp
private val FanOverlap = 34.dp // how much of each card behind the top one still peeks out

/** One parsed card: rank as shown ("A", "10", "K", ...) plus a suit letter (H/D/S/C). */
data class UiCard(val rank: String, val suit: Char) {
    val isRed: Boolean get() = suit == 'H' || suit == 'D'

    val suitSymbol: String get() = when (suit) {
        'H' -> "\u2665" // hearts
        'D' -> "\u2666" // diamonds
        'S' -> "\u2660" // spades
        'C' -> "\u2663" // clubs
        else -> "?"
    }
}

/**
 * Parses the engine's hand string ("KH 7D 10S") into structured cards.
 * The engine's Card::toString() always puts the suit as the last
 * character, so this just splits on spaces and peels that off --
 * no engine/JNI changes needed for this.
 */
fun parseHand(hand: String): List<UiCard> {
    if (hand.isBlank()) return emptyList()
    return hand.trim().split(" ").mapNotNull { token ->
        if (token.length < 2) return@mapNotNull null
        val suit = token.last()
        val rank = token.dropLast(1)
        UiCard(rank, suit)
    }
}

/**
 * A single card face. Pass faceDown = true to render a card back
 * instead (used for the dealer's hidden hole card).
 */
@Composable
fun PlayingCard(card: UiCard?, faceDown: Boolean = false, modifier: Modifier = Modifier) {
    if (faceDown || card == null) {
        Box(
            modifier = modifier
                .size(CardWidth, CardHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(BalatroBlue)
                .border(2.dp, BalatroGold, RoundedCornerShape(8.dp))
        )
        return
    }

    val suitColor: Color = if (card.isRed) BalatroRed else TableBg

    Box(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(InkWhite)
            .border(2.dp, TableBg, RoundedCornerShape(8.dp))
    ) {
        // rank in the top-left corner
        Text(
            text = card.rank,
            style = MaterialTheme.typography.labelSmall,
            color = suitColor,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        )
        // big suit symbol centered
        Text(
            text = card.suitSymbol,
            style = MaterialTheme.typography.headlineSmall,
            color = suitColor,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * A hand laid out as a fanned, overlapping row -- like cards dealt
 * onto a table rather than a plain list. Pass hideIndex to render
 * that one position face-down (used for the dealer's hole card while
 * the player is still deciding hit/stand).
 */
@Composable
fun HandRow(cards: List<UiCard>, hideIndex: Int? = null, modifier: Modifier = Modifier) {
    if (cards.isEmpty()) return

    val totalWidth = CardWidth + FanOverlap * (cards.size - 1).coerceAtLeast(0)

    Box(modifier = modifier.size(width = totalWidth, height = CardHeight)) {
        cards.forEachIndexed { index, card ->
            PlayingCard(
                card = card,
                faceDown = index == hideIndex,
                modifier = Modifier
                    .zIndex(index.toFloat())
                    .align(Alignment.CenterStart)
                    .offset(x = FanOverlap * index)
            )
        }
    }
}
