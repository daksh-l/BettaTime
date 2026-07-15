package com.example.blackjackscreentime

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class ScreenTimeAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg != INSTAGRAM_PACKAGE) return

        // overlay already showing -- don't stack duplicate launches
        // every time Instagram fires another window-state event
        if (blockingActive) return

        // reuses the same natural-reset check the game screen uses, so
        // the 2-hour allowance timer is honored consistently whether
        // you're inside the app or just launching Instagram
        val wallet = WalletStore.checkAndApplyNaturalReset(applicationContext)
        if (wallet <= 0) {
            blockingActive = true
            val intent = Intent(this, BlockingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // required override -- nothing to clean up
    }

    companion object {
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"

        @Volatile
        private var blockingActive = false

        // BlockingActivity calls this from onDestroy() so the next
        // Instagram launch can trigger a fresh block if still out of time
        fun onBlockingActivityFinished() {
            blockingActive = false
        }
    }
}
