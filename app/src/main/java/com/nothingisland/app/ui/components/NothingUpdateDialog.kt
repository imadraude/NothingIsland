package com.nothingisland.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingisland.app.model.AppUpdate
import com.nothingisland.app.ui.theme.NothingBlack
import com.nothingisland.app.ui.theme.NothingCardBorder
import com.nothingisland.app.ui.theme.NothingDarkSurface
import com.nothingisland.app.ui.theme.NothingGrey
import com.nothingisland.app.ui.theme.NothingRed
import com.nothingisland.app.ui.theme.NothingWhite

@Composable
fun NothingUpdateDialog(
    updateInfo: AppUpdate,
    isDownloading: Boolean,
    downloadProgress: Int,
    onConfirmUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        containerColor = NothingDarkSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, NothingCardBorder, RoundedCornerShape(16.dp)),
        title = {
            Column {
                Text(
                    text = "UPDATE AVAILABLE",
                    color = NothingWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "New release ${updateInfo.latestVersionName}",
                    color = NothingRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isDownloading) {
                    Text(
                        text = "Downloading APK: $downloadProgress%",
                        color = NothingWhite,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NothingRed,
                        trackColor = NothingCardBorder
                    )
                } else {
                    Text(
                        text = "RELEASE NOTES",
                        color = NothingGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                            .background(NothingBlack, RoundedCornerShape(8.dp))
                            .border(1.dp, NothingCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = updateInfo.releaseNotes.ifBlank { "Minor bug fixes and UI improvements." },
                            color = NothingWhite,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                Button(
                    onClick = onConfirmUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update Now", color = NothingWhite, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Later", color = NothingGrey)
                }
            }
        }
    )
}
