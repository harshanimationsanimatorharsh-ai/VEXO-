package com.vexo.app.ui

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.vexo.app.R
import com.vexo.app.utils.EffectUtils
import com.vexo.app.utils.FilterUtils
import java.io.File
import java.util.UUID

// ══════════════════════════════════════════
// CLIP MODEL
// ══════════════════════════════════════════
class VClip(
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

    fun cloneWith(
        newId: String = this.id,
        newStart: Long = this.trimStartMs,
        newEnd: Long = this.trimEndMs,
        newVol: Float = this.volume,
        newSpeed: Float = this.speed
    ): VClip {
        return VClip(
            id = newId,
            uri = this.uri,
            sourceDurationMs = this.sourceDurationMs,
            trimStartMs = newStart,
            trimEndMs = newEnd,
            volume = newVol,
            speed = newSpeed,
            order = this.order
        )
    }
}

// ══════════════════════════════════════════
// PROJECT STATE
// ══════════════════════════════════════════
class VProject(
    val clips: List<VClip> = emptyList()
) {
    val totalDurationMs: Long
        get() {
            var t = 0L
            for (c in clips) t += c.trimmedDurationMs
            return t
        }

    fun withClips(newClips: List<VClip>): VProject {
        for (i in newClips.indices) newClips[i].order = i
        return VProject(newClips)
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

// ══════════════════════════════════════════
// UNDO / REDO
// ══════════════════════════════════════════
class VUndoRedo {
    private val undoStack = ArrayDeque<VProject>()
    private val redoStack = ArrayDeque<VProject>()

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    fun push(p: VProject) {
        undoStack.addLast(p)
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(cur: VProject): VProject? {
        if (!canUndo) return null
        val prev = undoStack.removeLast()
        redoStack.addLast(cur)
        return prev
    }

    fun redo(cur: VProject): VProject? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(cur)
        return next
    }
}

// ══════════════════════════════════════════
// VIEW MODEL
// ══════════════════════════════════════════
class VEditorViewModel : ViewModel() {

    private val ur = VUndoRedo()
    private val _proj = MutableLiveData<VProject>(VProject())
    val project: LiveData<VProject> = _proj

    private val _selId = MutableLiveData<String?>(null)
    val selectedClipId: LiveData<String?> = _selId

    val canUndo get() = ur.canUndo
    val canRedo get() = ur.canRedo

    private fun cur() = _proj.value ?: VProject()

    private fun commit(p: VProject) {
        ur.push(cur())
        _proj.value = p
    }

    fun addClips(uris: List<Uri>, durations: Map<Uri, Long>) {
        val list = cur().clips.toMutableList()
        for (uri in uris) {
            val dur = durations[uri] ?: 0L
            if (dur > 0L) list.add(VClip(uri = uri, sourceDurationMs = dur))
        }
        commit(cur().withClips(list))
    }

    fun selectClip(id: String?) { _selId.value = id }

    fun trimClip(clipId: String, ns: Long, ne: Long) {
        val list = ArrayList<VClip>()
        for (c in cur().clips) {
            if (c.id == clipId) {
                val s = ns.coerceIn(0L, c.sourceDurationMs)
                val e = ne.coerceIn(s + 100L, c.sourceDurationMs)
                list.add(c.cloneWith(newStart = s, newEnd = e))
            } else list.add(c)
        }
        commit(cur().withClips(list))
    }

    fun splitClip(clipId: String, splitAtMs: Long) {
        val list = cur().clips.toMutableList()
        var idx = -1
        for (i in list.indices) { if (list[i].id == clipId) { idx = i; break } }
        if (idx < 0) return
        val clip = list[idx]
        val abs = clip.trimStartMs + splitAtMs
        if (abs <= clip.trimStartMs || abs >= clip.trimEndMs) return
        val left  = clip.cloneWith(newEnd = abs)
        val right = clip.cloneWith(newId = UUID.randomUUID().toString(), newStart = abs)
        list.removeAt(idx)
        list.add(idx, left)
        list.add(idx + 1, right)
        commit(cur().withClips(list))
    }

    fun deleteClip(clipId: String) {
        val list = cur().clips.filter { it.id != clipId }
        commit(cur().withClips(list))
        if (_selId.value == clipId) _selId.value = null
    }

    fun moveClip(from: Int, to: Int) {
        val list = cur().clips.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        commit(cur().withClips(list))
    }

    fun setVolume(clipId: String, vol: Float) {
        val list = ArrayList<VClip>()
        for (c in cur().clips) list.add(if (c.id == clipId) c.cloneWith(newVol = vol) else c)
        commit(cur().withClips(list))
    }

    fun undo(): Boolean {
        val prev = ur.undo(cur()) ?: return false
        _proj.value = prev; return true
    }

    fun redo(): Boolean {
        val next = ur.redo(cur()) ?: return false
        _proj.value = next; return true
    }
}

// ══════════════════════════════════════════
// PLAYBACK
// ══════════════════════════════════════════
class VPlayback(private val ctx: Context) {

    var player: ExoPlayer? = null
        private set

    fun init(pv: PlayerView) {
        release()
        val p = ExoPlayer.Builder(ctx).build()
        pv.player = p
        p.playWhenReady = false
        player = p
    }

    fun load(proj: VProject, play: Boolean = false) {
        val p = player ?: return
        p.stop(); p.clearMediaItems()
        for (clip in proj.clips) {
            p.addMediaItem(
                MediaItem.Builder().setUri(clip.uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.trimStartMs)
                            .setEndPositionMs(clip.trimEndMs)
                            .build()
                    ).build()
            )
        }
        p.prepare(); p.playWhenReady = play
    }

    fun play()  { player?.play() }
    fun pause() { player?.pause() }

    fun seekTo(posMs: Long, proj: VProject) {
        val p = player ?: return
        var offset = 0L
        for (i in proj.clips.indices) {
            val clip = proj.clips[i]
            val end = offset + clip.trimmedDurationMs
            if (posMs <= end) {
                val diff = (posMs - offset).coerceIn(0L, clip.trimmedDurationMs)
                p.seekTo(i, clip.trimStartMs + diff); return
            }
            offset = end
        }
        if (proj.clips.isNotEmpty()) {
            val last = proj.clips.last()
            p.seekTo(proj.clips.size - 1, last.trimEndMs)
        }
    }

    fun posMs(proj: VProject): Long {
        val p = player ?: return 0L
        var offset = 0L
        val idx = p.currentMediaItemIndex
        for (i in proj.clips.indices) {
            val clip = proj.clips[i]
            if (i == idx) return offset + (p.currentPosition - clip.trimStartMs).coerceAtLeast(0L)
            offset += clip.trimmedDurationMs
        }
        return offset
    }

    fun addListener(l: Player.Listener) { player?.addListener(l) }

    fun release() { player?.release(); player = null }
}

// ══════════════════════════════════════════
// EXPORT
// ══════════════════════════════════════════
class VExport(private val ctx: Context) {

    interface Cb {
        fun onProgress(pct: Int)
        fun onSuccess(path: String)
        fun onFailure(err: String)
    }

    private var transformer: Transformer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun start(proj: VProject, cb: Cb) {
        if (proj.clips.isEmpty()) { cb.onFailure("No clips"); return }
        val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(ctx.filesDir, "Movies")
        if (!dir.exists()) dir.mkdirs()
        val out = File(dir, "VEXO_${System.currentTimeMillis()}.mp4")

        val items = ArrayList<EditedMediaItem>()
        for (clip in proj.clips) {
            val mi = MediaItem.Builder().setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                ).build()
            items.add(EditedMediaItem.Builder(mi).setRemoveAudio(clip.volume == 0f).build())
        }

        val seq  = EditedMediaItemSequence(items)
        val comp = Composition.Builder(listOf(seq)).build()

        transformer = Transformer.Builder(ctx)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(c: Composition, r: ExportResult) {
                    save(out); handler.post { cb.onSuccess(out.absolutePath) }
                }
                override fun onError(c: Composition, r: ExportResult, ex: ExportException) {
                    handler.post { cb.onFailure(ex.message ?: "Error") }
                }
            }).build()

        transformer?.start(comp, out.absolutePath)
        poll(cb)
    }

    private fun poll(cb: Cb) {
        val t = transformer ?: return
        val ph = ProgressHolder()
        val s  = t.getProgress(ph)
        if (s == Transformer.PROGRESS_STATE_AVAILABLE) cb.onProgress(ph.progress)
        if (s != Transformer.PROGRESS_STATE_NOT_STARTED) handler.postDelayed({ poll(cb) }, 300)
    }

    private fun save(file: File) {
        try {
            val cv = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VEXO")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv)
            uri?.let { dest ->
                ctx.contentResolver.openOutputStream(dest)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun cancel() { transformer?.cancel(); transformer = null }
}

// ══════════════════════════════════════════
// TIMELINE VIEW
// ══════════════════════════════════════════
class VTimeline @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onClipSelected(id: String)
        fun onSeek(posMs: Long)
        fun onTrim(id: String, start: Long, end: Long)
    }

    var listener: Listener? = null
    private var clips: List<VClip> = emptyList()
    private var totalMs = 1L
    private var headMs = 0L
    private var selId: String? = null

    private fun ppm() = if (width > 0 && totalMs > 0) width.toFloat() / totalMs.toFloat() else 1f

    private val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.WHITE }
    private val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED; strokeWidth = 3f }
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 26f }
    private val hndP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#BB000000") }

    private val colors = listOf(
        Color.parseColor("#6C63FF"), Color.parseColor("#43E97B"),
        Color.parseColor("#FF6584"), Color.parseColor("#FFD700"),
        Color.parseColor("#00B4D8")
    )

    private enum class DM { NONE, HEAD, LEFT, RIGHT }
    private var dm = DM.NONE
    private var dId: String? = null

    fun set(list: List<VClip>, total: Long, ph: Long, sel: String?) {
        clips = list; totalMs = if (total > 0) total else 1L
        headMs = ph; selId = sel; invalidate()
    }

    fun setHead(ms: Long) { headMs = ms; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val p = ppm()
        val h = height.toFloat(); val ch = h * 0.65f; val ct = (h - ch) / 2f
        var ox = 0f

        for (i in clips.indices) {
            val c = clips[i]
            val cw = c.trimmedDurationMs.toFloat() * p
            if (cw < 1f) { ox += cw; continue }
            cp.color = colors[i % colors.size]
            val r = RectF(ox, ct, ox + cw, ct + ch)
            canvas.drawRoundRect(r, 10f, 10f, cp)
            if (c.id == selId) {
                canvas.drawRoundRect(r, 10f, 10f, sp)
                canvas.drawRoundRect(RectF(ox, ct, ox + 18f, ct + ch), 6f, 6f, hndP)
                canvas.drawRoundRect(RectF(ox + cw - 18f, ct, ox + cw, ct + ch), 6f, 6f, hndP)
            }
            val lbl = "Clip ${i + 1}"
            val tw = tp.measureText(lbl)
            if (tw < cw - 8f) canvas.drawText(lbl, ox + (cw - tw) / 2f, ct + ch / 2f + 10f, tp)
            ox += cw
        }

        val px = headMs.toFloat() * p
        canvas.drawLine(px, 0f, px, h, hp)
        val tri = Path(); tri.moveTo(px - 12f, 0f); tri.lineTo(px + 12f, 0f); tri.lineTo(px, 24f); tri.close()
        canvas.drawPath(tri, hp)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x; val p = ppm()
        val ms = (x / p).toLong().coerceIn(0L, totalMs)
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                dm = DM.NONE
                val sel = selId
                if (sel != null) {
                    var off = 0L; var sc: VClip? = null
                    for (c in clips) { if (c.id == sel) { sc = c; break }; off += c.trimmedDurationMs }
                    if (sc != null) {
                        val sP = off.toFloat() * p; val eP = sP + sc.trimmedDurationMs.toFloat() * p
                        if (x >= sP && x <= sP + 30f) { dm = DM.LEFT; dId = sel; return true }
                        if (x >= eP - 30f && x <= eP) { dm = DM.RIGHT; dId = sel; return true }
                    }
                }
                val px = headMs.toFloat() * p
                if (Math.abs(x - px) < 40f) { dm = DM.HEAD; return true }
                var off = 0L
                for (c in clips) {
                    val end = off + c.trimmedDurationMs
                    if (ms in off..end) { listener?.onClipSelected(c.id); return true }
                    off = end
                }
            }
            MotionEvent.ACTION_MOVE -> when (dm) {
                DM.HEAD  -> { headMs = ms; listener?.onSeek(ms); invalidate() }
                DM.LEFT  -> {
                    val cid = dId ?: return false; var off = 0L; var clip: VClip? = null
                    for (c in clips) { if (c.id == cid) { clip = c; break }; off += c.trimmedDurationMs }
                    clip ?: return false
                    listener?.onTrim(cid, (clip.trimStartMs + (ms - off)).coerceIn(0L, clip.trimEndMs - 500L), clip.trimEndMs)
                }
                DM.RIGHT -> {
                    val cid = dId ?: return false; var off = 0L; var clip: VClip? = null
                    for (c in clips) { if (c.id == cid) { clip = c; break }; off += c.trimmedDurationMs }
                    clip ?: return false
                    listener?.onTrim(cid, clip.trimStartMs, (clip.trimStartMs + (ms - off)).coerceIn(clip.trimStartMs + 500L, clip.sourceDurationMs))
                }
                else -> {}
            }
            MotionEvent.ACTION_UP -> { dm = DM.NONE; dId = null }
        }
        return true
    }
}

// ══════════════════════════════════════════
// EDITOR ACTIVITY
// ══════════════════════════════════════════
class EditorActivity : AppCompatActivity() {

    private val vm: VEditorViewModel by viewModels()
    private lateinit var pb: VPlayback
    private lateinit var ex: VExport

    private lateinit var playerView: PlayerView
    private lateinit var timeline: VTimeline
    private lateinit var btnPlay: ImageButton
    private lateinit var tvDur: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvCount: TextView
    private lateinit var overlay: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val p = vm.project.value ?: return
            val pos = pb.posMs(p)
            timeline.setHead(pos)
            val s = pos / 1000L; val tot = p.totalDurationMs / 1000L
            tvDur.text = "${s/60}:${String.format("%02d",s%60)} / ${tot/60}:${String.format("%02d",tot%60)}"
            handler.postDelayed(this, 100)
        }
    }

    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) {
            val uris = mutableListOf<Uri>()
            val d = r.data; val cd = d?.clipData
            if (cd != null) for (i in 0 until cd.itemCount) uris.add(cd.getItemAt(i).uri)
            else d?.data?.let { uris.add(it) }
            if (uris.isNotEmpty()) importUris(uris)
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_editor)
        pb = VPlayback(this); ex = VExport(this)

        playerView = findViewById(R.id.playerView)
        timeline   = findViewById(R.id.timelineView)
        btnPlay    = findViewById(R.id.btnPlayPause)
        tvDur      = findViewById(R.id.tvDuration)
        tvInfo     = findViewById(R.id.tvClipInfo)
        tvCount    = findViewById(R.id.tvClipCount)

        overlay = ImageView(this)
        overlay.setImageDrawable(ColorDrawable(Color.WHITE))
        overlay.alpha = 0f
        val flp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        overlay.layoutParams = flp
        (playerView.parent as? FrameLayout)?.addView(overlay)

        setupPlayback(); setupToolbar(); setupTools(); setupTimeline(); observe()
        setupFilters(); setupEffects(); setupAnims(); setupVFX()

        val uriStrings = intent.getStringArrayListExtra("media_uris") ?: arrayListOf()
        if (uriStrings.isNotEmpty()) importUris(uriStrings.map { Uri.parse(it) })
    }

    private fun setupPlayback() {
        pb.init(playerView)
        pb.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                if (playing) { btnPlay.setImageResource(android.R.drawable.ic_media_pause); handler.post(tick) }
                else { btnPlay.setImageResource(android.R.drawable.ic_media_play); handler.removeCallbacks(tick) }
            }
        })
        btnPlay.setOnClickListener { if (pb.player?.isPlaying == true) pb.pause() else pb.play() }
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnUndo).setOnClickListener {
            if (vm.undo()) { reload(); toast("Undone") } else toast("Nothing to undo")
        }
        findViewById<ImageButton>(R.id.btnRedo).setOnClickListener {
            if (vm.redo()) { reload(); toast("Redone") } else toast("Nothing to redo")
        }
        findViewById<Button>(R.id.btnExport).setOnClickListener { doExport() }
    }

    private fun setupTools() {
        findViewById<Button>(R.id.btnSplit).setOnClickListener {
            val p = vm.project.value ?: return@setOnClickListener
            val sid = vm.selectedClipId.value ?: run { toast("Select clip first"); return@setOnClickListener }
            val pos = pb.posMs(p); val inClip = pos - p.clipStartOffsetMs(sid)
            if (inClip <= 0L) { toast("Move playhead inside clip"); return@setOnClickListener }
            vm.splitClip(sid, inClip); reload(); toast("Split!")
        }
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            val sid = vm.selectedClipId.value ?: run { toast("Select clip first"); return@setOnClickListener }
            vm.deleteClip(sid); reload(); toast("Deleted")
        }
        val speeds = floatArrayOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f); var si = 2
        findViewById<Button>(R.id.btnSpeed).setOnClickListener {
            si = (si + 1) % speeds.size; pb.player?.setPlaybackSpeed(speeds[si]); toast("Speed: ${speeds[si]}x")
        }
        var vol = 1f
        findViewById<Button>(R.id.btnVolume).setOnClickListener {
            vol = if (vol >= 1f) 0.3f else 1f; pb.player?.volume = vol; toast("Volume: ${(vol*100).toInt()}%")
        }
        findViewById<Button>(R.id.btnMute).setOnClickListener {
            val sid = vm.selectedClipId.value ?: run { toast("Select clip first"); return@setOnClickListener }
            val clip = vm.project.value?.clips?.firstOrNull { it.id == sid }
            val nv = if ((clip?.volume ?: 1f) > 0f) 0f else 1f
            vm.setVolume(sid, nv); reload(); toast(if (nv == 0f) "Muted" else "Unmuted")
        }
        findViewById<Button>(R.id.btnAddClip).setOnClickListener {
            picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "video/*"; addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            })
        }
        findViewById<Button>(R.id.btnMoveLeft).setOnClickListener {
            val sid = vm.selectedClipId.value ?: run { toast("Select clip first"); return@setOnClickListener }
            val clips = vm.project.value?.clips ?: return@setOnClickListener
            var idx = -1; for (i in clips.indices) { if (clips[i].id == sid) { idx = i; break } }
            if (idx <= 0) { toast("Already first"); return@setOnClickListener }
            vm.moveClip(idx, idx - 1); reload(); toast("Moved left")
        }
        findViewById<Button>(R.id.btnMoveRight).setOnClickListener {
            val sid = vm.selectedClipId.value ?: run { toast("Select clip first"); return@setOnClickListener }
            val clips = vm.project.value?.clips ?: return@setOnClickListener
            var idx = -1; for (i in clips.indices) { if (clips[i].id == sid) { idx = i; break } }
            if (idx < 0 || idx >= clips.size - 1) { toast("Already last"); return@setOnClickListener }
            vm.moveClip(idx, idx + 1); reload(); toast("Moved right")
        }
    }

    private fun setupTimeline() {
        timeline.listener = object : VTimeline.Listener {
            override fun onClipSelected(id: String) { vm.selectClip(id) }
            override fun onSeek(posMs: Long) { val p = vm.project.value ?: return; pb.seekTo(posMs, p) }
            override fun onTrim(id: String, start: Long, end: Long) { vm.trimClip(id, start, end); reload(false) }
        }
    }

    private fun observe() {
        vm.project.observe(this) { p ->
            val sel = vm.selectedClipId.value; val pos = pb.posMs(p)
            timeline.set(p.clips, p.totalDurationMs, pos, sel)
            tvCount.text = "${p.clips.size} clips"
        }
        vm.selectedClipId.observe(this) { sel ->
            val p = vm.project.value ?: return@observe; val pos = pb.posMs(p)
            timeline.set(p.clips, p.totalDurationMs, pos, sel)
            val clip = p.clips.firstOrNull { it.id == sel }
            tvInfo.text = if (clip != null) {
                val d = clip.trimmedDurationMs / 1000f
                "Selected • ${String.format("%.1f",d)}s • Vol:${(clip.volume*100).toInt()}%"
            } else "Tap a clip to select"
        }
    }

    private fun importUris(uris: List<Uri>) {
        for (uri in uris) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (e: Exception) { /* ignore */ }
        }
        val durs = HashMap<Uri, Long>()
        for (uri in uris) {
            try {
                val ret = MediaMetadataRetriever(); ret.setDataSource(this, uri)
                durs[uri] = ret.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10000L
                ret.release()
            } catch (e: Exception) { durs[uri] = 10000L }
        }
        vm.addClips(uris, durs); reload()
    }

    private fun reload(play: Boolean = false) {
        val p = vm.project.value ?: return; pb.load(p, play)
    }

    private fun doExport() {
        val p = vm.project.value ?: return
        if (p.clips.isEmpty()) { toast("No clips"); return }
        pb.pause()
        val dlg = AlertDialog.Builder(this).setTitle("Exporting…").setMessage("0%")
            .setCancelable(false).setNegativeButton("Cancel") { _, _ -> ex.cancel() }.create()
        dlg.show()
        ex.start(p, object : VExport.Cb {
            override fun onProgress(pct: Int) { runOnUiThread { dlg.setMessage("$pct%") } }
            override fun onSuccess(path: String) {
                runOnUiThread { dlg.dismiss()
                    AlertDialog.Builder(this@EditorActivity).setTitle("✅ Done!")
                        .setMessage("Saved to gallery!").setPositiveButton("OK", null).show() }
            }
            override fun onFailure(err: String) { runOnUiThread { dlg.dismiss(); toast("Failed: $err") } }
        })
    }

    private fun setupFilters() {
        val c = findViewById<LinearLayout>(R.id.filterButtonsContainer)
        val list = listOf("None" to null, "Cinematic" to FilterUtils.cinematic(),
            "HDR" to FilterUtils.hdr(), "Aesthetic" to FilterUtils.aesthetic(),
            "Warm Glow" to FilterUtils.warmGlow(), "Cool Tone" to FilterUtils.coolTone(),
            "Vintage" to FilterUtils.vintageFilm(), "Retro" to FilterUtils.retro(),
            "Y2K" to FilterUtils.y2k(), "VHS" to FilterUtils.vhs(),
            "B&W Noir" to FilterUtils.bwNoir(), "Glamour" to FilterUtils.glamour(),
            "Night" to FilterUtils.nightScene(), "Movie" to FilterUtils.movie(),
            "Colorist" to FilterUtils.colorist(), "Neon" to FilterUtils.neon(),
            "Dreamy" to FilterUtils.dreamy(), "Dark Mood" to FilterUtils.darkMood(),
            "Faded Film" to FilterUtils.fadedFilm(), "Cartoon AI" to FilterUtils.cartoonAI(),
            "Barbie Pink" to FilterUtils.barbiePink())
        for (pr in list) c.addView(btn(pr.first) {
            if (pr.second == null) { overlay.colorFilter = null; overlay.alpha = 0f }
            else { overlay.colorFilter = ColorMatrixColorFilter(pr.second!!); overlay.alpha = 0.35f }
            toast("Filter: ${pr.first}")
        })
    }

    private fun setupEffects() {
        val c = findViewById<LinearLayout>(R.id.effectButtonsContainer)
        val list = listOf("Glow","Motion Blur","Zoom","3D Zoom","Shake","Flash","Glitch",
            "RGB Split","Chromatic","Lens Flare","Light Leak","Film Grain","Vignette",
            "Blur","Pixelate","Noise","Smoke","Fire","Spark","Aura")
        for (l in list) c.addView(btn(l) {
            when(l) {
                "Glow" -> EffectUtils.applyGlow(playerView)
                "Motion Blur" -> EffectUtils.applyMotionBlur(playerView)
                "Zoom" -> EffectUtils.applyZoom(playerView)
                "3D Zoom" -> EffectUtils.apply3DZoom(playerView)
                "Shake" -> EffectUtils.applyShake(playerView)
                "Flash" -> EffectUtils.applyFlash(playerView)
                "Glitch" -> EffectUtils.applyGlitch(playerView)
                "RGB Split" -> EffectUtils.applyRGBSplit(playerView)
                "Chromatic" -> EffectUtils.applyChromatic(playerView)
                "Lens Flare" -> EffectUtils.applyLensFlare(playerView)
                "Light Leak" -> EffectUtils.applyLightLeak(playerView)
                "Film Grain" -> EffectUtils.applyFilmGrain(playerView)
                "Vignette" -> EffectUtils.applyVignette(playerView)
                "Blur" -> EffectUtils.applyBlur(playerView)
                "Pixelate" -> EffectUtils.applyPixelate(playerView)
                "Noise" -> EffectUtils.applyNoise(playerView)
                "Smoke" -> EffectUtils.applySmoke(playerView)
                "Fire" -> EffectUtils.applyFire(playerView)
                "Spark" -> EffectUtils.applySpark(playerView)
                "Aura" -> EffectUtils.applyAura(playerView)
            }; toast("Effect: $l")
        })
    }

    private fun setupAnims() {
        val c = findViewById<LinearLayout>(R.id.animationButtonsContainer)
        val list = listOf("Fade In","Fade Out","Zoom In","Zoom Out","Pop Up","Bounce",
            "Slide Left","Slide Right","Slide Up","Slide Down","Spin","Swing","Shake",
            "Wobble","Pulse","Float","Typewriter","Elastic","Flip","3D Rotate")
        for (l in list) c.addView(btn(l) {
            when(l) {
                "Fade In" -> EffectUtils.animFadeIn(playerView)
                "Fade Out" -> EffectUtils.animFadeOut(playerView)
                "Zoom In" -> EffectUtils.animZoomIn(playerView)
                "Zoom Out" -> EffectUtils.animZoomOut(playerView)
                "Pop Up" -> EffectUtils.animPopUp(playerView)
                "Bounce" -> EffectUtils.animBounce(playerView)
                "Slide Left" -> EffectUtils.animSlideLeft(playerView)
                "Slide Right" -> EffectUtils.animSlideRight(playerView)
                "Slide Up" -> EffectUtils.animSlideUp(playerView)
                "Slide Down" -> EffectUtils.animSlideDown(playerView)
                "Spin" -> EffectUtils.animSpin(playerView)
                "Swing" -> EffectUtils.animSwing(playerView)
                "Shake" -> EffectUtils.animShake(playerView)
                "Wobble" -> EffectUtils.animWobble(playerView)
                "Pulse" -> EffectUtils.animPulse(playerView)
                "Float" -> EffectUtils.animFloat(playerView)
                "Typewriter" -> EffectUtils.animTypewriter(playerView)
                "Elastic" -> EffectUtils.animElastic(playerView)
                "Flip" -> EffectUtils.animFlip(playerView)
                "3D Rotate" -> EffectUtils.anim3DRotate(playerView)
            }; toast("Anim: $l")
        })
    }

    private fun setupVFX() {
        val c = findViewById<LinearLayout>(R.id.videoEffectButtonsContainer)
        val list = listOf("Velocity","Slow Mo","Speed Ramp","Beat Shake","Flash Beat",
            "Glitch Trans","RGB Glitch","Motion Trail","Cam Shake","Dyn. Zoom",
            "Spin Trans","Whip Pan","Light Sweep","Lens Flare","Film Burn",
            "Flashback","Freeze Frame","Echo Trail","Blur Trans","Particle Burst")
        for (l in list) c.addView(btn(l) {
            when(l) {
                "Velocity" -> EffectUtils.videoVelocity(playerView)
                "Slow Mo" -> EffectUtils.videoSlowMo(playerView)
                "Speed Ramp" -> EffectUtils.videoSpeedRamp(playerView)
                "Beat Shake" -> EffectUtils.videoBeatShake(playerView)
                "Flash Beat" -> EffectUtils.videoFlashBeat(playerView)
                "Glitch Trans" -> EffectUtils.videoGlitchTransition(playerView)
                "RGB Glitch" -> EffectUtils.videoRGBGlitch(playerView)
                "Motion Trail" -> EffectUtils.videoMotionTrail(playerView)
                "Cam Shake" -> EffectUtils.videoCameraShake(playerView)
                "Dyn. Zoom" -> EffectUtils.videoDynamicZoom(playerView)
                "Spin Trans" -> EffectUtils.videoSpinTransition(playerView)
                "Whip Pan" -> EffectUtils.videoWhipPan(playerView)
                "Light Sweep" -> EffectUtils.videoLightSweep(playerView)
                "Lens Flare" -> EffectUtils.videoLensFlare(playerView)
                "Film Burn" -> EffectUtils.videoFilmBurn(playerView)
                "Flashback" -> EffectUtils.videoFlashback(playerView)
                "Freeze Frame" -> EffectUtils.videoFreezeFrame(playerView)
                "Echo Trail" -> EffectUtils.videoEchoTrail(playerView)
                "Blur Trans" -> EffectUtils.videoBlurTransition(playerView)
                "Particle Burst" -> EffectUtils.videoParticleBurst(playerView)
            }; toast("VFX: $l")
        })
    }

    private fun btn(label: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label; b.isAllCaps = false; b.textSize = 11f
        b.minWidth = 0; b.minHeight = 0; b.setPadding(24, 10, 24, 10)
        b.setTextColor(Color.WHITE)
        b.background = ContextCompat.getDrawable(this, R.drawable.bg_button)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(6, 4, 6, 4); b.layoutParams = lp
        b.setOnClickListener { onClick() }; return b
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tick)
        pb.release()
    }
}
