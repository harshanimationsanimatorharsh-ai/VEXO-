package com.vexo.app.editor

import android.net.Uri
import java.util.UUID

data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val sourceDurationMs: Long,
    var trimStartMs: Long = 0L,
    var trimEndMs: Long = sourceDurationMs,
    var volume: Float = 1f,
    var speed: Float = 1f,
    var order: Int = 0
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}
