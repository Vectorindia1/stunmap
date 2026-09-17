package com.stunmap.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stunmap.ui.theme.ActiveGreen
import com.stunmap.ui.theme.BackgroundDark
import com.stunmap.ui.theme.JetBrainsMonoFamily
import com.stunmap.ui.theme.MetaRed
import com.stunmap.ui.theme.PrimaryAccent
import com.stunmap.ui.theme.SurfaceDark

@Composable
fun CaptureButton(
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCapturing) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val borderColor = if (isCapturing) ActiveGreen else PrimaryAccent
    val bgColor = if (isCapturing) MetaRed.copy(alpha = 0.15f) else PrimaryAccent.copy(alpha = 0.1f)
    val label = if (isCapturing) "■  STOP CAPTURE" else "▶  START CAPTURE"

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(pulseScale)
            .border(1.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = if (isCapturing) MetaRed else PrimaryAccent
        )
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 16.sp,
            letterSpacing = 2.sp
        )
    }
}
