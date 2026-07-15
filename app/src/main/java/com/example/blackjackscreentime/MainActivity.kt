package com.example.blackjackscreentime

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // BUG FIX: this was plain MaterialTheme{} before, which
            // meant the Balatro palette/pixel font from Theme.kt were
            // never actually applied at the top level -- everything
            // was falling back to Compose Material3 defaults unless a
            // screen set colors/fonts explicitly at the call site.
            BettaTimeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

/**
 * Root content: shows a one-time-visible onboarding banner if the
 * accessibility service isn't enabled yet, then the game screen either
 * way. The banner disappears automatically once the service is on --
 * checked on first composition and again every time the app resumes
 * (e.g. coming back from the Settings screen after enabling it).
 */
@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var accessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, ScreenTimeAccessibilityService::class.java))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled =
                    isAccessibilityServiceEnabled(context, ScreenTimeAccessibilityService::class.java)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!accessibilityEnabled) {
            AccessibilityOnboardingBanner()
        }
        BlackjackScreen()
    }
}

@Composable
private fun AccessibilityOnboardingBanner() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BalatroRed)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Blocking is off. Enable BettaTime's Accessibility Service to actually lock Instagram.",
                style = MaterialTheme.typography.bodyLarge,
                color = InkWhite
            )
            Spacer(Modifier.height(10.dp))
            ChunkyButton(
                text = "Open settings",
                color = BalatroGold,
                shadowColor = BalatroGoldShadow,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }
    }
}

// standard robust check -- string-matches the exact registered
// component rather than doing a loose substring search, which avoids
// false positives from similarly-named services
private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
    val expected = ComponentName(context, serviceClass)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServicesSetting)
    while (splitter.hasNext()) {
        val componentName = ComponentName.unflattenFromString(splitter.next())
        if (componentName == expected) return true
    }
    return false
}
