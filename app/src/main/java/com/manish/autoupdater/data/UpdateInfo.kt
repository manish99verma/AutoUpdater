package com.manish.autoupdater.data

import com.google.firebase.Timestamp

data class UpdateInfo(
    val apkUrl: String? = null,
    val versionCode: Int? = null,
    val versionName: String? = null,
    val updateNotes: String? = null,
    val uploadTime: Timestamp? = null
)
