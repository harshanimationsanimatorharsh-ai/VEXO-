package com.vexo.app.editor

class EditorState(
    val clips: List<Clip> = emptyList()
) {
    val totalDurationMs: Long
        get() {
            var total = 0L
            for (clip in clips) {
                total = total + clip.trimmedDurationMs
            }
            return total
        }

    fun withClips(newClips: List<Clip>): EditorState {
        for (i in newClips.indices) {
            newClips[i].order = i
        }
        return EditorState(newClips)
    }

    fun copy(clips: List<Clip> = this.clips): EditorState {
        return EditorState(clips)
    }

    fun clipStartOffsetMs(clipId: String): Long {
        var offset = 0L
        for (clip in clips) {
            if (clip.id == clipId) return offset
            offset = offset + clip.trimmedDurationMs
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
        val last = clips.lastOrNull() ?: return null
        return Pair(last, last.trimmedDurationMs)
    }
}
