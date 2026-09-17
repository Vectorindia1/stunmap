package com.stunmap.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stunmap.R
import com.stunmap.ui.theme.DividerColor
import com.stunmap.ui.theme.ErrorRed
import com.stunmap.ui.theme.JetBrainsMonoFamily
import com.stunmap.ui.theme.PrimaryAccent
import com.stunmap.ui.theme.SurfaceVariantDark
import com.stunmap.ui.theme.TextPrimary
import com.stunmap.ui.theme.TextSecondary
import com.stunmap.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var apiKeyInput by remember(state.apiKey) { mutableStateOf(state.apiKey) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text("Clear All History", fontFamily = JetBrainsMonoFamily, color = TextPrimary)
            },
            text = {
                Text(
                    "This will delete all sessions permanently.",
                    fontFamily = JetBrainsMonoFamily,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearDialog = false
                }) {
                    Text("Delete All", color = ErrorRed, fontFamily = JetBrainsMonoFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", fontFamily = JetBrainsMonoFamily)
                }
            }
        )
    }

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
                text = "SETTINGS",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryAccent,
                letterSpacing = 2.sp
            )
        }

        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))

        SettingSection("GeoIP API") {
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("IPinfo.io API Key", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp) },
                placeholder = { Text("Leave empty for offline only", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions.Default,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveApiKey(apiKeyInput) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceVariantDark,
                    contentColor = PrimaryAccent
                )
            ) {
                Text("Save Key", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
            }
        }

        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

        SettingSection("Display") {
            ToggleRow(
                label = "Show META / SELF IPs",
                checked = state.showNonCandidates,
                onCheckedChange = viewModel::setShowNonCandidates
            )
        }

        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

        SettingSection("Data") {
            Button(
                onClick = { viewModel.updateMetaAsnList(context) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceVariantDark,
                    contentColor = PrimaryAccent
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Meta ASN List", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showClearDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed.copy(alpha = 0.15f),
                    contentColor = ErrorRed
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All History", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        fontFamily = JetBrainsMonoFamily,
        fontSize = 10.sp,
        color = TextSecondary,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
    content()
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryAccent,
                checkedTrackColor = PrimaryAccent.copy(alpha = 0.3f)
            )
        )
    }
}
