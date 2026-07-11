package com.example.blackjackscreentime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// the real Balatro font, dropped in at res/font/m6x11.ttf. It's a
// single-weight bitmap font -- don't ask for Bold/Black on it, Compose
// will just fake-bold it and it looks bad. Use FontWeight.Normal
// everywhere this family is used.
val PixelFont = FontFamily(Font(R.font.m6x11))

// Balatro-ish palette: near-black background, saturated primaries,
// chunky and bright rather than subtle
val TableBg = Color(0xFF161A20)
val PanelBg = Color(0xFF20242C)
val BalatroGold = Color(0xFFFFD400)
val BalatroGoldShadow = Color(0xFFB8930A)
val BalatroRed = Color(0xFFFE5F55)
val BalatroRedShadow = Color(0xFFB23029)
val BalatroBlue = Color(0xFF4F9DDE)
val BalatroBlueShadow = Color(0xFF2F6C99)
val InkWhite = Color(0xFFF5F5F5)
val InkMuted = Color(0xFFA0A6B4)

private val BettaTimeColorScheme = darkColorScheme(
    primary = BalatroGold,
    onPrimary = TableBg,
    secondary = BalatroBlue,
    background = TableBg,
    onBackground = InkWhite,
    surface = PanelBg,
    onSurface = InkWhite,
    error = BalatroRed,
    onError = InkWhite
)

// m6x11 pixel font throughout, matching Balatro's actual UI. Sizes
// bumped up from a normal sans since bitmap fonts read smaller than
// their point size suggests -- adjust to taste once you see it on
// device.
private val BettaTimeTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        letterSpacing = 0.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = InkMuted,
        letterSpacing = 1.sp
    )
)

val WalletNumberStyle = TextStyle(
    fontFamily = PixelFont,
    fontWeight = FontWeight.Normal,
    fontSize = 40.sp,
    color = TableBg
)

@Composable
fun BettaTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BettaTimeColorScheme,
        typography = BettaTimeTypography,
        content = content
    )
}

/**
 * Chunky drop-shadow button, Balatro's signature UI element: a solid
 * color block sitting on top of a darker "shelf" of the same hue,
 * offset down so it reads as a pressable 3D block rather than a flat
 * Material button.
 */
@Composable
fun ChunkyButton(
    text: String,
    color: Color,
    shadowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // the shelf, offset down -- gives the button its thickness.
        // holds the same text, colored to match its own background,
        // so it's invisible but forces identical sizing to the face
        // box above it (an empty Box here would collapse to 0 size)
        Box(
            modifier = Modifier
                .offset(y = 5.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(shadowColor)
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = shadowColor
            )
        }
        // the actual clickable face
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 22.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = TableBg
            )
        }
    }
}
