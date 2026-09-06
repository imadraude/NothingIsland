package com.nothingisland.app.model

data class AppUpdate(
    val latestVersionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val isUpdateAvailable: Boolean
)
