package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.theme.*

@Composable
fun PermissionsScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) } // 1 = READ_SMS, 2 = RECEIVE_SMS

    val receiveSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onComplete()
    }

    val readSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val hasReceive = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasReceive) {
            onComplete()
        } else {
            step = 2
        }
    }

    Scaffold(containerColor = SurfaceBg) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mpesa_hero),
                        contentDescription = "M-Pesa SMS Ingestion Illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Surface(
                        color = BrandLime,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (step == 1) Icons.Default.Inbox else Icons.Default.Sms,
                                contentDescription = null,
                                tint = DarkGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Step $step of 2",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen
                            )
                        }
                    }

                    Text(
                        text = if (step == 1) {
                            stringResource(R.string.permissions_historical_title)
                        } else {
                            stringResource(R.string.permissions_live_title)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DarkGreen,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (step == 1) {
                            stringResource(R.string.permissions_historical_desc)
                        } else {
                            stringResource(R.string.permissions_live_desc)
                        },
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            if (step == 1) {
                                readSmsLauncher.launch(Manifest.permission.READ_SMS)
                            } else {
                                receiveSmsLauncher.launch(Manifest.permission.RECEIVE_SMS)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("grant_permission_button")
                    ) {
                        Text(
                            text = if (step == 1) {
                                stringResource(R.string.grant_read_sms)
                            } else {
                                stringResource(R.string.grant_receive_sms)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.testTag("skip_permissions_button")
                    ) {
                        Text(
                            text = stringResource(R.string.skip_manual_mode),
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
