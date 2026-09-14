package com.vexo.app.editor

data class EditorState(
    val clips: List<Clip> = emptyList()
) {
    val totalDurationMs: Long
        get() {
            var total = 0L
            clips.forEach { total += it.trimmedDurationMs }
            return total
        }

    fun withClips(newClips: List<Clip>): EditorState {
        val ordered = newClips.mapIndexed { i, c -> c.also { it.order = i } }
        return copy(clips = ordered)
    }

    fun clipStartOffsetMs(clipId: String): Long {
        var offset = 0L
        for (clip in clips) {
            if (clip.id == clipId) return offset
            offset += clip.trimmedDurationMs
        }
        return 0L
    }

    fun clipAtPosition(positionMs: Long): Pair<Clip, Long>? {
        var offset = 0L
        for (clip in clips) {
            val end = offset + clip.trimmedDurationMs
            if (positionMs < end) {
                return Pair(clip, positionMs - offset)
            }
            offset = end
        }
        return clips.lastOrNull()?.let { Pair(it, it.trimmedDurationMs) }
    }
}
