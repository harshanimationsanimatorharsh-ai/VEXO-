package com.vexo.app.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.*
import androidx.media3.ui.PlayerView
import android.provider.MediaStore
import java.io.File
import java.util.UUID

// ══════════════════════════════════════════════
//  CLIP
// ══════════════════════════════════════════════

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

    fun duplicate(
        newId: String = UUID.randomUUID().toString(),
        newTrimStart: Long = trimStartMs,
        newTrimEnd: Long = trimEndMs,
        newVolume: Float = volume,
        newSpeed: Float = speed
    ): Clip {
        return Clip(
            id = newId,
            uri = uri,
            sourceDurationMs = sourceDurationMs,
            trimStartMs = newTrimStart,
            trimEndMs = newTrimEnd,
            volume = newVolume,
            speed = newSpeed,
            order = order
        )
    }
}

// ══════════════════════════════════════════════
//  EDITOR STATE
// ══════════════════════════════════════════════

class EditorState(
    val clips: List<Clip> = emptyList()
) {
    val totalDurationMs: Long
        get() {
            var total = 0L
            for (c in clips) total += c.trimmedDurationMs
            return total
        }

    fun withClips(newClips: List<Clip>): EditorState {
        for (i in newClips.indices) newClips[i].order = i
        return EditorState(newClips)
    }

    fun clipStartOffsetMs(clipId: String): Long {
        var offset = 0L
        for (c in clips) {
            if (c.id == clipId) return offset
            offset += c.trimmedDurationMs
        }
        return 0L
    }
}

// ══════════════════════════════════════════════
//  UNDO REDO MANAGER
// ══════════════════════════════════════════════

class UndoRedoManager {
    private val undoStack = ArrayDeque<EditorState>()
    private val redoStack = ArrayDeque<EditorState>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun push(state: EditorState) {
        undoStack.addLast(state)
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: EditorState): EditorState? {
        if (!canUndo) return null
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        return prev
    }

    fun redo(current: EditorState): EditorState? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        return next
    }
}

// ══════════════════════════════════════════════
//  EDITOR VIEW MODEL
// ══════════════════════════════════════════════

class EditorViewModel : ViewModel() {

    private val undoRedo = UndoRedoManager()
    private val _state = MutableLiveData<EditorState>(EditorState())
    val state: LiveData<EditorState> = _state

    private val _selectedClipId = MutableLiveData<String?>(null)
    val selectedClipId: LiveData<String?> = _selectedClipId

    val canUndo: Boolean get() = undoRedo.canUndo
    val canRedo: Boolean get() = undoRedo.canRedo

    private fun current(): EditorState = _state.value ?: EditorState()

    private fun commit(newState: EditorState) {
        undoRedo.push(current())
        _state.value = newState
    }

    fun addClips(uris: List<Uri>, durations: Map<Uri, Long>) {
        val list = current().clips.toMutableList()
        for (uri in uris) {
            val dur = durations[uri] ?: 0L
            if (dur > 0L) {
                list.add(Clip(uri = uri, sourceDurationMs = dur))
            }
        }
        commit(current().withClips(list))
    }

    fun selectClip(id: String?) { _selectedClipId.value = id }

    fun trimClip(clipId: String, newStart: Long, newEnd: Long) {
        val list = ArrayList<Clip>()
        for (c in current().clips) {
            if (c.id == clipId) {
                val s = newStart.coerceIn(0L, c.sourceDurationMs)
                val e = newEnd.coerceIn(s + 100L, c.sourceDurationMs)
                list.add(c.duplicate(newId = c.id, newTrimStart = s, newTrimEnd = e))
            } else list.add(c)
        }
        commit(current().withClips(list))
    }

    fun splitClip(clipId: String, splitAtMs: Long) {
        val list = current().clips.toMutableList()
        var idx = -1
        for (i in list.indices) { if (list[i].id == clipId) { idx = i; break } }
        if (idx < 0) return
        val clip = list[idx]
        val abs = clip.trimStartMs + splitAtMs
        if (abs <= clip.trimStartMs || abs >= clip.trimEndMs) return
        val left  = clip.duplicate(newTrimEnd = abs)
        val right = clip.duplicate(newId = UUID.randomUUID().toString(), newTrimStart = abs)
        list.removeAt(idx)
        list.add(idx, left)
        list.add(idx + 1, right)
        commit(current().withClips(list))
    }

    fun deleteClip(clipId: String) {
        val list = current().clips.filter { it.id != clipId }
        commit(current().withClips(list))
        if (_selectedClipId.value == clipId) _selectedClipId.value = null
    }

    fun moveClip(from: Int, to: Int) {
        val list = current().clips.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        commit(current().withClips(list))
    }

    fun setVolume(clipId: String, vol: Float) {
        val list = ArrayList<Clip>()
        for (c in current().clips) {
            list.add(if (c.id == clipId) c.duplicate(newId = c.id, newVolume = vol) else c)
        }
        commit(current().withClips(list))
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

// ══════════════════════════════════════════════
//  PLAYBACK CONTROLLER
// ══════════════════════════════════════════════

class PlaybackController(private val context: Context) {

    var player: ExoPlayer? = null
        private set

    fun init(playerView: PlayerView) {
        release()
        val p = ExoPlayer.Builder(context).build()
        playerView.player = p
        p.playWhenReady = false
        player = p
    }

    fun loadState(state: EditorState, playWhenReady: Boolean = false) {
        val p = player ?: return
        p.stop()
        p.clearMediaItems()
        for (clip in state.clips) {
            val item = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                )
                .build()
            p.addMediaItem(item)
        }
        p.prepare()
        p.playWhenReady = playWhenReady
    }

    fun play()  { player?.play() }
    fun pause() { player?.pause() }

    fun seekToMs(posMs: Long, state: EditorState) {
        val p = player ?: return
        var offset = 0L
        for (i in state.clips.indices) {
            val clip = state.clips[i]
            val end = offset + clip.trimmedDurationMs
            if (posMs <= end) {
                val diff = (posMs - offset).coerceIn(0L, clip.trimmedDurationMs)
                p.seekTo(i, clip.trimStartMs + diff)
                return
            }
            offset = end
        }
        if (state.clips.isNotEmpty()) {
            val last = state.clips.last()
            p.seekTo(state.clips.size - 1, last.trimEndMs)
        }
    }

    fun currentPositionMs(state: EditorState): Long {
        val p = player ?: return 0L
        var offset = 0L
        val idx = p.currentMediaItemIndex
        for (i in state.clips.indices) {
            val clip = state.clips[i]
            if (i == idx) {
                val diff = (p.currentPosition - clip.trimStartMs).coerceAtLeast(0L)
                return offset + diff
            }
            offset += clip.trimmedDurationMs
        }
        return offset
    }

    fun addListener(l: Player.Listener) { player?.addListener(l) }

    fun release() {
        player?.release()
        player = null
    }
}

// ══════════════════════════════════════════════
//  EXPORT CONTROLLER
// ══════════════════════════════════════════════

class ExportController(private val context: Context) {

    interface ExportCallback {
        fun onProgress(percent: Int)
        fun onSuccess(filePath: String)
        fun onFailure(error: String)
    }

    private var transformer: Transformer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun export(state: EditorState, callback: ExportCallback) {
        if (state.clips.isEmpty()) { callback.onFailure("No clips"); return }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "Movies")
        if (!dir.exists()) dir.mkdirs()
        val outFile = File(dir, "VEXO_${System.currentTimeMillis()}.mp4")

        val items = ArrayList<EditedMediaItem>()
        for (clip in state.clips) {
            val mi = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                )
                .build()
            items.add(EditedMediaItem.Builder(mi).setRemoveAudio(clip.volume == 0f).build())
        }

        val seq  = EditedMediaItemSequence(items)
        val comp = Composition.Builder(listOf(seq)).build()

        transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(c: Composition, result: ExportResult) {
                    saveToGallery(outFile)
                    handler.post { callback.onSuccess(outFile.absolutePath) }
                }
                override fun onError(c: Composition, result: ExportResult, ex: ExportException) {
                    handler.post { callback.onFailure(ex.message ?: "Export error") }
                }
            })
            .build()

        transformer?.start(comp, outFile.absolutePath)
        pollProgress(callback)
    }

    private fun pollProgress(cb: ExportCallback) {
        val t = transformer ?: return
        val ph = ProgressHolder()
        val s  = t.getProgress(ph)
        if (s == Transformer.PROGRESS_STATE_AVAILABLE) cb.onProgress(ph.progress)
        if (s != Transformer.PROGRESS_STATE_NOT_STARTED) {
            handler.postDelayed({ pollProgress(cb) }, 300)
        }
    }

    private fun saveToGallery(file: File) {
        try {
            val cv = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VEXO")
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv)
            uri?.let { dest ->
                context.contentResolver.openOutputStream(dest)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun cancel() { transformer?.cancel(); transformer = null }
}

// ══════════════════════════════════════════════
//  TIMELINE VIEW
// ══════════════════════════════════════════════

class TimelineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onClipSelected(clipId: String)
        fun onPlayheadSeeked(positionMs: Long)
        fun onTrimChanged(clipId: String, newStartMs: Long, newEndMs: Long)
    }

    var listener: Listener? = null

    private var clips: List<Clip> = emptyList()
    private var totalMs: Long = 1L
    private var playheadMs: Long = 0L
    private var selectedId: String? = null

    private fun ppm(): Float {
        if (width <= 0 || totalMs <= 0L) return 1f
        return width.toFloat() / totalMs.toFloat()
    }

    private val clipPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val selPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.WHITE }
    private val phPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED; strokeWidth = 3f }
    private val txtPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 26f }
    private val handlePaint= Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#BB000000") }

    private val colors = listOf(
        Color.parseColor("#6C63FF"), Color.parseColor("#43E97B"),
        Color.parseColor("#FF6584"), Color.parseColor("#FFD700"),
        Color.parseColor("#00B4D8")
    )

    private enum class Drag { NONE, HEAD, LEFT, RIGHT }
    private var drag = Drag.NONE
    private var dragId: String? = null

    fun setClips(list: List<Clip>, total: Long, ph: Long, sel: String?) {
        clips = list
        totalMs = if (total > 0L) total else 1L
        playheadMs = ph
        selectedId = sel
        invalidate()
    }

    fun updatePlayhead(ms: Long) { playheadMs = ms; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val p = ppm()
        val h = height.toFloat()
        val ch = h * 0.65f
        val ct = (h - ch) / 2f
        var ox = 0f

        for (i in clips.indices) {
            val c = clips[i]
            val cw = c.trimmedDurationMs.toFloat() * p
            if (cw < 1f) { ox += cw; continue }
            clipPaint.color = colors[i % colors.size]
            val r = RectF(ox, ct, ox + cw, ct + ch)
            canvas.drawRoundRect(r, 10f, 10f, clipPaint)
            if (c.id == selectedId) {
                canvas.drawRoundRect(r, 10f, 10f, selPaint)
                canvas.drawRoundRect(RectF(ox, ct, ox + 18f, ct + ch), 6f, 6f, handlePaint)
                canvas.drawRoundRect(RectF(ox + cw - 18f, ct, ox + cw, ct + ch), 6f, 6f, handlePaint)
            }
            val lbl = "Clip ${i + 1}"
            val tw = txtPaint.measureText(lbl)
            if (tw < cw - 8f) canvas.drawText(lbl, ox + (cw - tw) / 2f, ct + ch / 2f + 10f, txtPaint)
            ox += cw
        }

        val px = playheadMs.toFloat() * p
        canvas.drawLine(px, 0f, px, h, phPaint)
        val tri = Path(); tri.moveTo(px - 12f, 0f); tri.lineTo(px + 12f, 0f); tri.lineTo(px, 24f); tri.close()
        canvas.drawPath(tri, phPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val p = ppm()
        val ms = (x / p).toLong().coerceIn(0L, totalMs)

        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                drag = Drag.NONE
                val sel = selectedId
                if (sel != null) {
                    var off = 0L
                    var sc: Clip? = null
                    for (c in clips) { if (c.id == sel) { sc = c; break }; off += c.trimmedDurationMs }
                    if (sc != null) {
                        val sp = off.toFloat() * p
                        val ep = sp + sc.trimmedDurationMs.toFloat() * p
                        if (x in sp..(sp + 30f)) { drag = Drag.LEFT; dragId = sel; return true }
                        if (x in (ep - 30f)..ep)  { drag = Drag.RIGHT; dragId = sel; return true }
                    }
                }
                val px = playheadMs.toFloat() * p
                if (Math.abs(x - px) < 40f) { drag = Drag.HEAD; return true }
                var off = 0L
                for (c in clips) {
                    val end = off + c.trimmedDurationMs
                    if (ms in off..end) { listener?.onClipSelected(c.id); return true }
                    off = end
                }
            }
            MotionEvent.ACTION_MOVE -> when (drag) {
                Drag.HEAD  -> { playheadMs = ms; listener?.onPlayheadSeeked(ms); invalidate() }
                Drag.LEFT  -> {
                    val cid = dragId ?: return false
                    var off = 0L; var clip: Clip? = null
                    for (c in clips) { if (c.id == cid) { clip = c; break }; off += c.trimmedDurationMs }
                    clip ?: return false
                    val ns = (clip.trimStartMs + (ms - off)).coerceIn(0L, clip.trimEndMs - 500L)
                    listener?.onTrimChanged(cid, ns, clip.trimEndMs)
                }
                Drag.RIGHT -> {
                    val cid = dragId ?: return false
                    var off = 0L; var clip: Clip? = null
                    for (c in clips) { if (c.id == cid) { clip = c; break }; off += c.trimmedDurationMs }
                    clip ?: return false
                    val ne = (clip.trimStartMs + (ms - off)).coerceIn(clip.trimStartMs + 500L, clip.sourceDurationMs)
                    listener?.onTrimChanged(cid, clip.trimStartMs, ne)
                }
                else -> {}
            }
            MotionEvent.ACTION_UP -> { drag = Drag.NONE; dragId = null }
        }
        return true
    }
}
