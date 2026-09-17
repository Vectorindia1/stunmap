package com.stunmap.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stunmap.R
import com.stunmap.session.CaptureSession
import com.stunmap.ui.theme.DividerColor
import com.stunmap.ui.theme.JetBrainsMonoFamily
import com.stunmap.ui.theme.PrimaryAccent
import com.stunmap.ui.theme.TextPrimary
import com.stunmap.ui.theme.TextSecondary
import com.stunmap.ui.theme.WarningAmber
import com.stunmap.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onSessionClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = TextSecondary
                )
            }
            Text(
                text = "SESSION HISTORY",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryAccent,
                letterSpacing = 2.sp
            )
        }

        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))

        if (sessions.isEmpty()) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "No sessions yet.\nStart a capture to begin.",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            LazyColumn {
                items(sessions, key = { it.id }) { session ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteSession(session)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {},
                        content = {
                            SessionRow(
                                session = session,
                                dateFormat = dateFormat,
                                onClick = { onSessionClick(session.id) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: CaptureSession,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateFormat.format(Date(session.startedAt)),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (session.interrupted) {
                Text(
                    text = "INTERRUPTED",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    color = WarningAmber,
                    letterSpacing = 1.sp
                )
            }
        }
        Text(
            text = "${session.candidateCount} candidate(s) · ${session.stunHitCount} STUN pkts · ${formatDuration(session.durationMs)}",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
    Divider(color = DividerColor)
}

private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return if (s < 60) "${s}s" else if (s < 3600) "${s / 60}m ${s % 60}s" else "${s / 3600}h ${(s % 3600) / 60}m"
}
