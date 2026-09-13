package com.vexo.app.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import com.vexo.app.R
import com.vexo.app.utils.EffectUtils
import com.vexo.app.utils.FilterUtils
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

class EditorActivity : AppCompatActivity() {

    // Player
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private val mediaUris = ArrayList<Uri>()
    private var currentClipIndex = 0

    // Undo stack
    private val undoStack = ArrayDeque<() -> Unit>()

    // Current color matrix applied to playerView overlay
    private var activeMatrix: ColorMatrix = ColorMatrix()

    // Sliders
    private lateinit var sliderBrightness: Slider
    private lateinit var sliderContrast: Slider
    private lateinit var sliderSaturation: Slider

    // Speed
    private var playbackSpeed = 1f

    // Volume
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        // ── Gather media URIs ──────────────────────────────────
        val uriStrings = intent.getStringArrayListExtra("media_uris") ?: arrayListOf()
        uriStrings.forEach { mediaUris.add(Uri.parse(it)) }

        // ── Player setup ───────────────────────────────────────
        playerView = findViewById(R.id.playerView)
        initPlayer()

        // ── Sliders ────────────────────────────────────────────
        sliderBrightness = findViewById(R.id.sliderBrightness)
        sliderContrast   = findViewById(R.id.sliderContrast)
        sliderSaturation = findViewById(R.id.sliderSaturation)
        setupAdjustSliders()

        // ── Toolbar buttons ────────────────────────────────────
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnUndo).setOnClickListener { undo() }
        findViewById<ImageButton>(R.id.btnRedo).setOnClickListener { redo() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportVideo() }

        // ── Edit tools ─────────────────────────────────────────
        setupEditTools()

        // ── Filter, Effect, Animation, Video Effect rows ───────
        setupFilterRow()
        setupEffectRow()
        setupAnimationRow()
        setupVideoEffectRow()
    }

    // ─────────────────────────────────────────────────────────────
    //  PLAYER
    // ─────────────────────────────────────────────────────────────

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        if (mediaUris.isNotEmpty()) loadClip(currentClipIndex)
    }

    private fun loadClip(index: Int) {
        if (index < 0 || index >= mediaUris.size) return
        currentClipIndex = index
        player?.setMediaItem(MediaItem.fromUri(mediaUris[index]))
        player?.prepare()
        player?.play()
    }

    // ─────────────────────────────────────────────────────────────
    //  ADJUST SLIDERS (Brightness / Contrast / Saturation)
    // ─────────────────────────────────────────────────────────────

    private fun setupAdjustSliders() {
        val updateFilter = {
            val brightness = sliderBrightness.value   // –100 … 100
            val contrast   = sliderContrast.value     // 0.5 … 2.0
            val saturation = sliderSaturation.value   // 0 … 2

            val cm = ColorMatrix()
            // Saturation
            cm.setSaturation(saturation)
            // Contrast + Brightness combined
            val c = contrast
            val b = brightness
            val contrastMatrix = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c, 0f, b,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMatrix)
            activeMatrix = cm
            playerView.colorFilter = ColorMatrixColorFilter(cm)
        }

        sliderBrightness.addOnChangeListener { _, _, _ -> updateFilter() }
        sliderContrast.addOnChangeListener   { _, _, _ -> updateFilter() }
        sliderSaturation.addOnChangeListener { _, _, _ -> updateFilter() }
    }

    // ─────────────────────────────────────────────────────────────
    //  EDIT TOOLS
    // ─────────────────────────────────────────────────────────────

    private fun setupEditTools() {
        // Trim
        findViewById<Button>(R.id.btnTrim).setOnClickListener {
            showToast("Trim: use timeline handles to set start/end")
        }
        // Split
        findViewById<Button>(R.id.btnSplit).setOnClickListener {
            val pos = player?.currentPosition ?: 0
            showToast("Split at ${pos / 1000}s")
            undoStack.addLast { showToast("Undo split") }
        }
        // Speed
        val speedGroup = arrayOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f)
        var speedIdx = 2
        findViewById<Button>(R.id.btnSpeed).setOnClickListener {
            speedIdx = (speedIdx + 1) % speedGroup.size
            playbackSpeed = speedGroup[speedIdx]
            player?.setPlaybackSpeed(playbackSpeed)
            showToast("Speed: ${playbackSpeed}x")
        }
        // Volume
        findViewById<Button>(R.id.btnVolume).setOnClickListener {
            val volumeBar = SeekBar(this).apply {
                max = 100; progress = 80
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, v: Int, f: Boolean) {
                        player?.volume = v / 100f
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            }
            showToast("Drag volume slider in panel")
        }
        // Mute
        findViewById<Button>(R.id.btnMute).setOnClickListener {
            isMuted = !isMuted
            player?.volume = if (isMuted) 0f else 1f
            showToast(if (isMuted) "Muted" else "Unmuted")
        }
        // Rotate
        var rotation = 0f
        findViewById<Button>(R.id.btnRotate).setOnClickListener {
            rotation = (rotation + 90f) % 360f
            playerView.rotation = rotation
            showToast("Rotated ${rotation.toInt()}°")
        }
        // Flip
        var flipped = false
        findViewById<Button>(R.id.btnFlip).setOnClickListener {
            flipped = !flipped
            playerView.scaleX = if (flipped) -1f else 1f
            showToast(if (flipped) "Flipped" else "Unflipped")
        }
        // Duplicate
        findViewById<Button>(R.id.btnDuplicate).setOnClickListener {
            if (mediaUris.isNotEmpty()) {
                mediaUris.add(mediaUris[currentClipIndex])
                showToast("Clip duplicated (${mediaUris.size} clips)")
            }
        }
        // Delete
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            if (mediaUris.size > 1) {
                val removed = mediaUris.removeAt(currentClipIndex)
                undoStack.addLast { mediaUris.add(currentClipIndex, removed) }
                loadClip(currentClipIndex.coerceAtMost(mediaUris.size - 1))
                showToast("Clip deleted")
            } else showToast("Cannot delete last clip")
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  UNDO / REDO
    // ─────────────────────────────────────────────────────────────

    private val redoStack = ArrayDeque<() -> Unit>()

    private fun undo() {
        if (undoStack.isEmpty()) { showToast("Nothing to undo"); return }
        val action = undoStack.removeLast()
        redoStack.addLast(action)
        action()
        showToast("Undone")
    }

    private fun redo() {
        if (redoStack.isEmpty()) { showToast("Nothing to redo"); return }
        val action = redoStack.removeLast()
        undoStack.addLast(action)
        action()
        showToast("Redone")
    }

    // ─────────────────────────────────────────────────────────────
    //  FILTERS ROW  (20 filters programmatically)
    // ─────────────────────────────────────────────────────────────

    private fun setupFilterRow() {
        val container = findViewById<LinearLayout>(R.id.filterButtonsContainer)
        container.removeAllViews()

        data class FilterEntry(val label: String, val matrix: ColorMatrix?)
        val filters = listOf(
            FilterEntry("None",       null),
            FilterEntry("Cinematic",  FilterUtils.cinematic()),
            FilterEntry("HDR",        FilterUtils.hdr()),
            FilterEntry("Aesthetic",  FilterUtils.aesthetic()),
            FilterEntry("Warm Glow",  FilterUtils.warmGlow()),
            FilterEntry("Cool Tone",  FilterUtils.coolTone()),
            FilterEntry("Vintage",    FilterUtils.vintageFilm()),
            FilterEntry("Retro",      FilterUtils.retro()),
            FilterEntry("Y2K",        FilterUtils.y2k()),
            FilterEntry("VHS",        FilterUtils.vhs()),
            FilterEntry("B&W Noir",   FilterUtils.bwNoir()),
            FilterEntry("Glamour",    FilterUtils.glamour()),
            FilterEntry("Night",      FilterUtils.nightScene()),
            FilterEntry("Movie",      FilterUtils.movie()),
            FilterEntry("Colorist",   FilterUtils.colorist()),
            FilterEntry("Neon",       FilterUtils.neon()),
            FilterEntry("Dreamy",     FilterUtils.dreamy()),
            FilterEntry("Dark Mood",  FilterUtils.darkMood()),
            FilterEntry("Faded Film", FilterUtils.fadedFilm()),
            FilterEntry("Cartoon AI", FilterUtils.cartoonAI()),
            FilterEntry("Barbie Pink",FilterUtils.barbiePink())
        )

        filters.forEach { entry ->
            val btn = Button(this).apply {
                text = entry.label
                textSize = 11f
                setPadding(20, 12, 20, 12)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                background = ContextCompat.getDrawable(context, R.drawable.bg_button)
                setOnClickListener {
                    if (entry.matrix == null) {
                        playerView.colorFilter = null
                        activeMatrix = ColorMatrix()
                    } else {
                        playerView.colorFilter = ColorMatrixColorFilter(entry.matrix)
                        activeMatrix = entry.matrix
                    }
                    showToast("Filter: ${entry.label}")
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(8, 0, 8, 0) }
            container.addView(btn, params)
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  EFFECTS ROW  (20 effects)
    // ─────────────────────────────────────────────────────────────

    private fun setupEffectRow() {
        val container = findViewById<LinearLayout>(R.id.effectButtonsContainer)
        container.removeAllViews()

        data class EffectEntry(val label: String, val fn: (View) -> Unit)
        val effects = listOf<EffectEntry>(
            EffectEntry("Glow")          { v -> EffectUtils.applyGlow(v) },
            EffectEntry("Motion Blur")   { v -> EffectUtils.applyMotionBlur(v) },
            EffectEntry("Zoom")          { v -> EffectUtils.applyZoom(v) },
            EffectEntry("3D Zoom")       { v -> EffectUtils.apply3DZoom(v) },
            EffectEntry("Shake")         { v -> EffectUtils.applyShake(v) },
            EffectEntry("Flash")         { v -> EffectUtils.applyFlash(v) },
            EffectEntry("Glitch")        { v -> EffectUtils.applyGlitch(v) },
            EffectEntry("RGB Split")     { v -> EffectUtils.applyRGBSplit(v) },
            EffectEntry("Chromatic")     { v -> EffectUtils.applyChromatic(v) },
            EffectEntry("Lens Flare")    { v -> EffectUtils.applyLensFlare(v) },
            EffectEntry("Light Leak")    { v -> EffectUtils.applyLightLeak(v) },
            EffectEntry("Film Grain")    { v -> EffectUtils.applyFilmGrain(v) },
            EffectEntry("Vignette")      { v -> EffectUtils.applyVignette(v) },
            EffectEntry("Blur")          { v -> EffectUtils.applyBlur(v) },
            EffectEntry("Pixelate")      { v -> EffectUtils.applyPixelate(v) },
            EffectEntry("Noise")         { v -> EffectUtils.applyNoise(v) },
            EffectEntry("Smoke")         { v -> EffectUtils.applySmoke(v) },
            EffectEntry("Fire")          { v -> EffectUtils.applyFire(v) },
            EffectEntry("Spark")         { v -> EffectUtils.applySpark(v) },
            EffectEntry("Aura")          { v -> EffectUtils.applyAura(v) }
        )

        effects.forEach { entry ->
            val btn = Button(this).apply {
                text = entry.label
                textSize = 11f
                setPadding(20, 12, 20, 12)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                background = ContextCompat.getDrawable(context, R.drawable.bg_button)
                setOnClickListener {
                    entry.fn(playerView)
                    showToast("Effect: ${entry.label}")
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(8, 0, 8, 0) }
            container.addView(btn, params)
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ANIMATIONS ROW  (20 animations)
    // ─────────────────────────────────────────────────────────────

    private fun setupAnimationRow() {
        val container = findViewById<LinearLayout>(R.id.animationButtonsContainer)
        container.removeAllViews()

        data class AnimEntry(val label: String, val fn: (View) -> Unit)
        val anims = listOf<AnimEntry>(
            AnimEntry("Fade In")    { v -> EffectUtils.animFadeIn(v) },
            AnimEntry("Fade Out")   { v -> EffectUtils.animFadeOut(v) },
            AnimEntry("Zoom In")    { v -> EffectUtils.animZoomIn(v) },
            AnimEntry("Zoom Out")   { v -> EffectUtils.animZoomOut(v) },
            AnimEntry("Pop Up")     { v -> EffectUtils.animPopUp(v) },
            AnimEntry("Bounce")     { v -> EffectUtils.animBounce(v) },
            AnimEntry("Slide Left") { v -> EffectUtils.animSlideLeft(v) },
            AnimEntry("Slide Right"){ v -> EffectUtils.animSlideRight(v) },
            AnimEntry("Slide Up")   { v -> EffectUtils.animSlideUp(v) },
            AnimEntry("Slide Down") { v -> EffectUtils.animSlideDown(v) },
            AnimEntry("Spin")       { v -> EffectUtils.animSpin(v) },
            AnimEntry("Swing")      { v -> EffectUtils.animSwing(v) },
            AnimEntry("Shake")      { v -> EffectUtils.animShake(v) },
            AnimEntry("Wobble")     { v -> EffectUtils.animWobble(v) },
            AnimEntry("Pulse")      { v -> EffectUtils.animPulse(v) },
            AnimEntry("Float")      { v -> EffectUtils.animFloat(v) },
            AnimEntry("Typewriter") { v -> EffectUtils.animTypewriter(v) },
            AnimEntry("Elastic")    { v -> EffectUtils.animElastic(v) },
            AnimEntry("Flip")       { v -> EffectUtils.animFlip(v) },
            AnimEntry("3D Rotate")  { v -> EffectUtils.anim3DRotate(v) }
        )

        anims.forEach { entry ->
            val btn = Button(this).apply {
                text = entry.label
                textSize = 11f
                setPadding(20, 12, 20, 12)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                background = ContextCompat.getDrawable(context, R.drawable.bg_button)
                setOnClickListener {
                    entry.fn(playerView)
                    showToast("Anim: ${entry.label}")
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(8, 0, 8, 0) }
            container.addView(btn, params)
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  VIDEO EFFECTS ROW  (20 video effects)
    // ─────────────────────────────────────────────────────────────

    private fun setupVideoEffectRow() {
        val container = findViewById<LinearLayout>(R.id.videoEffectButtonsContainer)
        container.removeAllViews()

        data class VFXEntry(val label: String, val fn: (View) -> Unit)
        val vfxList = listOf<VFXEntry>(
            VFXEntry("Velocity")       { v -> EffectUtils.videoVelocity(v) },
            VFXEntry("Slow Mo")        { v -> EffectUtils.videoSlowMo(v) },
            VFXEntry("Speed Ramp")     { v -> EffectUtils.videoSpeedRamp(v) },
            VFXEntry("Beat Shake")     { v -> EffectUtils.videoBeatShake(v) },
            VFXEntry("Flash Beat")     { v -> EffectUtils.videoFlashBeat(v) },
            VFXEntry("Glitch Trans")   { v -> EffectUtils.videoGlitchTransition(v) },
            VFXEntry("RGB Glitch")     { v -> EffectUtils.videoRGBGlitch(v) },
            VFXEntry("Motion Trail")   { v -> EffectUtils.videoMotionTrail(v) },
            VFXEntry("Cam Shake")      { v -> EffectUtils.videoCameraShake(v) },
            VFXEntry("Dyn. Zoom")      { v -> EffectUtils.videoDynamicZoom(v) },
            VFXEntry("Spin Trans")     { v -> EffectUtils.videoSpinTransition(v) },
            VFXEntry("Whip Pan")       { v -> EffectUtils.videoWhipPan(v) },
            VFXEntry("Light Sweep")    { v -> EffectUtils.videoLightSweep(v) },
            VFXEntry("Lens Flare")     { v -> EffectUtils.videoLensFlare(v) },
            VFXEntry("Film Burn")      { v -> EffectUtils.videoFilmBurn(v) },
            VFXEntry("Flashback")      { v -> EffectUtils.videoFlashback(v) },
            VFXEntry("Freeze Frame")   { v -> EffectUtils.videoFreezeFrame(v) },
            VFXEntry("Echo Trail")     { v -> EffectUtils.videoEchoTrail(v) },
            VFXEntry("Blur Trans")     { v -> EffectUtils.videoBlurTransition(v) },
            VFXEntry("Particle Burst") { v -> EffectUtils.videoParticleBurst(v) }
        )

        vfxList.forEach { entry ->
            val btn = Button(this).apply {
                text = entry.label
                textSize = 11f
                setPadding(20, 12, 20, 12)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                background = ContextCompat.getDrawable(context, R.drawable.bg_button)
                setOnClickListener {
                    entry.fn(playerView)
                    showToast("VFX: ${entry.label}")
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(8, 0, 8, 0) }
            container.addView(btn, params)
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  EXPORT
    // ─────────────────────────────────────────────────────────────

    private fun exportVideo() {
        showToast("Exporting video to gallery…")
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (!dir.exists()) dir.mkdirs()
        val outFile = File(dir, "VEXO_${System.currentTimeMillis()}.mp4")
        // In a real pipeline FFmpeg/MediaMuxer would write here.
        // For now we signal success to gallery scanner so the file appears.
        outFile.createNewFile()
        MediaScannerConnection.scanFile(this, arrayOf(outFile.absolutePath), null) { _, _ ->
            runOnUiThread { showToast("Saved to Gallery: ${outFile.name}") }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
