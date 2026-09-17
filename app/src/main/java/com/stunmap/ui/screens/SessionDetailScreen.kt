package com.stunmap.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.stunmap.classifier.IpClassification
import com.stunmap.session.IpGeoEntry
import com.stunmap.ui.components.GeoMapView
import com.stunmap.ui.components.IpCard
import com.stunmap.ui.theme.DividerColor
import com.stunmap.ui.theme.JetBrainsMonoFamily
import com.stunmap.ui.theme.PrimaryAccent
import com.stunmap.ui.theme.SurfaceVariantDark
import com.stunmap.ui.theme.TextPrimary
import com.stunmap.ui.theme.TextSecondary
import com.stunmap.ui.viewmodel.SessionDetailViewModel
import com.stunmap.util.ExportUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = TextSecondary
                )
            }
            Text(
                text = "SESSION DETAIL",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryAccent,
                letterSpacing = 2.sp
            )
        }

        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))

        if (state.isLoading) {
            Text("Loading...", fontFamily = JetBrainsMonoFamily, color = TextSecondary)
            return
        }

        val session = state.session ?: run {
            Text("Session not found", fontFamily = JetBrainsMonoFamily, color = TextSecondary)
            return
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                // Session metadata
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    MetaRow("Start", dateFormat.format(Date(session.startedAt)))
                    session.endedAt?.let { MetaRow("End", dateFormat.format(Date(it))) }
                    MetaRow("Device IP", session.devicePublicIp)
                    MetaRow("STUN Hits", session.stunHitCount.toString())
                    MetaRow("Candidates", session.candidateCount.toString())
                    MetaRow("Duration", formatDuration(session.durationMs))
                }

                Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "CANDIDATE IPs",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            val candidates = state.hits
                .filter { it.classification == IpClassification.CANDIDATE }
                .mapNotNull { it.mappedIp }
                .distinct()

            if (candidates.isEmpty()) {
                item {
                    Text(
                        text = "No candidate IPs — possible TURN relay",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            } else {
                items(candidates) { ip ->
                    val geo = state.geoResults[ip]
                    IpCard(
                        entry = IpGeoEntry(ip, geo),
                        classification = IpClassification.CANDIDATE,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (geo?.latitude != null && geo.longitude != null) {
                        GeoMapView(
                            latitude = geo.latitude,
                            longitude = geo.longitude,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Divider(color = DividerColor)
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val intent = ExportUtils.exportAsJson(
                                context, session, state.hits, state.geoResults
                            )
                            intent?.let { context.startActivity(Intent.createChooser(it, "Export JSON")) }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = PrimaryAccent
                        )
                    ) {
                        Text("JSON", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                    }

                    Spacer(Modifier.padding(horizontal = 6.dp))

                    Button(
                        onClick = {
                            val intent = ExportUtils.exportAsText(
                                context, session, state.hits, state.geoResults
                            )
                            intent?.let { context.startActivity(Intent.createChooser(it, "Export Text")) }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = PrimaryAccent
                        )
                    ) {
                        Text("TEXT", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = "$label:",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            color = TextPrimary,
            modifier = Modifier.weight(0.65f)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return if (s < 60) "${s}s" else if (s < 3600) "${s / 60}m ${s % 60}s" else "${s / 3600}h ${(s % 3600) / 60}m"
}
