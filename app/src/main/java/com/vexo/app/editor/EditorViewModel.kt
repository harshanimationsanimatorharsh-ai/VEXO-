package com.vexo.app.editor

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.UUID

class EditorViewModel : ViewModel() {

    private val undoRedo = UndoRedoManager()
    private val _state = MutableLiveData<EditorState>(EditorState())
    val state: LiveData<EditorState> = _state

    private val _selectedClipId = MutableLiveData<String?>(null)
    val selectedClipId: LiveData<String?> = _selectedClipId

    val canUndo: Boolean get() = undoRedo.canUndo
    val canRedo: Boolean get() = undoRedo.canRedo

    private fun current(): EditorState {
        return _state.value ?: EditorState()
    }

    private fun commit(newState: EditorState) {
        undoRedo.push(current())
        _state.value = newState
    }

    fun addClips(uris: List<Uri>, durations: Map<Uri, Long>) {
        val existing = current().clips.toMutableList()
        for (uri in uris) {
            val dur: Long = durations[uri] ?: 0L
            if (dur > 0L) {
                val clip = Clip(
                    uri = uri,
                    sourceDurationMs = dur,
                    trimStartMs = 0L,
                    trimEndMs = dur
                )
                existing.add(clip)
            }
        }
        commit(current().withClips(existing))
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
    }

    fun trimClip(clipId: String, newStartMs: Long, newEndMs: Long) {
        val newClips = ArrayList<Clip>()
        for (c in current().clips) {
            if (c.id == clipId) {
                val start = newStartMs.coerceIn(0L, c.sourceDurationMs)
                val end = newEndMs.coerceIn(start, c.sourceDurationMs)
                newClips.add(c.copy(trimStartMs = start, trimEndMs = end))
            } else {
                newClips.add(c)
            }
        }
        commit(current().withClips(newClips))
    }

    fun splitClip(clipId: String, splitAtMs: Long) {
        val clips = current().clips.toMutableList()
        var idx = -1
        for (i in clips.indices) {
            if (clips[i].id == clipId) { idx = i; break }
        }
        if (idx < 0) return

        val clip = clips[idx]
        val absoluteSplit = clip.trimStartMs + splitAtMs
        if (absoluteSplit <= clip.trimStartMs) return
        if (absoluteSplit >= clip.trimEndMs) return

        val left = clip.copy(trimEndMs = absoluteSplit)
        val right = Clip(
            id = UUID.randomUUID().toString(),
            uri = clip.uri,
            sourceDurationMs = clip.sourceDurationMs,
            trimStartMs = absoluteSplit,
            trimEndMs = clip.trimEndMs,
            volume = clip.volume,
            speed = clip.speed
        )

        clips.removeAt(idx)
        clips.add(idx, left)
        clips.add(idx + 1, right)
        commit(current().withClips(clips))
    }

    fun deleteClip(clipId: String) {
        val newClips = current().clips.filter { c -> c.id != clipId }
        commit(current().withClips(newClips))
        if (_selectedClipId.value == clipId) {
            _selectedClipId.value = null
        }
    }

    fun moveClip(fromIndex: Int, toIndex: Int) {
        val clips = current().clips.toMutableList()
        if (fromIndex !in clips.indices) return
        if (toIndex !in clips.indices) return
        val item = clips.removeAt(fromIndex)
        clips.add(toIndex, item)
        commit(current().withClips(clips))
    }

    fun setVolume(clipId: String, vol: Float) {
        val newClips = ArrayList<Clip>()
        for (c in current().clips) {
            if (c.id == clipId) {
                newClips.add(c.copy(volume = vol))
            } else {
                newClips.add(c)
            }
        }
        commit(current().withClips(newClips))
    }

    fun undo(): Boolean {
        val prev = undoRedo.undo(current()) ?: return false
        _state.value = prev
        return true
    }

    fun redo(): Boolean {
        val next = undoRedo.redo(current()) ?: return false
        _state.value = next
        return true
    }
}
