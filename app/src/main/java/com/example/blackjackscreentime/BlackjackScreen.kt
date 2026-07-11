package com.example.blackjackscreentime

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

// change this before sharing the repo publicly if you care about it
// staying secret -- long-pressing the wallet badge triggers this
private const val DEBUG_RESET_CODE = "ducky"

/**
 * Main game screen: bet, deal, hit/stand, watch the wallet move.
 * Balatro-inspired look -- near-black background, a gold money tag
 * for the wallet, chunky drop-shadow buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlackjackScreen(onWalletBecamePositive: (() -> Unit)? = null) {
    val context = LocalContext.current

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
        WalletStore.saveWallet(context, wallet)
        if (wallet > 0) onWalletBecamePositive?.invoke()
    }

    // checks once a minute whether the 2-hour allowance timer is up.
    // BUG FIX: this branch used to only call engine.debugResetWallet(),
    // which resets the C++ wallet but never touched WalletStore's
    // saved timestamp -- so the timer looked "stuck" until the app was
    // restarted (checkAndApplyNaturalReset on launch was the only path
    // that correctly called markReset()). Now this branch calls
    // markReset() too, so the 2-hour countdown actually restarts here.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            millisUntilReset = WalletStore.millisUntilNextReset(context)
            if (millisUntilReset <= 0) {
                engine.debugResetWallet(10)
                WalletStore.markReset(context, 10)
                betText = ""
                message = "Your 2-hour allowance topped back up to 10 min."
                refresh(revealDealer = false)
                millisUntilReset = WalletStore.millisUntilNextReset(context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TableBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // wallet "money tag" -- chunky rounded gold badge, long-press
        // is the hidden debug reset trigger
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BalatroGoldShadow)
                .padding(bottom = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(BalatroGold)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            resetCodeInput = ""
                            resetError = false
                            showResetDialog = true
                        }
                    )
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$wallet", style = WalletNumberStyle)
                    Text(
                        text = "MIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = TableBg
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = formatCountdown(millisUntilReset),
            fontFamily = FontFamily.Default,
            fontSize = 12.sp,
            color = InkMuted
        )

        Spacer(Modifier.height(24.dp))

        // the table -- a dark panel holding both hands, thin gold rule
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBg)
                .border(2.dp, BalatroGold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "DEALER",
                fontFamily = FontFamily.Default,
                fontSize = 12.sp,
                color = InkMuted
            )
            Text(
                text = dealerHand.ifBlank { "--" },
                style = MaterialTheme.typography.bodyLarge,
                color = InkWhite
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "YOU",
                fontFamily = FontFamily.Default,
                fontSize = 12.sp,
                color = InkMuted
            )
            Text(
                text = playerHand.ifBlank { "--" },
                style = MaterialTheme.typography.bodyLarge,
                color = InkWhite
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = InkWhite
        )
        Spacer(Modifier.height(20.dp))

        if (wallet <= 0) {
            Text(
                text = "Out of time. Wait for your allowance to refill, or go touch grass.",
                style = MaterialTheme.typography.bodyLarge,
                color = BalatroRed
            )
        } else {
            when (state) {
                GameState.BETTING -> {
                    OutlinedTextField(
                        value = betText,
                        onValueChange = { betText = it.filter(Char::isDigit) },
                        label = { Text("Bet (minutes)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BalatroGold,
                            unfocusedBorderColor = InkMuted,
                            focusedTextColor = InkWhite,
                            unfocusedTextColor = InkWhite,
                            focusedLabelColor = BalatroGold,
                            cursorColor = BalatroGold
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    ChunkyButton(
                        text = "Deal",
                        color = BalatroGold,
                        shadowColor = BalatroGoldShadow,
                        onClick = {
                            val bet = betText.toIntOrNull()
                            if (bet == null) {
                                message = "Enter a bet first."
                                return@ChunkyButton
                            }
                            if (!engine.placeBet(bet)) {
                                message = "Invalid bet -- must be 1 to $wallet."
                                return@ChunkyButton
                            }
                            val outcome = engine.dealInitial()
                            message = outcome.description
                            refresh(revealDealer = engine.getState() != GameState.PLAYER_TURN)
                        }
                    )
                }

                GameState.PLAYER_TURN -> {
                    Row {
                        ChunkyButton(
                            text = "Hit",
                            color = BalatroGold,
                            shadowColor = BalatroGoldShadow,
                            onClick = {
                                val outcome = engine.hit()
                                message = outcome.description
                                refresh(revealDealer = engine.getState() != GameState.PLAYER_TURN)
                            }
                        )
                        Spacer(Modifier.width(14.dp))
                        ChunkyButton(
                            text = "Stand",
                            color = BalatroBlue,
                            shadowColor = BalatroBlueShadow,
                            onClick = {
                                val outcome = engine.stand()
                                message = outcome.description
                                refresh(revealDealer = true)
                            }
                        )
                    }
                }

                GameState.ROUND_OVER -> {
                    ChunkyButton(
                        text = "Next round",
                        color = BalatroGold,
                        shadowColor = BalatroGoldShadow,
                        onClick = {
                            engine.newRound()
                            betText = ""
                            message = "Place a bet to start."
                            refresh(revealDealer = false)
                        }
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        Dialog(onDismissRequest = { showResetDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = PanelBg
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Debug reset",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkWhite
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetCodeInput,
                        onValueChange = {
                            resetCodeInput = it
                            resetError = false
                        },
                        label = { Text("Passcode") },
                        isError = resetError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BalatroGold,
                            unfocusedBorderColor = InkMuted,
                            focusedTextColor = InkWhite,
                            unfocusedTextColor = InkWhite
                        )
                    )
                    if (resetError) {
                        Text("Wrong code.", color = BalatroRed)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Cancel", color = InkMuted)
                        }
                        Spacer(Modifier.width(8.dp))
                        ChunkyButton(
                            text = "Reset",
                            color = BalatroGold,
                            shadowColor = BalatroGoldShadow,
                            onClick = {
                                if (resetCodeInput == DEBUG_RESET_CODE) {
                                    engine.debugResetWallet(10)
                                    WalletStore.markReset(context, 10)
                                    millisUntilReset = WalletStore.millisUntilNextReset(context)
                                    betText = ""
                                    message = "Place a bet to start."
                                    refresh(revealDealer = false)
                                    showResetDialog = false
                                } else {
                                    resetError = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatCountdown(millis: Long): String {
    val clamped = millis.coerceAtLeast(0L)
    val totalMinutes = clamped / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "Resets in ${hours}h ${minutes}m"
}
