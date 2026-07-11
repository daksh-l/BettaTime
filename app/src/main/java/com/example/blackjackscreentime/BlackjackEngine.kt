package com.example.blackjackscreentime

// mirrors RoundResult in blackjack_game.h -- order matters, has to
// match the enum's declaration order exactly
enum class RoundResult {
    NONE, PLAYER_BLACKJACK, PLAYER_WIN, PLAYER_BUST, DEALER_BUST, DEALER_WIN, PUSH
}

// mirrors GameState in blackjack_game.h, same deal on ordering
enum class GameState {
    BETTING, PLAYER_TURN, ROUND_OVER
}

data class RoundOutcome(
    val result: RoundResult,
    val walletDelta: Int,
    val walletAfter: Int,
    val description: String
)

/**
 * Thin Kotlin wrapper around the C++ blackjack engine. One instance of
 * this = one live game with its own wallet, deck, and hands.
 *
 * Call close() when you're done with it (e.g. onCleared() in a
 * ViewModel) so the native side actually frees the BlackjackGame.
 */
class BlackjackEngine(startingMinutes: Int) : AutoCloseable {

    private var handle: Long = nativeCreate(startingMinutes)

    fun placeBet(minutes: Int): Boolean = nativePlaceBet(handle, minutes)

    fun dealInitial(): RoundOutcome = parseOutcome(nativeDealInitial(handle))

    fun hit(): RoundOutcome = parseOutcome(nativeHit(handle))

    fun stand(): RoundOutcome = parseOutcome(nativeStand(handle))

    fun newRound() = nativeNewRound(handle)

    fun getWallet(): Int = nativeGetWallet(handle)

    fun getState(): GameState = GameState.entries[nativeGetState(handle)]

    fun isBrokeOut(): Boolean = nativeIsBrokeOut(handle)

    fun getPlayerHand(): String = nativeGetPlayerHand(handle)

    // pass revealAll = true once the round is over, false while the
    // player is still deciding hit/stand
    fun getDealerHand(revealAll: Boolean): String = nativeGetDealerHand(handle, revealAll)

    // testing only -- forces the wallet to `minutes` (default 10) and
    // resets to a clean betting state. Only reachable via the hidden
    // passcode dialog, not part of normal gameplay.
    fun debugResetWallet(minutes: Int = 10) {
        nativeDebugSetWallet(handle, minutes)
        nativeNewRound(handle)
    }

    override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    // native side packs outcomes as "resultCode|delta|walletAfter|description"
    private fun parseOutcome(packed: String): RoundOutcome {
        val parts = packed.split("|", limit = 4)
        val resultCode = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val delta = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val walletAfter = parts.getOrNull(2)?.toIntOrNull() ?: getWallet()
        val description = parts.getOrNull(3) ?: ""
        return RoundOutcome(RoundResult.entries[resultCode], delta, walletAfter, description)
    }

    private external fun nativeCreate(startingMinutes: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativePlaceBet(handle: Long, minutes: Int): Boolean
    private external fun nativeDealInitial(handle: Long): String
    private external fun nativeHit(handle: Long): String
    private external fun nativeStand(handle: Long): String
    private external fun nativeNewRound(handle: Long)
    private external fun nativeGetWallet(handle: Long): Int
    private external fun nativeGetState(handle: Long): Int
    private external fun nativeIsBrokeOut(handle: Long): Boolean
    private external fun nativeGetPlayerHand(handle: Long): String
    private external fun nativeGetDealerHand(handle: Long, revealAll: Boolean): String
    private external fun nativeDebugSetWallet(handle: Long, minutes: Int)

    companion object {
        init {
            System.loadLibrary("blackjack")
        }
    }
}
