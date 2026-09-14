package com.vexo.app.editor

data class EditorState(
    val clips: List<Clip> = emptyList()
) {
    val totalDurationMs: Long get() = clips.sumOf { it.trimmedDurationMs }

    fun withClips(newClips: List<Clip>): EditorState =
        copy(clips = newClips.mapIndexed { i, c -> c.also { it.order = i } })

    /** Offset (ms) of clip's start within the full timeline */
    fun clipStartOffsetMs(clipId: String): Long {
        var offset = 0L
        for (clip in clips) {
            if (clip.id == clipId) return offset
            offset += clip.trimmedDurationMs
        }
        return 0L
    }

    /** Which clip contains playhead at positionMs */
    fun clipAtPosition(positionMs: Long): Pair<Clip, Long>? {
        var offset = 0L
        for (clip in clips) {
            val end = offset + clip.trimmedDurationMs
            if (positionMs < end) return Pair(clip, positionMs - offset)
            offset = end
        }
        return clips.lastOrNull()?.let { Pair(it, it.trimmedDurationMs) }
    }
}
