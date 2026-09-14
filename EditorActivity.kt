package com.vexo.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.util.UUID

// ─────────────────────────────────────────────
// VClip — single video clip model
// ─────────────────────────────────────────────
class VClip(
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
        get() = trimEndMs - trimStartMs

    fun cloneWith(
        id: String = this.id,
        uri: Uri = this.uri,
        sourceDurationMs: Long = this.sourceDurationMs,
        trimStartMs: Long = this.trimStartMs,
        trimEndMs: Long = this.trimEndMs,
        volume: Float = this.volume,
        speed: Float = this.speed,
        order: Int = this.order
    ): VClip = VClip(id, uri, sourceDurationMs, trimStartMs, trimEndMs, volume, speed, order)
}

// ─────────────────────────────────────────────
// VProject — holds list of clips
// ─────────────────────────────────────────────
class VProject(val clips: List<VClip> = emptyList()) {

    val totalDurationMs: Long
        get() {
            var total = 0L
            for (c in clips) total += c.trimmedDurationMs
            return total
        }

    fun withClips(newClips: List<VClip>): VProject {
        val sorted = newClips.toMutableList()
        for (i in sorted.indices) sorted[i].order = i
        return VProject(sorted)
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

// ─────────────────────────────────────────────
// VUndoRedo — undo/redo stack
// ─────────────────────────────────────────────
class VUndoRedo {
    private val undoStack = ArrayDeque<VProject>()
    private val redoStack = ArrayDeque<VProject>()

    fun push(project: VProject) {
        undoStack.addLast(project)
        redoStack.clear()
    }

    fun undo(current: VProject): VProject? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current)
        return undoStack.removeLast()
    }

    fun redo(current: VProject): VProject? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current)
        return redoStack.removeLast()
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()
}

// ─────────────────────────────────────────────
// VEditorViewModel
// ─────────────────────────────────────────────
class VEditorViewModel : ViewModel() {

    private val _project = MutableLiveData(VProject())
    val project: LiveData<VProject> = _project

    private val _selectedClipId = MutableLiveData<String?>(null)
    val selectedClipId: LiveData<String?> = _selectedClipId

    private val undoRedo = VUndoRedo()

    private fun current() = _project.value ?: VProject()

    private fun commit(newProject: VProject) {
        undoRedo.push(current())
        _project.value = newProject
    }

    fun addClips(uris: List<Uri>, durations: Map<Uri, Long>) {
        val existing = current().clips.toMutableList()
        for (uri in uris) {
            val dur = durations[uri] ?: 0L
            if (dur > 0L) existing.add(VClip(uri = uri, sourceDurationMs = dur))
        }
        commit(current().withClips(existing))
    }

    fun selectClip(id: String?) {
        _selectedClipId.value = id
    }

    fun trimClip(clipId: String, newStart: Long, newEnd: Long) {
        val clips = current().clips.map { c ->
            if (c.id == clipId) c.cloneWith(trimStartMs = newStart, trimEndMs = newEnd) else c
        }
        commit(current().withClips(clips))
    }

    fun splitClip(clipId: String, positionInClipMs: Long) {
        val clips = current().clips.toMutableList()
        val idx = clips.indexOfFirst { it.id == clipId }
        if (idx < 0) return
        val original = clips[idx]
        val splitPoint = original.trimStartMs + positionInClipMs
        if (splitPoint <= original.trimStartMs || splitPoint >= original.trimEndMs) return
        val left = original.cloneWith(id = UUID.randomUUID().toString(), trimEndMs = splitPoint)
        val right = original.cloneWith(id = UUID.randomUUID().toString(), trimStartMs = splitPoint)
        clips.removeAt(idx)
        clips.add(idx, right)
        clips.add(idx, left)
        commit(current().withClips(clips))
    }

    fun deleteClip(clipId: String) {
        val clips = current().clips.filter { it.id != clipId }
        if (_selectedClipId.value == clipId) _selectedClipId.value = null
        commit(current().withClips(clips))
    }

    fun moveClip(clipId: String, direction: Int) {
        val clips = current().clips.toMutableList()
        val idx = clips.indexOfFirst { it.id == clipId }
        val newIdx = idx + direction
        if (idx < 0 || newIdx < 0 || newIdx >= clips.size) return
        val temp = clips[idx]
        clips[idx] = clips[newIdx]
        clips[newIdx] = temp
        commit(current().withClips(clips))
    }

    fun setVolume(clipId: String, vol: Float) {
        val clips = current().clips.map { c ->
            if (c.id == clipId) c.cloneWith(volume = vol) else c
        }
        commit(current().withClips(clips))
    }

    fun setSpeed(clipId: String, spd: Float) {
        val clips = current().clips.map { c ->
            if (c.id == clipId) c.cloneWith(speed = spd) else c
        }
        commit(current().withClips(clips))
    }

    fun undo() {
        val prev = undoRedo.undo(current()) ?: return
        _project.value = prev
    }

    fun redo() {
        val next = undoRedo.redo(current()) ?: return
        _project.value = next
    }

    fun canUndo() = undoRedo.canUndo()
    fun canRedo() = undoRedo.canRedo()
}

// ─────────────────────────────────────────────
// VPlayback — ExoPlayer wrapper
// ─────────────────────────────────────────────
class VPlayback(private val context: Context) {

    private var player: ExoPlayer? = null

    fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
        }
        return player!!
    }

    fun load(project: VProject) {
        val p = getPlayer()
        p.stop()
        p.clearMediaItems()
        for (clip in project.clips) {
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
    }

    fun play() { player?.play() }
    fun pause() { player?.pause() }
    fun isPlaying() = player?.isPlaying ?: false
    fun seekTo(ms: Long) { player?.seekTo(ms) }
    fun currentPositionMs() = player?.currentPosition ?: 0L

    fun addListener(listener: Player.Listener) {
        player?.addListener(listener)
    }

    fun setVolume(vol: Float) { player?.volume = vol }

    fun release() {
        player?.release()
        player = null
    }
}

// ─────────────────────────────────────────────
// VExport — Media3 Transformer export
// ─────────────────────────────────────────────
class VExport(private val context: Context) {

    interface ExportCallback {
        fun onProgress(percent: Int)
        fun onSuccess(uri: Uri)
        fun onFailure(error: String)
    }

    private var transformer: Transformer? = null

    fun export(project: VProject, callback: ExportCallback) {
        if (project.clips.isEmpty()) {
            callback.onFailure("No clips to export")
            return
        }

        val outputFile = File(context.cacheDir, "vexo_export_${System.currentTimeMillis()}.mp4")

        val items = project.clips.map { clip ->
            val mediaItem = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                )
                .build()
            EditedMediaItem.Builder(mediaItem).build()
        }

        val sequence = EditedMediaItemSequence(items)
        val composition = Composition.Builder(listOf(sequence)).build()

        transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(comp: Composition, result: ExportResult) {
                    saveToGallery(outputFile, callback)
                }
                override fun onError(comp: Composition, result: ExportResult, exception: ExportException) {
                    callback.onFailure(exception.message ?: "Export failed")
                }
            })
            .build()

        transformer?.start(composition, outputFile.absolutePath)

        // Progress polling
        val handler = Handler(Looper.getMainLooper())
        val progressRunnable = object : Runnable {
            override fun run() {
                val t = transformer ?: return
                val progressHolder = androidx.media3.transformer.ProgressHolder()
                val state = t.getProgress(progressHolder)
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    callback.onProgress(progressHolder.progress)
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.postDelayed(progressRunnable, 500)
    }

    private fun saveToGallery(file: File, callback: ExportCallback) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "VEXO_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VEXO")
            }
            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                callback.onSuccess(uri)
            } else {
                callback.onFailure("Could not save to gallery")
            }
        } catch (e: Exception) {
            callback.onFailure(e.message ?: "Save failed")
        }
    }

    fun cancel() { transformer?.cancel() }
}

// ─────────────────────────────────────────────
// VTimeline — Custom Canvas Timeline View
// ─────────────────────────────────────────────
class VTimeline @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onClipSelected(clipId: String)
        fun onSeek(positionMs: Long)
        fun onTrim(clipId: String, newStartMs: Long, newEndMs: Long)
    }

    var listener: Listener? = null
    private var project: VProject = VProject()
    private var selectedClipId: String? = null
    private var playheadMs: Long = 0L

    private val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6C63FF") }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#43E97B") }
    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        strokeWidth = 3f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
    }
    private val trimHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF6584") }

    private val clipRect = RectF()
    private val CLIP_HEIGHT = 70f
    private val CLIP_TOP = 25f
    private val HANDLE_WIDTH = 18f
    private val MIN_TRIM_MS = 500L

    private enum class DragMode { NONE, HEAD, LEFT, RIGHT, CLIP }
    private var dragMode = DragMode.NONE
    private var dragClipId: String? = null
    private var lastTouchX = 0f

    fun setProject(p: VProject) {
        project = p
        invalidate()
    }

    fun setSelectedClipId(id: String?) {
        selectedClipId = id
        invalidate()
    }

    fun setPlayheadMs(ms: Long) {
        playheadMs = ms
        invalidate()
    }

    private fun totalMs() = maxOf(project.totalDurationMs, 1L)
    private fun msToX(ms: Long) = (ms.toFloat() / totalMs()) * width
    private fun xToMs(x: Float) = ((x / width) * totalMs()).toLong().coerceIn(0L, totalMs())

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#0A0A0F"))

        var offsetMs = 0L
        for (clip in project.clips) {
            val startX = msToX(offsetMs)
            val endX = msToX(offsetMs + clip.trimmedDurationMs)
            clipRect.set(startX + 2, CLIP_TOP, endX - 2, CLIP_TOP + CLIP_HEIGHT)

            val paint = if (clip.id == selectedClipId) selectedPaint else clipPaint
            canvas.drawRoundRect(clipRect, 8f, 8f, paint)

            // Trim handles
            canvas.drawRoundRect(RectF(startX + 2, CLIP_TOP, startX + HANDLE_WIDTH, CLIP_TOP + CLIP_HEIGHT), 4f, 4f, trimHandlePaint)
            canvas.drawRoundRect(RectF(endX - HANDLE_WIDTH, CLIP_TOP, endX - 2, CLIP_TOP + CLIP_HEIGHT), 4f, 4f, trimHandlePaint)

            // Clip label
            canvas.drawText("Clip ${clip.order + 1}", startX + HANDLE_WIDTH + 4, CLIP_TOP + 44f, textPaint)

            offsetMs += clip.trimmedDurationMs
        }

        // Playhead
        val phX = msToX(playheadMs)
        canvas.drawLine(phX, 0f, phX, height.toFloat(), playheadPaint)
        canvas.drawCircle(phX, 12f, 10f, playheadPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragMode = DragMode.NONE
                var offsetMs = 0L
                for (clip in project.clips) {
                    val startX = msToX(offsetMs)
                    val endX = msToX(offsetMs + clip.trimmedDurationMs)

                    if (y in CLIP_TOP..(CLIP_TOP + CLIP_HEIGHT)) {
                        when {
                            x in startX..(startX + HANDLE_WIDTH) -> {
                                dragMode = DragMode.LEFT
                                dragClipId = clip.id
                            }
                            x in (endX - HANDLE_WIDTH)..endX -> {
                                dragMode = DragMode.RIGHT
                                dragClipId = clip.id
                            }
                            x in startX..endX -> {
                                dragMode = DragMode.CLIP
                                dragClipId = clip.id
                                listener?.onClipSelected(clip.id)
                            }
                        }
                        if (dragMode != DragMode.NONE) break
                    }
                    offsetMs += clip.trimmedDurationMs
                }

                // Playhead drag
                val phX = msToX(playheadMs)
                if (dragMode == DragMode.NONE && Math.abs(x - phX) < 30f) {
                    dragMode = DragMode.HEAD
                }

                lastTouchX = x
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastTouchX
                val dMs = xToMs(x) - xToMs(lastTouchX)

                when (dragMode) {
                    DragMode.HEAD -> {
                        val newPos = (playheadMs + dMs).coerceIn(0L, totalMs())
                        listener?.onSeek(newPos)
                    }
                    DragMode.LEFT -> {
                        val clipId = dragClipId ?: return true
                        val clip = project.clips.find { it.id == clipId } ?: return true
                        val newStart = (clip.trimStartMs + dMs).coerceIn(0L, clip.trimEndMs - MIN_TRIM_MS)
                        listener?.onTrim(clipId, newStart, clip.trimEndMs)
                    }
                    DragMode.RIGHT -> {
                        val clipId = dragClipId ?: return true
                        val clip = project.clips.find { it.id == clipId } ?: return true
                        val newEnd = (clip.trimEndMs + dMs).coerceIn(clip.trimStartMs + MIN_TRIM_MS, clip.sourceDurationMs)
                        listener?.onTrim(clipId, clip.trimStartMs, newEnd)
                    }
                    else -> {}
                }
                lastTouchX = x
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                dragMode = DragMode.NONE
                dragClipId = null
            }
        }
        return true
    }
}

// ─────────────────────────────────────────────
// EditorActivity — Main Editor Screen
// ─────────────────────────────────────────────
class EditorActivity : AppCompatActivity() {

    private val viewModel: VEditorViewModel by viewModels()
    private lateinit var playback: VPlayback
    private lateinit var exporter: VExport

    private lateinit var playerView: androidx.media3.ui.PlayerView
    private lateinit var timelineView: VTimeline
    private lateinit var btnPlayPause: ImageButton
    private lateinit var tvDuration: TextView
    private lateinit var tvClipInfo: TextView
    private lateinit var tvClipCount: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnExport: Button
    private lateinit var btnSplit: Button
    private lateinit var btnDelete: Button
    private lateinit var btnSpeed: Button
    private lateinit var btnVolume: Button
    private lateinit var btnMute: Button
    private lateinit var btnAddClip: Button
    private lateinit var btnMoveLeft: Button
    private lateinit var btnMoveRight: Button
    private lateinit var filterButtonsContainer: android.widget.HorizontalScrollView
    private lateinit var effectButtonsContainer: android.widget.HorizontalScrollView
    private lateinit var animationButtonsContainer: android.widget.HorizontalScrollView
    private lateinit var videoEffectButtonsContainer: android.widget.HorizontalScrollView

    private val handler = Handler(Looper.getMainLooper())
    private var isMuted = false

    private val pickVideos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        val durations = mutableMapOf<Uri, Long>()
        for (uri in uris) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                val dur = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                durations[uri] = dur
            } catch (e: Exception) {
                durations[uri] = 0L
            } finally {
                retriever.release()
            }
        }
        viewModel.addClips(uris, durations)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        playback = VPlayback(this)
        exporter = VExport(this)

        bindViews()
        setupButtons()
        setupTimeline()
        observeViewModel()
        startProgressUpdater()

        playerView.player = playback.getPlayer()
    }

    private fun bindViews() {
        playerView = findViewById(R.id.playerView)
        timelineView = findViewById(R.id.timelineView)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        tvDuration = findViewById(R.id.tvDuration)
        tvClipInfo = findViewById(R.id.tvClipInfo)
        tvClipCount = findViewById(R.id.tvClipCount)
        btnBack = findViewById(R.id.btnBack)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        btnExport = findViewById(R.id.btnExport)
        btnSplit = findViewById(R.id.btnSplit)
        btnDelete = findViewById(R.id.btnDelete)
        btnSpeed = findViewById(R.id.btnSpeed)
        btnVolume = findViewById(R.id.btnVolume)
        btnMute = findViewById(R.id.btnMute)
        btnAddClip = findViewById(R.id.btnAddClip)
        btnMoveLeft = findViewById(R.id.btnMoveLeft)
        btnMoveRight = findViewById(R.id.btnMoveRight)
        filterButtonsContainer = findViewById(R.id.filterButtonsContainer)
        effectButtonsContainer = findViewById(R.id.effectButtonsContainer)
        animationButtonsContainer = findViewById(R.id.animationButtonsContainer)
        videoEffectButtonsContainer = findViewById(R.id.videoEffectButtonsContainer)
    }

    private fun setupButtons() {
        btnBack.setOnClickListener { finish() }

        btnAddClip.setOnClickListener { pickVideos.launch("video/*") }

        btnPlayPause.setOnClickListener {
            if (playback.isPlaying()) {
                playback.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                playback.play()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        btnUndo.setOnClickListener {
            if (viewModel.canUndo()) viewModel.undo()
            else Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show()
        }

        btnRedo.setOnClickListener {
            if (viewModel.canRedo()) viewModel.redo()
            else Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show()
        }

        btnSplit.setOnClickListener {
            val selId = viewModel.selectedClipId.value
            if (selId == null) { Toast.makeText(this, "Select a clip first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val project = viewModel.project.value ?: return@setOnClickListener
            val clip = project.clips.find { it.id == selId } ?: return@setOnClickListener
            val clipOffset = project.clipStartOffsetMs(selId)
            val posInClip = playback.currentPositionMs() - clipOffset
            if (posInClip <= 0 || posInClip >= clip.trimmedDurationMs) {
                Toast.makeText(this, "Move playhead inside clip to split", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.splitClip(selId, posInClip)
        }

        btnDelete.setOnClickListener {
            val selId = viewModel.selectedClipId.value
            if (selId == null) { Toast.makeText(this, "Select a clip first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            viewModel.deleteClip(selId)
        }

        btnMoveLeft.setOnClickListener {
            val selId = viewModel.selectedClipId.value ?: run {
                Toast.makeText(this, "Select a clip first", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            viewModel.moveClip(selId, -1)
        }

        btnMoveRight.setOnClickListener {
            val selId = viewModel.selectedClipId.value ?: run {
                Toast.makeText(this, "Select a clip first", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            viewModel.moveClip(selId, 1)
        }

        btnSpeed.setOnClickListener {
            val selId = viewModel.selectedClipId.value ?: run {
                Toast.makeText(this, "Select a clip first", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val speeds = arrayOf("0.25x", "0.5x", "1x", "1.5x", "2x")
            val vals = floatArrayOf(0.25f, 0.5f, 1f, 1.5f, 2f)
            AlertDialog.Builder(this)
                .setTitle("Select Speed")
                .setItems(speeds) { _, i ->
                    viewModel.setSpeed(selId, vals[i])
                    playback.getPlayer().playbackParameters = PlaybackParameters(vals[i])
                }
                .show()
        }

        btnVolume.setOnClickListener {
            val selId = viewModel.selectedClipId.value ?: run {
                Toast.makeText(this, "Select a clip first", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val vols = arrayOf("25%", "50%", "75%", "100%")
            val vals = floatArrayOf(0.25f, 0.5f, 0.75f, 1f)
            AlertDialog.Builder(this)
                .setTitle("Set Volume")
                .setItems(vols) { _, i ->
                    viewModel.setVolume(selId, vals[i])
                    playback.setVolume(vals[i])
                }
                .show()
        }

        btnMute.setOnClickListener {
            isMuted = !isMuted
            playback.setVolume(if (isMuted) 0f else 1f)
            btnMute.text = if (isMuted) "🔇 Muted" else "🔇 Mute"
        }

        btnExport.setOnClickListener {
            val project = viewModel.project.value ?: return@setOnClickListener
            if (project.clips.isEmpty()) {
                Toast.makeText(this, "Add clips first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val pd = ProgressDialog(this).apply {
                setTitle("Exporting...")
                setMessage("0%")
                setCancelable(false)
                show()
            }
            exporter.export(project, object : VExport.ExportCallback {
                override fun onProgress(percent: Int) {
                    runOnUiThread { pd.setMessage("$percent%") }
                }
                override fun onSuccess(uri: Uri) {
                    runOnUiThread {
                        pd.dismiss()
                        Toast.makeText(this@EditorActivity, "✅ Exported to gallery!", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(error: String) {
                    runOnUiThread {
                        pd.dismiss()
                        Toast.makeText(this@EditorActivity, "❌ Export failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
            })
        }
    }

    private fun setupTimeline() {
        timelineView.listener = object : VTimeline.Listener {
            override fun onClipSelected(clipId: String) {
                viewModel.selectClip(clipId)
            }
            override fun onSeek(positionMs: Long) {
                playback.seekTo(positionMs)
            }
            override fun onTrim(clipId: String, newStartMs: Long, newEndMs: Long) {
                viewModel.trimClip(clipId, newStartMs, newEndMs)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.project.observe(this) { project ->
            playback.load(project)
            timelineView.setProject(project)
            tvClipCount.text = "Clips: ${project.clips.size}"
            updateDuration(project)
        }

        viewModel.selectedClipId.observe(this) { id ->
            timelineView.setSelectedClipId(id)
            val clip = viewModel.project.value?.clips?.find { it.id == id }
            tvClipInfo.text = if (clip != null)
                "Clip ${clip.order + 1} | ${clip.trimmedDurationMs / 1000}s | Vol:${clip.volume} | ${clip.speed}x"
            else "No clip selected"
        }
    }

    private fun updateDuration(project: VProject) {
        val total = project.totalDurationMs
        val cur = playback.currentPositionMs()
        tvDuration.text = "${formatMs(cur)} / ${formatMs(total)}"
    }

    private fun startProgressUpdater() {
        handler.post(object : Runnable {
            override fun run() {
                val pos = playback.currentPositionMs()
                timelineView.setPlayheadMs(pos)
                val project = viewModel.project.value ?: VProject()
                tvDuration.text = "${formatMs(pos)} / ${formatMs(project.totalDurationMs)}"
                handler.postDelayed(this, 200)
            }
        })
    }

    private fun formatMs(ms: Long): String {
        val s = ms / 1000
        return "%02d:%02d".format(s / 60, s % 60)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        playback.release()
    }
}
