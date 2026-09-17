package com.stunmap.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stunmap.R
import com.stunmap.classifier.IpClassification
import com.stunmap.ui.components.CaptureButton
import com.stunmap.ui.components.IpCard
import com.stunmap.ui.theme.ActiveGreen
import com.stunmap.ui.theme.DividerColor
import com.stunmap.ui.theme.JetBrainsMonoFamily
import com.stunmap.ui.theme.PrimaryAccent
import com.stunmap.ui.theme.TextPrimary
import com.stunmap.ui.theme.TextSecondary
import com.stunmap.ui.theme.WarningAmber
import com.stunmap.ui.viewmodel.CaptureViewModel
import kotlinx.coroutines.delay

@Composable
fun LiveCaptureScreen(
    onStop: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.captureState.collectAsState()
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.isCapturing) {
        if (state.isCapturing) {
            while (true) {
                elapsed = state.durationMs
                delay(1000)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Status bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_dot),
                    contentDescription = null,
                    tint = if (state.isCapturing) ActiveGreen else TextSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (state.isCapturing) "LIVE" else "STOPPED",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (state.isCapturing) ActiveGreen else TextSecondary
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = formatDuration(elapsed),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))

        // Counters
        Row(modifier = Modifier.fillMaxWidth()) {
            StatChip("STUN PKTS", state.stunHitCount.toString())
            Spacer(Modifier.width(24.dp))
            StatChip("CANDIDATES", state.candidateIps.size.toString())
        }

        Spacer(Modifier.height(16.dp))

        // IP list
        if (state.isCapturing && state.candidateIps.isEmpty() && state.stunHitCount > 30) {
            Text(
                text = "⚠ No peer IP detected — WhatsApp may be using relay servers.",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                color = WarningAmber,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.candidateIps) { entry ->
                IpCard(
                    entry = entry,
                    classification = IpClassification.CANDIDATE,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        CaptureButton(
            isCapturing = state.isCapturing,
            onClick = {
                viewModel.onStopCapture(context)
                onStop()
            }
        )
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = PrimaryAccent
        )
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
