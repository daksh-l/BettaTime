package com.example.blackjackscreentime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class BlockingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // while blocked, back should never reveal Instagram underneath --
        // send the user to the home screen instead. Once the wallet goes
        // positive we finish() this activity ourselves (see below), so
        // back becomes irrelevant at that point anyway.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        })

        setContent {
            BettaTimeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlockingScreen(onUnlocked = { finish() })
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // BUG FIX: onDestroy() alone isn't enough here. If the user
        // leaves this screen via the home button/gesture or recents
        // instead of winning a hand, Android just backgrounds the
        // activity -- onDestroy() may not fire for a long time (or at
        // all before the system kills it), leaving blockingActive
        // stuck true forever and silently preventing any future block.
        // onStop() fires reliably whenever this screen actually leaves
        // the foreground, for any reason, so reset there instead.
        ScreenTimeAccessibilityService.onBlockingActivityFinished()
    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenTimeAccessibilityService.onBlockingActivityFinished()
    }
}

@Composable
private fun BlockingScreen(onUnlocked: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(TableBg)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BalatroRed)
                .padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Instagram is locked -- win minutes to continue",
                style = MaterialTheme.typography.labelSmall,
                color = InkWhite
            )
        }
        // reuses the exact same game screen -- onWalletBecamePositive
        // fires the moment a hand resolves with wallet > 0, which
        // dismisses this activity and drops the user back into whatever
        // was running underneath (Instagram)
        BlackjackScreen(onWalletBecamePositive = onUnlocked)
    }
}
