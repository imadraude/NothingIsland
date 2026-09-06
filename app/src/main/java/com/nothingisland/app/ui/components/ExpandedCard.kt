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
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top header with camera clearance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (state) {
                    is IslandState.Expanded.Media -> state.media.appName.uppercase()
                    is IslandState.Expanded.Notification -> state.notification.packageName.split(".").lastOrNull()?.uppercase() ?: "ALERT"
                    is IslandState.Expanded.Battery -> "CHARGING"
                    is IslandState.Expanded.Timer -> "TIMER"
                },
                color = NothingGrey,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )

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

        // Center Content based on state
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

@Composable
private fun MediaExpandedBody(
    state: IslandState.Expanded.Media,
    stateManager: IslandStateManager
) {
    val media = state.media

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        val art = media.albumArt
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingSubtleGrey),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = NothingRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title & Artist
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

        NdotVisualizer(
            isPlaying = media.isPlaying,
            activeColor = NothingRed,
            size = 22.dp
        )
    }

    // Media Controls
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { stateManager.skipPrevious() }) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = NothingWhite,
                modifier = Modifier.size(26.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(NothingWhite),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = {
                if (media.isPlaying) stateManager.pauseMedia() else stateManager.playMedia()
            }) {
                Icon(
                    imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (media.isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        IconButton(onClick = { stateManager.skipNext() }) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = NothingWhite,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun NotificationExpandedBody(
    state: IslandState.Expanded.Notification,
    stateManager: IslandStateManager
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = state.notification.title,
            color = NothingWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.notification.text,
            color = NothingGrey,
            fontSize = 12.sp,
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null,
            tint = NothingRed,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "${state.battery.percentage}% Charged",
                color = NothingWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (state.battery.isFastCharging) "Fast Charging (Nothing Power)" else "Standard Charging",
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
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format("%02d:%02d", m, s),
            color = NothingRed,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Text(
            text = state.timer.label.ifBlank { "Countdown" },
            color = NothingGrey,
            fontSize = 12.sp
        )
    }
}
