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
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import com.google.android.material.slider.Slider
import com.vexo.app.R
import com.vexo.app.editor.*
import com.vexo.app.utils.EffectUtils
import com.vexo.app.utils.FilterUtils
import androidx.media3.ui.PlayerView

class EditorActivity : AppCompatActivity() {

    // ── ViewModel (survives rotation) ────────────────────────────
    private val vm: EditorViewModel by viewModels()

    // ── Playback ─────────────────────────────────────────────────
    private val playback = PlaybackController(this.let { it })
    private val export   by lazy { ExportController(this) }

    // ── Views ────────────────────────────────────────────────────
    private lateinit var playerView:   PlayerView
    private lateinit var timelineView: TimelineView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var tvDuration:   TextView
    private lateinit var tvClipInfo:   TextView
    private lateinit var tvClipCount:  TextView
    private lateinit var filterOverlay: ImageView

    // ── Playhead update ─────────────────────────────────────────
    private val handler = Handler(Looper.getMainLooper())
    private val updatePlayhead = object : Runnable {
        override fun run() {
            val state = vm.state.value ?: return
            val pos = playback.currentPositionMs(state)
            timelineView.updatePlayhead(pos)
            updateDurationLabel(state)
            handler.postDelayed(this, 100)
        }
    }

    // ── Media picker ────────────────────────────────────────────
    private val mediaPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uris = mutableListOf<Uri>()
            val data = result.data
            // Multiple select
            val clipData = data?.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) uris.add(clipData.getItemAt(i).uri)
            } else {
                data?.data?.let { uris.add(it) }
            }
            if (uris.isNotEmpty()) importUris(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        bindViews()
        setupPlayback()
        setupToolbar()
        setupEditTools()
        setupTimeline()
        observeViewModel()
        setupFilterRow()
        setupEffectRow()
        setupAnimationRow()
        setupVideoEffectRow()

        // Initial import from MediaPicker
        val uriStrings = intent.getStringArrayListExtra("media_uris") ?: arrayListOf()
        if (uriStrings.isNotEmpty()) {
            importUris(uriStrings.map { Uri.parse(it) })
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  BIND VIEWS
    // ─────────────────────────────────────────────────────────────

    private fun bindViews() {
        playerView    = findViewById(R.id.playerView)
        timelineView  = findViewById(R.id.timelineView)
        btnPlayPause  = findViewById(R.id.btnPlayPause)
        tvDuration    = findViewById(R.id.tvDuration)
        tvClipInfo    = findViewById(R.id.tvClipInfo)
        tvClipCount   = findViewById(R.id.tvClipCount)

        // Filter overlay on top of player
        filterOverlay = ImageView(this).also {
            it.setImageDrawable(ColorDrawable(Color.WHITE))
            it.alpha = 0f
            it.layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            (playerView.parent as? android.widget.FrameLayout)?.addView(it)
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PLAYBACK
    // ─────────────────────────────────────────────────────────────

    private fun setupPlayback() {
        playback.init(playerView)
        playback.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                btnPlayPause.setImageResource(
                    if (isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play
                )
                if (isPlaying) handler.post(updatePlayhead)
                else handler.removeCallbacks(updatePlayhead)
            }
        })

        btnPlayPause.setOnClickListener {
            if (playback.player?.isPlaying == true) playback.pause()
            else playback.play()
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  TOOLBAR
    // ─────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnUndo).setOnClickListener {
            if (vm.undo()) {
                reloadPlayback()
                toast("Undone")
            } else toast("Nothing to undo")
        }
        findViewById<ImageButton>(R.id.btnRedo).setOnClickListener {
            if (vm.redo()) {
                reloadPlayback()
                toast("Redone")
            } else toast("Nothing to redo")
        }
        findViewById<Button>(R.id.btnExport).setOnClickListener { startExport() }
    }

    // ─────────────────────────────────────────────────────────────
    //  EDIT TOOLS
    // ─────────────────────────────────────────────────────────────

    private fun setupEditTools() {
        // Split at current playhead
        findViewById<Button>(R.id.btnSplit).setOnClickListener {
            val state = vm.state.value ?: return@setOnClickListener
            val selId = vm.selectedClipId.value
            if (selId == null) { toast("Select a clip first"); return@setOnClickListener }
            val posMs = playback.currentPositionMs(state)
            val clipStartMs = state.clipStartOffsetMs(selId)
            val posInClip = posMs - clipStartMs
            if (posInClip <= 0) { toast("Move playhead inside clip to split"); return@setOnClickListener }
            vm.splitClip(selId, posInClip)
            reloadPlayback()
            toast("Clip split")
        }

        // Delete selected clip
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            val selId = vm.selectedClipId.value
            if (selId == null) { toast("Select a clip first"); return@setOnClickListener }
            vm.deleteClip(selId)
            reloadPlayback()
            toast("Clip deleted")
        }

        // Speed cycle
        val speeds = floatArrayOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f)
        var speedIdx = 2
        findViewById<Button>(R.id.btnSpeed).setOnClickListener {
            speedIdx = (speedIdx + 1) % speeds.size
            val selId = vm.selectedClipId.value
            if (selId != null) {
                playback.player?.setPlaybackSpeed(speeds[speedIdx])
                toast("Speed: ${speeds[speedIdx]}x")
            } else toast("Select a clip first")
        }

        // Volume dialog
        findViewById<Button>(R.id.btnVolume).setOnClickListener {
            val selId = vm.selectedClipId.value ?: run { toast("Select a clip first"); return@setOnClickListener }
            val sb = SeekBar(this).apply { max = 100; progress = 80 }
            AlertDialog.Builder(this)
                .setTitle("Clip Volume")
                .setView(sb)
                .setPositiveButton("OK") { _, _ ->
                    vm.setVolume(selId, sb.progress / 100f)
                    reloadPlayback()
                }
                .show()
        }

        // Mute toggle
        findViewById<Button>(R.id.btnMute).setOnClickListener {
            val selId = vm.selectedClipId.value ?: run { toast("Select a clip first"); return@setOnClickListener }
            val clip = vm.state.value?.clips?.firstOrNull { it.id == selId } ?: return@setOnClickListener
            val newVol = if (clip.volume > 0f) 0f else 1f
            vm.setVolume(selId, newVol)
            reloadPlayback()
            toast(if (newVol == 0f) "Muted" else "Unmuted")
        }

        // Add more clips
        findViewById<Button>(R.id.btnAddClip).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            mediaPicker.launch(intent)
        }

        // Reorder left
        findViewById<Button>(R.id.btnMoveLeft).setOnClickListener {
            val selId = vm.selectedClipId.value ?: run { toast("Select a clip first"); return@setOnClickListener }
            val clips = vm.state.value?.clips ?: return@setOnClickListener
            val idx = clips.indexOfFirst { it.id == selId }
            if (idx <= 0) { toast("Already first"); return@setOnClickListener }
            vm.moveClip(idx, idx - 1)
            reloadPlayback()
            toast("Moved left")
        }

        // Reorder right
        findViewById<Button>(R.id.btnMoveRight).setOnClickListener {
            val selId = vm.selectedClipId.value ?: run { toast("Select a clip first"); return@setOnClickListener }
            val clips = vm.state.value?.clips ?: return@setOnClickListener
            val idx = clips.indexOfFirst { it.id == selId }
            if (idx >= clips.size - 1) { toast("Already last"); return@setOnClickListener }
            vm.moveClip(idx, idx + 1)
            reloadPlayback()
            toast("Moved right")
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  TIMELINE
    // ─────────────────────────────────────────────────────────────

    private fun setupTimeline() {
        timelineView.listener = object : TimelineView.Listener {
            override fun onClipSelected(clipId: String) {
                vm.selectClip(clipId)
            }
            override fun onPlayheadSeeked(positionMs: Long) {
                val state = vm.state.value ?: return
                playback.seekToMs(positionMs, state)
            }
            override fun onTrimChanged(clipId: String, newStartMs: Long, newEndMs: Long) {
                vm.trimClip(clipId, newStartMs, newEndMs)
                reloadPlayback(playWhenReady = false)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  OBSERVE VIEWMODEL
    // ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        vm.state.observe(this) { state ->
            // Update timeline visual
            timelineView.setClips(
                state.clips,
                state.totalDurationMs,
                playback.currentPositionMs(state),
                vm.selectedClipId.value
            )
            // Clip count
            tvClipCount.text = "${state.clips.size} clips"
            updateDurationLabel(state)
        }

        vm.selectedClipId.observe(this) { selId ->
            val state = vm.state.value ?: return@observe
            val clip = state.clips.firstOrNull { it.id == selId }
            tvClipInfo.text = if (clip != null) {
                val dur = clip.trimmedDurationMs / 1000f
                "Clip selected  •  ${String.format("%.1f", dur)}s  •  Vol: ${(clip.volume * 100).toInt()}%"
            } else "Tap a clip to select"
            timelineView.setClips(state.clips, state.totalDurationMs,
                playback.currentPositionMs(state), selId)
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  IMPORT
    // ─────────────────────────────────────────────────────────────

    private fun importUris(uris: List<Uri>) {
        // Take persistable permissions
        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }

        // Get durations via MediaMetadataRetriever
        val durations = mutableMapOf<Uri, Long>()
        uris.forEach { uri ->
            try {
                val ret = MediaMetadataRetriever()
                ret.setDataSource(this, uri)
                val dur = ret.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                ret.release()
                durations[uri] = dur
            } catch (e: Exception) {
                durations[uri] = 10_000L // fallback 10s
            }
        }

        vm.addClips(uris, durations)
        reloadPlayback()
    }

    // ─────────────────────────────────────────────────────────────
    //  RELOAD PLAYBACK
    // ─────────────────────────────────────────────────────────────

    private fun reloadPlayback(playWhenReady: Boolean = false) {
        val state = vm.state.value ?: return
        playback.loadState(state, playWhenReady)
    }

    // ─────────────────────────────────────────────────────────────
    //  EXPORT
    // ─────────────────────────────────────────────────────────────

    private fun startExport() {
        val state = vm.state.value ?: return
        if (state.clips.isEmpty()) { toast("No clips to export"); return }

        playback.pause()

        val progress = AlertDialog.Builder(this)
            .setTitle("Exporting…")
            .setMessage("0%")
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ -> export.cancel() }
            .create()
        progress.show()

        export.export(state, object : ExportController.ExportCallback {
            override fun onProgress(percent: Int) {
                runOnUiThread { progress.setMessage("$percent%") }
            }
            override fun onSuccess(filePath: String) {
                runOnUiThread {
                    progress.dismiss()
                    AlertDialog.Builder(this@EditorActivity)
                        .setTitle("✅ Export Complete!")
                        .setMessage("Video saved to gallery.\n$filePath")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            override fun onFailure(error: String) {
                runOnUiThread {
                    progress.dismiss()
                    toast("Export failed: $error")
                }
            }
        })
    }

    // ─────────────────────────────────────────────────────────────
    //  DURATION LABEL
    // ─────────────────────────────────────────────────────────────

    private fun updateDurationLabel(state: EditorState) {
        val pos = playback.currentPositionMs(state)
        val total = state.totalDurationMs
        tvDuration.text = "${formatMs(pos)} / ${formatMs(total)}"
    }

    private fun formatMs(ms: Long): String {
        val s = ms / 1000
        return "${s / 60}:${String.format("%02d", s % 60)}"
    }

    // ─────────────────────────────────────────────────────────────
    //  FILTER / EFFECT / ANIMATION / VFX ROWS
    // ─────────────────────────────────────────────────────────────

    private fun setupFilterRow() {
        val c = findViewById<LinearLayout>(R.id.filterButtonsContainer)
        listOf(
            "None" to null,
            "Cinematic" to FilterUtils.cinematic(),
            "HDR" to FilterUtils.hdr(),
            "Aesthetic" to FilterUtils.aesthetic(),
            "Warm Glow" to FilterUtils.warmGlow(),
            "Cool Tone" to FilterUtils.coolTone(),
            "Vintage" to FilterUtils.vintageFilm(),
            "Retro" to FilterUtils.retro(),
            "Y2K" to FilterUtils.y2k(),
            "VHS" to FilterUtils.vhs(),
            "B&W Noir" to FilterUtils.bwNoir(),
            "Glamour" to FilterUtils.glamour(),
            "Night" to FilterUtils.nightScene(),
            "Movie" to FilterUtils.movie(),
            "Colorist" to FilterUtils.colorist(),
            "Neon" to FilterUtils.neon(),
            "Dreamy" to FilterUtils.dreamy(),
            "Dark Mood" to FilterUtils.darkMood(),
            "Faded Film" to FilterUtils.fadedFilm(),
            "Cartoon AI" to FilterUtils.cartoonAI(),
            "Barbie Pink" to FilterUtils.barbiePink()
        ).forEach { (label, matrix) ->
            c.addView(makeBtn(label) {
                applyFilter(matrix)
                toast("Filter: $label")
            })
        }
    }

    private fun applyFilter(matrix: ColorMatrix?) {
        if (matrix == null) {
            filterOverlay.colorFilter = null
            filterOverlay.alpha = 0f
        } else {
            filterOverlay.colorFilter = ColorMatrixColorFilter(matrix)
            filterOverlay.alpha = 0.35f
        }
    }

    private fun setupEffectRow() {
        val c = findViewById<LinearLayout>(R.id.effectButtonsContainer)
        listOf(
            "Glow" to { EffectUtils.applyGlow(playerView) },
            "Motion Blur" to { EffectUtils.applyMotionBlur(playerView) },
            "Zoom" to { EffectUtils.applyZoom(playerView) },
            "3D Zoom" to { EffectUtils.apply3DZoom(playerView) },
            "Shake" to { EffectUtils.applyShake(playerView) },
            "Flash" to { EffectUtils.applyFlash(playerView) },
            "Glitch" to { EffectUtils.applyGlitch(playerView) },
            "RGB Split" to { EffectUtils.applyRGBSplit(playerView) },
            "Chromatic" to { EffectUtils.applyChromatic(playerView) },
            "Lens Flare" to { EffectUtils.applyLensFlare(playerView) },
            "Light Leak" to { EffectUtils.applyLightLeak(playerView) },
            "Film Grain" to { EffectUtils.applyFilmGrain(playerView) },
            "Vignette" to { EffectUtils.applyVignette(playerView) },
            "Blur" to { EffectUtils.applyBlur(playerView) },
            "Pixelate" to { EffectUtils.applyPixelate(playerView) },
            "Noise" to { EffectUtils.applyNoise(playerView) },
            "Smoke" to { EffectUtils.applySmoke(playerView) },
            "Fire" to { EffectUtils.applyFire(playerView) },
            "Spark" to { EffectUtils.applySpark(playerView) },
            "Aura" to { EffectUtils.applyAura(playerView) }
        ).forEach { (l, fn) -> c.addView(makeBtn(l) { fn(); toast("Effect: $l") }) }
    }

    private fun setupAnimationRow() {
        val c = findViewById<LinearLayout>(R.id.animationButtonsContainer)
        listOf(
            "Fade In" to { EffectUtils.animFadeIn(playerView) },
            "Fade Out" to { EffectUtils.animFadeOut(playerView) },
            "Zoom In" to { EffectUtils.animZoomIn(playerView) },
            "Zoom Out" to { EffectUtils.animZoomOut(playerView) },
            "Pop Up" to { EffectUtils.animPopUp(playerView) },
            "Bounce" to { EffectUtils.animBounce(playerView) },
            "Slide Left" to { EffectUtils.animSlideLeft(playerView) },
            "Slide Right" to { EffectUtils.animSlideRight(playerView) },
            "Slide Up" to { EffectUtils.animSlideUp(playerView) },
            "Slide Down" to { EffectUtils.animSlideDown(playerView) },
            "Spin" to { EffectUtils.animSpin(playerView) },
            "Swing" to { EffectUtils.animSwing(playerView) },
            "Shake" to { EffectUtils.animShake(playerView) },
            "Wobble" to { EffectUtils.animWobble(playerView) },
            "Pulse" to { EffectUtils.animPulse(playerView) },
            "Float" to { EffectUtils.animFloat(playerView) },
            "Typewriter" to { EffectUtils.animTypewriter(playerView) },
            "Elastic" to { EffectUtils.animElastic(playerView) },
            "Flip" to { EffectUtils.animFlip(playerView) },
            "3D Rotate" to { EffectUtils.anim3DRotate(playerView) }
        ).forEach { (l, fn) -> c.addView(makeBtn(l) { fn(); toast("Anim: $l") }) }
    }

    private fun setupVideoEffectRow() {
        val c = findViewById<LinearLayout>(R.id.videoEffectButtonsContainer)
        listOf(
            "Velocity" to { EffectUtils.videoVelocity(playerView) },
            "Slow Mo" to { EffectUtils.videoSlowMo(playerView) },
            "Speed Ramp" to { EffectUtils.videoSpeedRamp(playerView) },
            "Beat Shake" to { EffectUtils.videoBeatShake(playerView) },
            "Flash Beat" to { EffectUtils.videoFlashBeat(playerView) },
            "Glitch Trans" to { EffectUtils.videoGlitchTransition(playerView) },
            "RGB Glitch" to { EffectUtils.videoRGBGlitch(playerView) },
            "Motion Trail" to { EffectUtils.videoMotionTrail(playerView) },
            "Cam Shake" to { EffectUtils.videoCameraShake(playerView) },
            "Dyn. Zoom" to { EffectUtils.videoDynamicZoom(playerView) },
            "Spin Trans" to { EffectUtils.videoSpinTransition(playerView) },
            "Whip Pan" to { EffectUtils.videoWhipPan(playerView) },
            "Light Sweep" to { EffectUtils.videoLightSweep(playerView) },
            "Lens Flare" to { EffectUtils.videoLensFlare(playerView) },
            "Film Burn" to { EffectUtils.videoFilmBurn(playerView) },
            "Flashback" to { EffectUtils.videoFlashback(playerView) },
            "Freeze Frame" to { EffectUtils.videoFreezeFrame(playerView) },
            "Echo Trail" to { EffectUtils.videoEchoTrail(playerView) },
            "Blur Trans" to { EffectUtils.videoBlurTransition(playerView) },
            "Particle Burst" to { EffectUtils.videoParticleBurst(playerView) }
        ).forEach { (l, fn) -> c.addView(makeBtn(l) { fn(); toast("VFX: $l") }) }
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun makeBtn(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 11f
            minWidth = 0; minHeight = 0
            setPadding(24, 10, 24, 10)
            setTextColor(Color.WHITE)
            background = ContextCompat.getDrawable(this@EditorActivity, R.drawable.bg_button)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(6, 4, 6, 4) }
            setOnClickListener { onClick() }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ─────────────────────────────────────────────────────────────
    //  LIFECYCLE
    // ─────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        // Re-init player if needed
        if (playback.player == null) {
            playback.init(playerView)
            reloadPlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updatePlayhead)
        playback.release()
    }
}
