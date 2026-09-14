package com.vexo.app.editor

import android.net.Uri
import java.util.UUID

data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val sourceDurationMs: Long,          // original full duration
    var trimStartMs: Long = 0L,          // trim in-point
    var trimEndMs: Long = sourceDurationMs, // trim out-point
    var volume: Float = 1f,
    var speed: Float = 1f,
    var order: Int = 0
) {
    val trimmedDurationMs: Long get() = trimEndMs - trimStartMs

    fun copy(): Clip = Clip(
        id = id,
        uri = uri,
        sourceDurationMs = sourceDurationMs,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        volume = volume,
        speed = speed,
        order = order
    )
}
