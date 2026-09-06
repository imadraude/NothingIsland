package com.nothingisland.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingisland.app.core.IslandStateManager
import com.nothingisland.app.model.CutoutConfig
import com.nothingisland.app.model.IslandState
import com.nothingisland.app.ui.theme.NothingCardBorder
import com.nothingisland.app.ui.theme.NothingGrey
import com.nothingisland.app.ui.theme.NothingRed
import com.nothingisland.app.ui.theme.NothingSubtleGrey
import com.nothingisland.app.ui.theme.NothingWhite

@Composable
fun ExpandedCardContent(
    state: IslandState.Expanded,
    config: CutoutConfig,
    stateManager: IslandStateManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top header row: perfectly aligned on left and right of hardware camera cutout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(config.cameraDiameterDp.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left of camera: App category / source
            Text(
                text = when (state) {
                    is IslandState.Expanded.Media -> state.media.appName.uppercase()
                    is IslandState.Expanded.Notification -> state.notification.packageName.split(".").lastOrNull()?.uppercase() ?: "ALERT"
                    is IslandState.Expanded.Battery -> "POWER"
                    is IslandState.Expanded.Timer -> "TIMER"
                },
                color = NothingGrey,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Clear camera cutout exclusion zone
            Spacer(modifier = Modifier.width((config.cameraDiameterDp + 16f).dp))

            // Right of camera: Collapse button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { stateManager.collapse() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Collapse",
                        tint = NothingGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Expanded Body (100% free of camera interference)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is IslandState.Expanded.Media -> {
                    MediaExpandedBody(state = state, stateManager = stateManager)
                }
                is IslandState.Expanded.Notification -> {
                    NotificationExpandedBody(state = state, stateManager = stateManager)
                }
                is IslandState.Expanded.Battery -> {
                    BatteryExpandedBody(state = state)
                }
                is IslandState.Expanded.Timer -> {
                    TimerExpandedBody(state = state)
                }
            }
        }
    }
}

@Composable
private fun MediaExpandedBody(
    state: IslandState.Expanded.Media,
    stateManager: IslandStateManager
) {
    val media = state.media

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Track info row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val art = media.albumArt
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingSubtleGrey),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title.ifBlank { "Nothing Playing" },
                    color = NothingWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = media.artist.ifBlank { "Unknown Artist" },
                    color = NothingGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            NdotVisualizer(
                isPlaying = media.isPlaying,
                activeColor = NothingRed,
                size = 22.dp
            )
        }

        // Progress bar and timestamps
        if (media.durationMs > 0) {
            val progress = (media.positionMs.toFloat() / media.durationMs).coerceIn(0f, 1f)
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = NothingRed,
                    trackColor = NothingCardBorder
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(media.positionMs),
                        color = NothingGrey,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "-${formatTime((media.durationMs - media.positionMs).coerceAtLeast(0L))}",
                        color = NothingGrey,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { stateManager.skipPrevious() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = NothingWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NothingWhite),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (media.isPlaying) stateManager.pauseMedia() else stateManager.playMedia()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (media.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            IconButton(
                onClick = { stateManager.skipNext() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = NothingWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationExpandedBody(
    state: IslandState.Expanded.Notification,
    stateManager: IslandStateManager
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = state.notification.title,
            color = NothingWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = state.notification.text,
            color = NothingGrey,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BatteryExpandedBody(
    state: IslandState.Expanded.Battery
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null,
            tint = NothingRed,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "${state.battery.percentage}% CHARGED",
                color = NothingWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (state.battery.isFastCharging) "Fast Charging (45W Max)" else "Standard Charging",
                color = NothingGrey,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TimerExpandedBody(
    state: IslandState.Expanded.Timer
) {
    val m = state.timer.remainingSeconds / 60
    val s = state.timer.remainingSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format("%02d:%02d", m, s),
            color = NothingRed,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.timer.label.ifBlank { "COUNTDOWN" }.uppercase(),
            color = NothingGrey,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%02d:%02d", m, s)
}

