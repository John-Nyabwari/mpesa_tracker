package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.UserPreferencesEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: UserPreferencesEntity?,
    feedbackMessage: String?,
    onClearFeedback: () -> Unit,
    onBack: () -> Unit,
    onSaveProfile: (name: String, currency: String, senders: String) -> Unit,
    onUpdateTrackingStartDate: (Long) -> Unit
) {
    val context = LocalContext.current
    var name by remember(prefs) { mutableStateOf(prefs?.displayName ?: "M-Pesa User") }
    var currency by remember(prefs) { mutableStateOf(prefs?.currencySymbol ?: "KSh") }
    var senders by remember(prefs) { mutableStateOf(prefs?.allowedSenderIds ?: "MPESA,M-PESA,SAFARICOM") }

    val trackingDate = prefs?.trackingStartDate ?: 0L
    val dateDisplay = if (trackingDate == 0L) {
        "All Historical SMS (No start restriction)"
    } else {
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(trackingDate))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Sync", fontWeight = FontWeight.Bold, color = DarkGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (feedbackMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkGreen)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(feedbackMessage, color = BrandLime, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onClearFeedback) {
                            Text("OK", color = SurfaceWhite)
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Profile & SMS Sender Whitelist", fontWeight = FontWeight.Bold, color = DarkGreen)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_name_input")
                    )

                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency Symbol (e.g. KSh, KES)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_currency_input")
                    )

                    OutlinedTextField(
                        value = senders,
                        onValueChange = { senders = it },
                        label = { Text("Allowed Sender IDs (comma-separated)") },
                        supportingText = {
                            Text("Default: MPESA,M-PESA,SAFARICOM. Messages from other senders are ignored.")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_senders_input")
                    )

                    Button(
                        onClick = { onSaveProfile(name, currency, senders) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                    ) {
                        Text("Save Profile")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tracking Start Date & Historical Scan", fontWeight = FontWeight.Bold, color = DarkGreen)
                    Text(
                        "Only M-Pesa SMS messages from this date onwards are included in your ledger. Updating this date automatically triggers a background HistoricalScanWorker re-scan of your SMS inbox.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Surface(
                        color = SurfaceBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Active Start Boundary", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = dateDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkGreen,
                                    modifier = Modifier.testTag("tracking_date_display")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateTrackingStartDate(0L) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_tracking_date_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Time")
                        }

                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (trackingDate > 0L) cal.timeInMillis = trackingDate
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selectedCal = Calendar.getInstance().apply {
                                            set(year, month, day, 0, 0, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        onUpdateTrackingStartDate(selectedCal.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_tracking_date_button")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Start Date")
                        }
                    }
                }
            }
        }
    }
}
