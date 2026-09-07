package com.nothingisland.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.nothingisland.app.BuildConfig
import com.nothingisland.app.IslandApplication
import com.nothingisland.app.data.update.GitHubUpdateManager
import com.nothingisland.app.model.AppUpdate
import com.nothingisland.app.ui.components.NothingUpdateDialog
import kotlinx.coroutines.launch
import com.nothingisland.app.model.CutoutConfig
import com.nothingisland.app.model.IslandEvent
import com.nothingisland.app.service.IslandNotificationListener
import com.nothingisland.app.service.IslandOverlayService
import com.nothingisland.app.ui.components.NothingIslandRoot
import com.nothingisland.app.ui.theme.NothingBlack
import com.nothingisland.app.ui.theme.NothingCardBorder
import com.nothingisland.app.ui.theme.NothingDarkSurface
import com.nothingisland.app.ui.theme.NothingGrey
import com.nothingisland.app.ui.theme.NothingIslandTheme
import com.nothingisland.app.ui.theme.NothingRed
import com.nothingisland.app.ui.theme.NothingWhite

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NothingIslandTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(isNotificationServiceEnabled(context))
    }
    var isServiceRunning by remember { mutableStateOf(false) }

    // Calibration settings for Nothing Phone (2a)
    var topMargin by remember { mutableFloatStateOf(IslandApplication.cutoutConfig.cameraTopMarginDp) }
    var centerXOffset by remember { mutableFloatStateOf(IslandApplication.cutoutConfig.cameraCenterXOffsetDp) }
    var pillHeight by remember { mutableFloatStateOf(IslandApplication.cutoutConfig.compactPillHeightDp) }
    var cameraDiameter by remember { mutableFloatStateOf(IslandApplication.cutoutConfig.cameraDiameterDp) }
    var compactWidth by remember { mutableFloatStateOf(IslandApplication.cutoutConfig.compactPillWidthDp) }
    var isAutoDetected by remember { mutableStateOf(IslandApplication.cutoutConfig.isAutoDetected) }

    val liveConfig = CutoutConfig(
        cameraCenterXOffsetDp = centerXOffset,
        cameraTopMarginDp = topMargin,
        cameraDiameterDp = cameraDiameter,
        compactPillHeightDp = pillHeight,
        compactMediaWidthDp = compactWidth,
        compactNotifWidthDp = (compactWidth + 50f).coerceAtLeast(180f),
        compactBatteryWidthDp = 100f,
        compactTimerWidthDp = 130f,
        compactVolumeWidthDp = 110f,
        compactPillWidthDp = compactWidth,
        expandedCardWidthDp = 340f,
        expandedCardHeightDp = 190f,
        isAutoDetected = isAutoDetected
    )

    LaunchedEffect(liveConfig) {
        if (!isAutoDetected) {
            IslandApplication.saveManualConfig(liveConfig)
        } else {
            IslandApplication.cutoutConfig = liveConfig
        }
    }

    val scope = rememberCoroutineScope()
    val updateManager = remember { GitHubUpdateManager(context) }
    var updateInfo by remember { mutableStateOf<AppUpdate?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateCheckStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val currentVer = BuildConfig.VERSION_NAME
        updateManager.checkForUpdate(currentVer).onSuccess { update ->
            if (update.isUpdateAvailable) {
                updateInfo = update
                showUpdateDialog = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nothing OS Header
        Text(
            text = "NOTHING ISLAND (2A)",
            color = NothingWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Text(
            text = "ADAPTIVE CUTOUT SYSTEM FOR NOTHING PHONE (2A)",
            color = NothingGrey,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive Live Simulator Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .border(1.dp, NothingCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = NothingDarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Simulated Nothing Phone (2a) Camera Punch-hole
                Box(
                    modifier = Modifier
                        .offset(x = centerXOffset.dp, y = topMargin.dp)
                        .size(cameraDiameter.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF080808))
                        .border(1.dp, Color(0xFF1E1E1E), CircleShape)
                )

                // The Actual Dynamic Island Running Over the Cutout
                NothingIslandRoot(
                    stateManager = IslandApplication.stateManager,
                    config = liveConfig
                )

                Text(
                    text = "LIVE INTERACTIVE PREVIEW",
                    color = NothingGrey.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulation Test Controls
        Text(
            text = "SIMULATE EVENTS",
            color = NothingWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    IslandApplication.stateManager.postEvent(
                        IslandEvent.Media(
                            packageName = "com.spotify.music",
                            appName = "Spotify",
                            title = "Birds of a Feather",
                            artist = "Billie Eilish",
                            isPlaying = true
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🎵 Music", fontSize = 11.sp, color = NothingWhite)
            }

            OutlinedButton(
                onClick = {
                    IslandApplication.stateManager.postEvent(
                        IslandEvent.Battery(
                            percentage = 88,
                            isCharging = true,
                            isFastCharging = true
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("⚡ Battery", fontSize = 11.sp, color = NothingWhite)
            }

            OutlinedButton(
                onClick = {
                    IslandApplication.stateManager.postEvent(
                        IslandEvent.Notification(
                            key = "notif_1",
                            packageName = "org.telegram.messenger",
                            title = "Telegram",
                            text = "New message from Nothing Community"
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🔔 Alert", fontSize = 11.sp, color = NothingWhite)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Overlay Service Master Switch
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NothingCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = NothingDarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "System Overlay Service",
                        color = NothingWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Float Island above status bar across all apps",
                        color = NothingGrey,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = { start ->
                        isServiceRunning = start
                        val intent = Intent(context, IslandOverlayService::class.java)
                        if (start) {
                            ContextCompat.startForegroundService(context, intent)
                        } else {
                            context.stopService(intent)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NothingWhite,
                        checkedTrackColor = NothingRed,
                        uncheckedTrackColor = NothingBlack
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permissions Section
        Text(
            text = "PERMISSIONS",
            color = NothingWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionCard(
            title = "Display over other apps",
            granted = hasOverlayPermission,
            onRequest = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionCard(
            title = "Notification & Media Access",
            granted = hasNotificationPermission,
            onRequest = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Nothing Phone (2a) Precision Calibration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CALIBRATION (NOTHING 2A)",
                color = NothingWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val detected = IslandApplication.autoDetectAndApply(context)
                        if (detected != null) {
                            topMargin = detected.cameraTopMarginDp
                            centerXOffset = detected.cameraCenterXOffsetDp
                            pillHeight = detected.compactPillHeightDp
                            cameraDiameter = detected.cameraDiameterDp
                            compactWidth = detected.compactPillWidthDp
                            isAutoDetected = true
                        }
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("⚡ Auto-Detect", color = NothingWhite, fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        IslandApplication.resetToDefaults()
                        val def = IslandApplication.cutoutConfig
                        topMargin = def.cameraTopMarginDp
                        centerXOffset = def.cameraCenterXOffsetDp
                        pillHeight = def.compactPillHeightDp
                        cameraDiameter = def.cameraDiameterDp
                        compactWidth = def.compactPillWidthDp
                        isAutoDetected = def.isAutoDetected
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("↺ Reset", color = NothingRed, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isAutoDetected) "● Cutout auto-detected from hardware" else "○ Manual adjustments applied",
            color = if (isAutoDetected) Color(0xFF4CAF50) else NothingGrey,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Top Margin (Y Offset): ${String.format("%.1f", topMargin)} dp",
            color = NothingGrey,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Slider(
            value = topMargin,
            onValueChange = {
                topMargin = it
                isAutoDetected = false
            },
            valueRange = 0f..25f,
            colors = SliderDefaults.colors(
                thumbColor = NothingRed,
                activeTrackColor = NothingRed,
                inactiveTrackColor = NothingCardBorder
            )
        )

        Text(
            text = "Horizontal Offset (X): ${String.format("%.1f", centerXOffset)} dp",
            color = NothingGrey,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Slider(
            value = centerXOffset,
            onValueChange = {
                centerXOffset = it
                isAutoDetected = false
            },
            valueRange = -20f..20f,
            colors = SliderDefaults.colors(
                thumbColor = NothingRed,
                activeTrackColor = NothingRed,
                inactiveTrackColor = NothingCardBorder
            )
        )

        Text(
            text = "Pill Height: ${pillHeight.toInt()} dp",
            color = NothingGrey,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Slider(
            value = pillHeight,
            onValueChange = {
                pillHeight = it
                isAutoDetected = false
            },
            valueRange = 28f..44f,
            colors = SliderDefaults.colors(
                thumbColor = NothingRed,
                activeTrackColor = NothingRed,
                inactiveTrackColor = NothingCardBorder
            )
        )

        Text(
            text = "Camera Hole Diameter: ${cameraDiameter.toInt()} dp",
            color = NothingGrey,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Slider(
            value = cameraDiameter,
            onValueChange = {
                cameraDiameter = it
                isAutoDetected = false
            },
            valueRange = 20f..40f,
            colors = SliderDefaults.colors(
                thumbColor = NothingRed,
                activeTrackColor = NothingRed,
                inactiveTrackColor = NothingCardBorder
            )
        )

        Text(
            text = "Compact Width: ${compactWidth.toInt()} dp",
            color = NothingGrey,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Slider(
            value = compactWidth,
            onValueChange = {
                compactWidth = it
                isAutoDetected = false
            },
            valueRange = 100f..220f,
            colors = SliderDefaults.colors(
                thumbColor = NothingRed,
                activeTrackColor = NothingRed,
                inactiveTrackColor = NothingCardBorder
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // System Updates Section
        Text(
            text = "APP UPDATES",
            color = NothingWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NothingCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = NothingDarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Version v${BuildConfig.VERSION_NAME}",
                            color = NothingWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = updateCheckStatus ?: "Tap to check latest release on GitHub",
                            color = NothingGrey,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isCheckingUpdate = true
                                updateCheckStatus = "Checking GitHub..."
                                updateManager.checkForUpdate(BuildConfig.VERSION_NAME)
                                    .onSuccess { update ->
                                        isCheckingUpdate = false
                                        if (update.isUpdateAvailable) {
                                            updateInfo = update
                                            showUpdateDialog = true
                                            updateCheckStatus = "New version ${update.latestVersionName} available!"
                                        } else {
                                            updateCheckStatus = "You have the latest version."
                                        }
                                    }
                                    .onFailure { err ->
                                        isCheckingUpdate = false
                                        updateCheckStatus = "Failed: ${err.localizedMessage}"
                                    }
                            }
                        },
                        enabled = !isCheckingUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isCheckingUpdate) "..." else "Check", color = NothingWhite, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Update Dialog
    updateInfo?.let { update ->
        if (showUpdateDialog) {
            NothingUpdateDialog(
                updateInfo = update,
                isDownloading = isDownloadingUpdate,
                downloadProgress = downloadProgress,
                onConfirmUpdate = {
                    scope.launch {
                        isDownloadingUpdate = true
                        downloadProgress = 0
                        updateManager.downloadApk(update.apkDownloadUrl) { progress ->
                            downloadProgress = progress
                        }.onSuccess { file ->
                            isDownloadingUpdate = false
                            showUpdateDialog = false
                            updateManager.installApk(file)
                        }.onFailure { err ->
                            isDownloadingUpdate = false
                            updateCheckStatus = "Download error: ${err.localizedMessage}"
                        }
                    }
                },
                onDismiss = {
                    showUpdateDialog = false
                }
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NothingCardBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = NothingDarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (granted) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (granted) Color(0xFF4CAF50) else NothingRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = NothingWhite,
                    fontSize = 14.sp
                )
            }

            if (!granted) {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Grant", fontSize = 12.sp, color = NothingWhite)
                }
            } else {
                Text(
                    text = "Active",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}
