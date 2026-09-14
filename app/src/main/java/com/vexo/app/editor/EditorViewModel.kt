package com.vexo.app.editor

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EditorViewModel : ViewModel() {

    private val undoRedo = UndoRedoManager()

    private val _state = MutableLiveData(EditorState())
    val state: LiveData<EditorState> = _state

    private val _selectedClipId = MutableLiveData<String?>(null)
    val selectedClipId: LiveData<String?> = _selectedClipId

    val canUndo: Boolean get() = undoRedo.canUndo
    val canRedo: Boolean get() = undoRedo.canRedo

    private fun current() = _state.value ?: EditorState()

    private fun commit(newState: EditorState) {
        undoRedo.push(current())
        _state.value = newState
    }

    // ── Import ─────────────────────────────────────────────────
    fun addClips(uris: List<Uri>, durations: Map<Uri, Long>) {
        val existing = current().clips.toMutableList()
        uris.forEach { uri ->
            val dur = durations[uri] ?: 0L
            if (dur > 0) {
                existing.add(Clip(uri = uri, sourceDurationMs = dur))
            }
        }
        commit(current().withClips(existing))
    }

    // ── Select ─────────────────────────────────────────────────
    fun selectClip(clipId: String?) { _selectedClipId.value = clipId }

    // ── Trim ───────────────────────────────────────────────────
    fun trimClip(clipId: String, newStartMs: Long, newEndMs: Long) {
        val clips = current().clips.map { c ->
            if (c.id == clipId) c.copy().also {
                it.trimStartMs = newStartMs.coerceIn(0, c.sourceDurationMs)
                it.trimEndMs   = newEndMs.coerceIn(newStartMs, c.sourceDurationMs)
            } else c
        }
        commit(current().withClips(clips))
    }

    // ── Split ──────────────────────────────────────────────────
    fun splitClip(clipId: String, splitAtMs: Long) {
        // splitAtMs = position within TRIMMED clip
        val clips = current().clips.toMutableList()
        val idx = clips.indexOfFirst { it.id == clipId }
        if (idx < 0) return
        val clip = clips[idx]

        val absoluteSplit = clip.trimStartMs + splitAtMs
        if (absoluteSplit <= clip.trimStartMs || absoluteSplit >= clip.trimEndMs) return

        val left  = clip.copy().also { it.trimEndMs   = absoluteSplit }
        val right = Clip(
            uri              = clip.uri,
            sourceDurationMs = clip.sourceDurationMs,
            trimStartMs      = absoluteSplit,
            trimEndMs        = clip.trimEndMs,
            volume           = clip.volume,
            speed            = clip.speed
        )

        clips.removeAt(idx)
        clips.add(idx, left)
        clips.add(idx + 1, right)
        commit(current().withClips(clips))
    }

    // ── Delete ─────────────────────────────────────────────────
    fun deleteClip(clipId: String) {
        val clips = current().clips.filter { it.id != clipId }
        commit(current().withClips(clips))
        if (_selectedClipId.value == clipId) _selectedClipId.value = null
    }

    // ── Reorder ────────────────────────────────────────────────
    fun moveClip(fromIndex: Int, toIndex: Int) {
        val clips = current().clips.toMutableList()
        if (fromIndex !in clips.indices || toIndex !in clips.indices) return
        val item = clips.removeAt(fromIndex)
        clips.add(toIndex, item)
        commit(current().withClips(clips))
    }

    // ── Volume / Speed ─────────────────────────────────────────
    fun setVolume(clipId: String, vol: Float) {
        val clips = current().clips.map { c ->
            if (c.id == clipId) c.copy().also { it.volume = vol } else c
        }
        commit(current().withClips(clips))
    }

    // ── Undo / Redo ────────────────────────────────────────────
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
