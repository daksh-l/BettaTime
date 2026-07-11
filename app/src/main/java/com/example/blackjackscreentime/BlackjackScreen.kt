package com.example.blackjackscreentime

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Rough proof-of-concept screen -- bet, deal, hit/stand, see the
 * wallet move. Not styled, just here to confirm the JNI plumbing
 * actually works end to end before building the real UI.
 */
@Composable
fun BlackjackScreen() {
    // remember{} so the engine survives recomposition but gets torn
    // down (freeing native memory) when this leaves composition
    val engine = remember { BlackjackEngine(startingMinutes = 10) }
    DisposableEffect(Unit) {
        onDispose { engine.close() }
    }

    var wallet by remember { mutableIntStateOf(engine.getWallet()) }
    var betText by remember { mutableStateOf("") }
    var playerHand by remember { mutableStateOf("") }
    var dealerHand by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Place a bet to start.") }
    var state by remember { mutableStateOf(engine.getState()) }

    fun refresh(revealDealer: Boolean) {
        wallet = engine.getWallet()
        playerHand = engine.getPlayerHand()
        dealerHand = engine.getDealerHand(revealDealer)
        state = engine.getState()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Instagram time: $wallet min", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Dealer: $dealerHand")
        Text("You: $playerHand")
        Spacer(Modifier.height(8.dp))
        Text(message)
        Spacer(Modifier.height(16.dp))

        when (state) {
            GameState.BETTING -> {
                OutlinedTextField(
                    value = betText,
                    onValueChange = { betText = it.filter(Char::isDigit) },
                    label = { Text("Bet (minutes)") }
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val bet = betText.toIntOrNull() ?: return@Button
                    if (!engine.placeBet(bet)) {
                        message = "Invalid bet -- must be 1 to $wallet."
                        return@Button
                    }
                    val outcome = engine.dealInitial()
                    message = outcome.description
                    refresh(revealDealer = engine.getState() != GameState.PLAYER_TURN)
                }) {
                    Text("Deal")
                }
            }

            GameState.PLAYER_TURN -> {
                Row {
                    Button(onClick = {
                        val outcome = engine.hit()
                        message = outcome.description
                        refresh(revealDealer = engine.getState() != GameState.PLAYER_TURN)
                    }) { Text("Hit") }

                    Spacer(Modifier.width(12.dp))

                    Button(onClick = {
                        val outcome = engine.stand()
                        message = outcome.description
                        refresh(revealDealer = true)
                    }) { Text("Stand") }
                }
            }

            GameState.ROUND_OVER -> {
                if (engine.isBrokeOut()) {
                    Text("Out of time. Go touch grass.")
                } else {
                    Button(onClick = {
                        engine.newRound()
                        betText = ""
                        message = "Place a bet to start."
                        refresh(revealDealer = false)
                    }) {
                        Text("Next round")
                    }
                }
            }
        }
    }
}
