package com.vexo.app.editor

import android.net.Uri
import java.util.UUID

class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val sourceDurationMs: Long,
    var trimStartMs: Long = 0L,
    var trimEndMs: Long = sourceDurationMs,
    var volume: Float = 1.0f,
    var speed: Float = 1.0f,
    var order: Int = 0
) {
    val trimmedDurationMs: Long
        get() {
            val d = trimEndMs - trimStartMs
            return if (d > 0L) d else 0L
        }

    fun copy(
        id: String = this.id,
        uri: Uri = this.uri,
        sourceDurationMs: Long = this.sourceDurationMs,
        trimStartMs: Long = this.trimStartMs,
        trimEndMs: Long = this.trimEndMs,
        volume: Float = this.volume,
        speed: Float = this.speed,
        order: Int = this.order
    ): Clip {
        val c = Clip(id, uri, sourceDurationMs, trimStartMs, trimEndMs, volume, speed, order)
        return c
    }
}
