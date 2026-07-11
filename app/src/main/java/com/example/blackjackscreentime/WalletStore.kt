package com.example.blackjackscreentime

import android.content.Context

// Wraps a single SharedPreferences file for the wallet balance plus
// the natural-reset timer. Nothing fancy -- a few keys, survives app
// restarts.
object WalletStore {
    private const val PREFS_NAME = "bettatime_prefs"
    private const val KEY_WALLET_MINUTES = "wallet_minutes"
    private const val KEY_LAST_RESET_TIME = "last_reset_time"
    private const val DEFAULT_STARTING_MINUTES = 10

    // wallet tops back up to the default every 2 hours, regardless of
    // what the balance was
    const val RESET_INTERVAL_MILLIS: Long = 2 * 60 * 60 * 1000L

    // Returns the saved balance, or the default starting allowance if
    // this is the first time the app has ever been opened. Does not
    // check the reset timer -- see checkAndApplyNaturalReset() for that.
    fun getWallet(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WALLET_MINUTES, DEFAULT_STARTING_MINUTES)
    }

    // Just persists the current balance, doesn't touch the reset timer.
    // Call this after every hand so the balance survives a restart.
    fun saveWallet(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_WALLET_MINUTES, minutes).apply()
    }

    // Timestamp (millis) of the last reset. Defaults to "now" on first
    // ever read so a fresh install doesn't immediately look overdue.
    private fun getLastResetTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAST_RESET_TIME)) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_RESET_TIME, now).apply()
            return now
        }
        return prefs.getLong(KEY_LAST_RESET_TIME, System.currentTimeMillis())
    }

    // Forces the wallet to `minutes` and restarts the 2-hour timer from
    // now. Used both by the natural reset and the hidden debug reset.
    fun markReset(context: Context, minutes: Int = DEFAULT_STARTING_MINUTES) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_WALLET_MINUTES, minutes)
            .putLong(KEY_LAST_RESET_TIME, System.currentTimeMillis())
            .apply()
    }

    // How much time is left before the next natural reset, in millis.
    // Zero or negative means a reset is due right now.
    fun millisUntilNextReset(context: Context): Long {
        val elapsed = System.currentTimeMillis() - getLastResetTime(context)
        return RESET_INTERVAL_MILLIS - elapsed
    }

    // Call this on launch and periodically while the app is open. If
    // 2 hours have passed, tops the wallet back up to 10 and returns
    // that new value. Otherwise just returns the current saved balance.
    fun checkAndApplyNaturalReset(context: Context): Int {
        return if (millisUntilNextReset(context) <= 0) {
            markReset(context, DEFAULT_STARTING_MINUTES)
            DEFAULT_STARTING_MINUTES
        } else {
            getWallet(context)
        }
    }
}
