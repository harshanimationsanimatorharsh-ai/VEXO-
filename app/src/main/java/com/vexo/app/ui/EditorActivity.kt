package com.vexo.app.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.vexo.app.R
import com.vexo.app.editor.EditorState
import com.vexo.app.editor.EditorViewModel
import com.vexo.app.editor.ExportController
import com.vexo.app.editor.PlaybackController
import com.vexo.app.editor.TimelineView
import com.vexo.app.utils.EffectUtils
import com.vexo.app.utils.FilterUtils

class EditorActivity : AppCompatActivity() {

    private val vm: EditorViewModel by viewModels()
    private lateinit var playback: PlaybackController
    private lateinit var exportCtrl: ExportController

    private lateinit var playerView: PlayerView
    private lateinit var timelineView: TimelineView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var tvDuration: TextView
    private lateinit var tvClipInfo: TextView
    private lateinit var tvClipCount: TextView
    private lateinit var filterOverlay: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            val st = vm.state.value ?: return
            val pos = playback.currentPositionMs(st)
            timelineView.updatePlayhead(pos)
            val s = pos / 1000L
            val total = st.totalDurationMs / 1000L
            tvDuration.text = "${s / 60}:${String.format("%02d", s % 60)} / ${total / 60}:${String.format("%02d", total % 60)}"
            handler.postDelayed(this, 100)
        }
    }

    private val mediaPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uris = mutableListOf<Uri>()
            val data = result.data
            val clip = data?.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
            } else {
                data?.data?.let { uris.add(it) }
            }
            if (uris.isNotEmpty()) importUris(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        playback   = PlaybackController(this)
        exportCtrl = ExportController(this)

        // Bind views
        playerView   = findViewById(R.id.playerView)
        timelineView = findViewById(R.id.timelineView)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        tvDuration   = findViewById(R.id.tvDuration)
        tvClipInfo   = findViewById(R.id.tvClipInfo)
        tvClipCount  = findViewById(R.id.tvClipCount)

        // Filter overlay
        filterOverlay = ImageView(this)
        filterOverlay.setImageDrawable(ColorDrawable(Color.WHITE))
        filterOverlay.alpha = 0f
        val flp = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        )
        filterOverlay.layoutParams = flp
        val parent = playerView.parent
        if (parent is android.widget.FrameLayout) parent.addView(filterOverlay)

        setupPlayback()
        setupToolbar()
        setupEditTools()
        setupTimeline()
        observeViewModel()
        setupFilterRow()
        setupEffectRow()
        setupAnimationRow()
        setupVideoEffectRow()

        // Import from MediaPicker
        val uriStrings = intent.getStringArrayListExtra("media_uris") ?: arrayListOf()
        if (uriStrings.isNotEmpty()) {
            importUris(uriStrings.map { Uri.parse(it) })
        }
    }

    private fun setupPlayback() {
        playback.init(playerView)
        playback.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                    handler.post(tickRunnable)
                } else {
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                    handler.removeCallbacks(tickRunnable)
                }
            }
        })
        btnPlayPause.setOnClickListener {
            if (playback.player?.isPlaying == true) playback.pause()
            else playback.play()
        }
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnUndo).setOnClickListener {
            if (vm.undo()) { reloadPlayback(); toast("Undone") } else toast("Nothing to undo")
        }
        findViewById<ImageButton>(R.id.btnRedo).setOnClickListener {
            if (vm.redo()) { reloadPlayback(); toast("Redone") } else toast("Nothing to redo")
        }
        findViewById<Button>(R.id.btnExport).setOnClickListener { startExport() }
    }

    private fun setupEditTools() {
        // Split
        findViewById<Button>(R.id.btnSplit).setOnClickListener {
            val state = vm.state.value ?: return@setOnClickListener
            val selId = vm.selectedClipId.value ?: run { toast("Select a clip first"); return@setOnClickListener }
            val posMs = playback.currentPositionMs(state)
            val clipStart = state.clipStartOffsetMs(selId)
            val posInClip = posMs - clipStart
            if (posInClip <= 0L) { toast("Move playhead inside clip"); return@setOnClickListener }
            vm.splitClip(selId, posInClip)
            reloadPlayback()
            toast("Split done!")
        }

        // Delete
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            val selId = vm.selectedClipId.value ?: run { toast("Select a clip first"); return@setOnClickListener }
            vm.deleteClip(selId)
            reloadPlayback()
            toast("Deleted")
        }

        // Speed
        val speeds = floatArrayOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f)
        var sIdx = 2
        findViewById<Button>(R.id.btnSpeed).setOnClickListener {
            sIdx = (sIdx + 1) % speeds.size
            playback.player?.setPlaybackSpeed(speeds[sIdx])
            toast("Speed: ${speeds[sIdx]}x")
        }

        // Volume
        var vol = 1f
        findViewById<Button>(R.id.btnVolume).setOnClickListener {
            vol = if (vol >= 1f) 0.3f else 1f
            playback.player?.volume = vol
            toast("Volume: ${(vol * 100).toInt()}%")
        }

        // Mute
        findViewById<Button>(R.id.btnMute).setOnClickListener {
            val selId = vm.selectedClipId.value ?: run { toast("Select a clip first"); return@setOnClickListener }
            val clip = vm.state.value?.clips?.firstOrNull { it.id == selId }
            val newVol = if ((clip?.volume ?: 1f) > 0f) 0f else 1f
            vm.setVolume(selId, newVol)
            reloadPlayback()
            toast(if (newVol == 0f) "Muted" else "Unmuted")
        }

        // Add clip
        findViewById<Button>(R.id.btnAddClip).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            mediaPicker.launch(intent)
        }

        // Move left
        findViewById<Button>(R.id.btnMoveLeft).setOnClickListener {
            val selId = vm.selectedClipId.value ?: run { toast("Select a clip first"); return@setOnClickListener }
            val clips = vm.state.value?.clips ?: return@setOnClickListener
            var idx = -1
            for (i in clips.indices) { if (clips[i].id == selId) { idx = i; break } }
            if (idx <= 0) { toast("Already first"); return@setOnClickListener }
            vm.moveClip(idx, idx - 1)
            reloadPlayback()
            toast("Moved left")
        }

        // Move right
        findViewById<Button>(R.id.btnMoveRight).setOnClickListener {
            val selId = vm.selectedClipId.value ?: run { toast("Select a clip first"); return@setOnClickListener }
            val clips = vm.state.value?.clips ?: return@setOnClickListener
            var idx = -1
            for (i in clips.indices) { if (clips[i].id == selId) { idx = i; break } }
            if (idx < 0 || idx >= clips.size - 1) { toast("Already last"); return@setOnClickListener }
            vm.moveClip(idx, idx + 1)
            reloadPlayback()
            toast("Moved right")
        }
    }

    private fun setupTimeline() {
        timelineView.listener = object : TimelineView.Listener {
            override fun onClipSelected(clipId: String) { vm.selectClip(clipId) }
            override fun onPlayheadSeeked(positionMs: Long) {
                val state = vm.state.value ?: return
                playback.seekToMs(positionMs, state)
            }
            override fun onTrimChanged(clipId: String, newStartMs: Long, newEndMs: Long) {
                vm.trimClip(clipId, newStartMs, newEndMs)
                reloadPlayback(false)
            }
        }
    }

    private fun observeViewModel() {
        vm.state.observe(this) { state ->
            val selId = vm.selectedClipId.value
            val pos = playback.currentPositionMs(state)
            timelineView.setClips(state.clips, state.totalDurationMs, pos, selId)
            tvClipCount.text = "${state.clips.size} clips"
        }
        vm.selectedClipId.observe(this) { selId ->
            val state = vm.state.value ?: return@observe
            val pos = playback.currentPositionMs(state)
            timelineView.setClips(state.clips, state.totalDurationMs, pos, selId)
            val clip = state.clips.firstOrNull { it.id == selId }
            tvClipInfo.text = if (clip != null) {
                val dur = clip.trimmedDurationMs / 1000f
                "Selected • ${String.format("%.1f", dur)}s • Vol:${(clip.volume * 100).toInt()}%"
            } else "Tap a clip to select"
        }
    }

    private fun importUris(uris: List<Uri>) {
        for (uri in uris) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { /* ignore */ }
        }
        val durations = HashMap<Uri, Long>()
        for (uri in uris) {
            try {
                val ret = MediaMetadataRetriever()
                ret.setDataSource(this, uri)
                durations[uri] = ret.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 10000L
                ret.release()
            } catch (e: Exception) { durations[uri] = 10000L }
        }
        vm.addClips(uris, durations)
        reloadPlayback()
    }

    private fun reloadPlayback(playWhenReady: Boolean = false) {
        val state = vm.state.value ?: return
        playback.loadState(state, playWhenReady)
    }

    private fun startExport() {
        val state = vm.state.value ?: return
        if (state.clips.isEmpty()) { toast("No clips to export"); return }
        playback.pause()
        val dialog = AlertDialog.Builder(this)
            .setTitle("Exporting…")
            .setMessage("0%")
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ -> exportCtrl.cancel() }
            .create()
        dialog.show()
        exportCtrl.export(state, object : ExportController.ExportCallback {
            override fun onProgress(percent: Int) { runOnUiThread { dialog.setMessage("$percent%") } }
            override fun onSuccess(filePath: String) {
                runOnUiThread {
                    dialog.dismiss()
                    AlertDialog.Builder(this@EditorActivity)
                        .setTitle("✅ Export Complete!")
                        .setMessage("Video saved to gallery!")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            override fun onFailure(error: String) {
                runOnUiThread { dialog.dismiss(); toast("Export failed: $error") }
            }
        })
    }

    private fun setupFilterRow() {
        val c = findViewById<LinearLayout>(R.id.filterButtonsContainer)
        val list = listOf(
            "None" to null, "Cinematic" to FilterUtils.cinematic(),
            "HDR" to FilterUtils.hdr(), "Aesthetic" to FilterUtils.aesthetic(),
            "Warm Glow" to FilterUtils.warmGlow(), "Cool Tone" to FilterUtils.coolTone(),
            "Vintage" to FilterUtils.vintageFilm(), "Retro" to FilterUtils.retro(),
            "Y2K" to FilterUtils.y2k(), "VHS" to FilterUtils.vhs(),
            "B&W Noir" to FilterUtils.bwNoir(), "Glamour" to FilterUtils.glamour(),
            "Night" to FilterUtils.nightScene(), "Movie" to FilterUtils.movie(),
            "Colorist" to FilterUtils.colorist(), "Neon" to FilterUtils.neon(),
            "Dreamy" to FilterUtils.dreamy(), "Dark Mood" to FilterUtils.darkMood(),
            "Faded Film" to FilterUtils.fadedFilm(), "Cartoon AI" to FilterUtils.cartoonAI(),
            "Barbie Pink" to FilterUtils.barbiePink()
        )
        for (pair in list) {
            c.addView(makeBtn(pair.first) {
                val m = pair.second
                if (m == null) { filterOverlay.colorFilter = null; filterOverlay.alpha = 0f }
                else { filterOverlay.colorFilter = ColorMatrixColorFilter(m); filterOverlay.alpha = 0.35f }
                toast("Filter: ${pair.first}")
            })
        }
    }

    private fun setupEffectRow() {
        val c = findViewById<LinearLayout>(R.id.effectButtonsContainer)
        val list = listOf("Glow","Motion Blur","Zoom","3D Zoom","Shake","Flash","Glitch",
            "RGB Split","Chromatic","Lens Flare","Light Leak","Film Grain","Vignette",
            "Blur","Pixelate","Noise","Smoke","Fire","Spark","Aura")
        for (l in list) c.addView(makeBtn(l) {
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
            }
            toast("Effect: $l")
        })
    }

    private fun setupAnimationRow() {
        val c = findViewById<LinearLayout>(R.id.animationButtonsContainer)
        val list = listOf("Fade In","Fade Out","Zoom In","Zoom Out","Pop Up","Bounce",
            "Slide Left","Slide Right","Slide Up","Slide Down","Spin","Swing","Shake",
            "Wobble","Pulse","Float","Typewriter","Elastic","Flip","3D Rotate")
        for (l in list) c.addView(makeBtn(l) {
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
            }
            toast("Anim: $l")
        })
    }

    private fun setupVideoEffectRow() {
        val c = findViewById<LinearLayout>(R.id.videoEffectButtonsContainer)
        val list = listOf("Velocity","Slow Mo","Speed Ramp","Beat Shake","Flash Beat",
            "Glitch Trans","RGB Glitch","Motion Trail","Cam Shake","Dyn. Zoom",
            "Spin Trans","Whip Pan","Light Sweep","Lens Flare","Film Burn",
            "Flashback","Freeze Frame","Echo Trail","Blur Trans","Particle Burst")
        for (l in list) c.addView(makeBtn(l) {
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
            }
            toast("VFX: $l")
        })
    }

    private fun makeBtn(label: String, onClick: () -> Unit): Button {
        val btn = Button(this)
        btn.text = label
        btn.isAllCaps = false
        btn.textSize = 11f
        btn.minWidth = 0
        btn.minHeight = 0
        btn.setPadding(24, 10, 24, 10)
        btn.setTextColor(Color.WHITE)
        btn.background = ContextCompat.getDrawable(this, R.drawable.bg_button)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(6, 4, 6, 4)
        btn.layoutParams = lp
        btn.setOnClickListener { onClick() }
        return btn
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        playback.release()
    }
}
