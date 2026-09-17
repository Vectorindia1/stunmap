package com.stunmap.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stunmap.R
import com.stunmap.ui.components.CaptureButton
import com.stunmap.ui.theme.DividerColor
import com.stunmap.ui.theme.JetBrainsMonoFamily
import com.stunmap.ui.theme.PrimaryAccent
import com.stunmap.ui.theme.TextPrimary
import com.stunmap.ui.theme.TextSecondary
import com.stunmap.ui.viewmodel.CaptureViewModel

@Composable
fun HomeScreen(
    onStartCapture: () -> Unit,
    onViewHistory: () -> Unit,
    onSettings: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.captureState.collectAsState()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onStartCapture(context)
            onStartCapture()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STUNMAP",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = PrimaryAccent,
                letterSpacing = 4.sp
            )
            IconButton(onClick = onSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }

        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "STUN/ICE Packet Analyzer",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        CaptureButton(
            isCapturing = state.isCapturing,
            onClick = {
                if (state.isCapturing) {
                    viewModel.onStopCapture(context)
                } else {
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnPermissionLauncher.launch(intent)
                    } else {
                        viewModel.onStartCapture(context)
                        onStartCapture()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Divider(color = DividerColor)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Session history",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                color = TextSecondary
            )
            TextButton(onClick = onViewHistory) {
                Text(
                    text = "View History →",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp,
                    color = PrimaryAccent
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "v1.0.0 — personal use only",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            color = TextSecondary.copy(alpha = 0.4f)
        )
    }
}
