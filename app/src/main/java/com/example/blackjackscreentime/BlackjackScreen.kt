package com.example.blackjackscreentime

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

// change this before sharing the repo publicly if you care about it
// staying secret -- long-pressing the wallet text triggers this
private const val DEBUG_RESET_CODE = "ducky"

/**
 * Rough proof-of-concept screen -- bet, deal, hit/stand, see the
 * wallet move. Not styled, just here to confirm the JNI plumbing
 * actually works end to end before building the real UI.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlackjackScreen() {
    val context = LocalContext.current

    // remember{} so the engine survives recomposition but gets torn
    // down (freeing native memory) when this leaves composition.
    // checkAndApplyNaturalReset also tops the wallet back up to 10 if
    // the 2-hour timer already expired while the app was closed.
    val engine = remember { BlackjackEngine(startingMinutes = WalletStore.checkAndApplyNaturalReset(context)) }
    DisposableEffect(Unit) {
        onDispose { engine.close() }
    }

    var wallet by remember { mutableIntStateOf(engine.getWallet()) }
    var betText by remember { mutableStateOf("") }
    var playerHand by remember { mutableStateOf("") }
    var dealerHand by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Place a bet to start.") }
    var state by remember { mutableStateOf(engine.getState()) }
    var millisUntilReset by remember { mutableLongStateOf(WalletStore.millisUntilNextReset(context)) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetCodeInput by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf(false) }

    fun refresh(revealDealer: Boolean) {
        wallet = engine.getWallet()
        playerHand = engine.getPlayerHand()
        dealerHand = engine.getDealerHand(revealDealer)
        state = engine.getState()
        WalletStore.saveWallet(context, wallet) // keep the saved balance in sync
    }

    // checks once a minute whether the 2-hour allowance timer is up.
    // if it is, tops the wallet back to 10 and restarts the timer,
    // interrupting whatever round was in progress -- same as the
    // debug reset does.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            millisUntilReset = WalletStore.millisUntilNextReset(context)
            if (millisUntilReset <= 0) {
                engine.debugResetWallet(10) // reuses the force-set + newRound helper
                betText = ""
                message = "Your 2-hour allowance topped back up to 10 min."
                refresh(revealDealer = false)
                millisUntilReset = WalletStore.millisUntilNextReset(context)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Instagram time: $wallet min",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.combinedClickable(
                onClick = {}, // normal taps do nothing, this isn't a button
                onLongClick = {
                    resetCodeInput = ""
                    resetError = false
                    showResetDialog = true
                }
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatCountdown(millisUntilReset),
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(16.dp))
        Text("Dealer: $dealerHand")
        Text("You: $playerHand")
        Spacer(Modifier.height(8.dp))
        Text(message)
        Spacer(Modifier.height(16.dp))

        if (wallet <= 0) {
            Text("Out of time. Wait for your allowance to refill, or go touch grass.")
        } else {
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

    if (showResetDialog) {
        Dialog(onDismissRequest = { showResetDialog = false }) {
            Surface(shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Debug reset", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetCodeInput,
                        onValueChange = {
                            resetCodeInput = it
                            resetError = false
                        },
                        label = { Text("Passcode") },
                        isError = resetError
                    )
                    if (resetError) {
                        Text("Wrong code.", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            if (resetCodeInput == DEBUG_RESET_CODE) {
                                engine.debugResetWallet(10)
                                WalletStore.markReset(context, 10) // also restarts the 2hr timer
                                millisUntilReset = WalletStore.millisUntilNextReset(context)
                                betText = ""
                                message = "Place a bet to start."
                                refresh(revealDealer = false)
                                showResetDialog = false
                            } else {
                                resetError = true
                            }
                        }) {
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }
}

// e.g. "Resets in 1h 42m" -- clamps negative values to 0 just in case
// the LaunchedEffect hasn't caught up yet
private fun formatCountdown(millis: Long): String {
    val clamped = millis.coerceAtLeast(0L)
    val totalMinutes = clamped / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "Resets in ${hours}h ${minutes}m"
}
