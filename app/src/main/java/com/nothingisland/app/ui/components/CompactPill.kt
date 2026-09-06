package com.nothingisland.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingisland.app.model.CutoutConfig
import com.nothingisland.app.model.IslandState
import com.nothingisland.app.ui.theme.NothingBlack
import com.nothingisland.app.ui.theme.NothingGrey
import com.nothingisland.app.ui.theme.NothingRed
import com.nothingisland.app.ui.theme.NothingWhite

@Composable
fun CompactPillContent(
    state: IslandState.Compact,
    config: CutoutConfig,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side of camera hole
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            when (state) {
                is IslandState.Compact.Media -> {
                    val art = state.media.albumArt
                    if (art != null) {
                        Image(
                            bitmap = art.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(NothingRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NothingRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                is IslandState.Compact.Notification -> {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is IslandState.Compact.Battery -> {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                is IslandState.Compact.Volume -> {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is IslandState.Compact.Timer -> {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Center space reserved for the physical punch-hole camera on Nothing Phone (2a)
        Spacer(modifier = Modifier.width(config.cameraDiameterDp.dp))

        // Right side of camera hole
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd
        ) {
            when (state) {
                is IslandState.Compact.Media -> {
                    NdotVisualizer(
                        isPlaying = state.media.isPlaying,
                        activeColor = NothingRed,
                        size = 18.dp
                    )
                }
                is IslandState.Compact.Notification -> {
                    Text(
                        text = state.notification.title,
                        color = NothingWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        fontFamily = FontFamily.Monospace
                    )
                }
                is IslandState.Compact.Battery -> {
                    Text(
                        text = "${state.battery.percentage}%",
                        color = if (state.battery.isCharging) NothingRed else NothingWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                is IslandState.Compact.Volume -> {
                    val pct = ((state.volume.volume.toFloat() / state.volume.maxVolume.coerceAtLeast(1)) * 100).toInt()
                    Text(
                        text = "$pct%",
                        color = NothingWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                is IslandState.Compact.Timer -> {
                    val m = state.timer.remainingSeconds / 60
                    val s = state.timer.remainingSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", m, s),
                        color = NothingRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
