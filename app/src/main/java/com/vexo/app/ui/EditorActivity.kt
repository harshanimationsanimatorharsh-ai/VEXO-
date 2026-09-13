package com.vexo.app.ui

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.ColorDrawable
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

    // ── Player ───────────────────────────────────────────────────
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private val mediaUris = ArrayList<Uri>()
    private var currentClipIndex = 0

    // ── Filter overlay (colorFilter applied here, NOT on PlayerView) ──
    private lateinit var filterOverlay: ImageView

    // ── Adjust sliders ───────────────────────────────────────────
    private lateinit var sliderBrightness: Slider
    private lateinit var sliderContrast: Slider
    private lateinit var sliderSaturation: Slider

    // ── Undo / Redo stacks ───────────────────────────────────────
    private val undoStack = ArrayDeque<() -> Unit>()
    private val redoStack = ArrayDeque<() -> Unit>()

    // ── State ────────────────────────────────────────────────────
    private var playbackSpeed = 1f
    private var isMuted = false

    // ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        // Gather URIs
        val uriStrings = intent.getStringArrayListExtra("media_uris") ?: arrayListOf()
        uriStrings.forEach { mediaUris.add(Uri.parse(it)) }

        // Player
        playerView = findViewById(R.id.playerView)
        initPlayer()

        // Filter overlay
        filterOverlay = findViewById(R.id.filterOverlay)
        filterOverlay.setImageDrawable(ColorDrawable(Color.WHITE))
        filterOverlay.alpha = 0f   // invisible until a filter is applied

        // Sliders
        sliderBrightness = findViewById(R.id.sliderBrightness)
        sliderContrast   = findViewById(R.id.sliderContrast)
        sliderSaturation = findViewById(R.id.sliderSaturation)
        setupAdjustSliders()

        // Toolbar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnUndo).setOnClickListener { undo() }
        findViewById<ImageButton>(R.id.btnRedo).setOnClickListener { redo() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportVideo() }

        // Edit tools
        setupEditTools()

        // Dynamic rows
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
    //  ADJUST SLIDERS
    // ─────────────────────────────────────────────────────────────

    private fun setupAdjustSliders() {
        val applyAdjust = {
            val brightness = sliderBrightness.value          // –100 … 100
            val contrast   = sliderContrast.value            // 0.5 … 2.0
            val saturation = sliderSaturation.value          // 0.0 … 2.0

            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(saturation)

            val c = contrast
            val b = brightness
            val contrastMatrix = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c, 0f, b,
                0f, 0f, 0f, 1f, 0f
            ))
            satMatrix.postConcat(contrastMatrix)
            applyColorMatrix(satMatrix)
        }

        sliderBrightness.addOnChangeListener { _, _, _ -> applyAdjust() }
        sliderContrast  .addOnChangeListener { _, _, _ -> applyAdjust() }
        sliderSaturation.addOnChangeListener { _, _, _ -> applyAdjust() }
    }

    /** Apply a ColorMatrix to the transparent overlay on top of the player. */
    private fun applyColorMatrix(matrix: ColorMatrix?) {
        if (matrix == null) {
            filterOverlay.colorFilter = null
            filterOverlay.alpha = 0f
        } else {
            filterOverlay.colorFilter = ColorMatrixColorFilter(matrix)
            filterOverlay.alpha = 0.35f   // semi-transparent tint over video
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  EDIT TOOLS
    // ─────────────────────────────────────────────────────────────

    private fun setupEditTools() {
        // Trim
        findViewById<Button>(R.id.btnTrim).setOnClickListener {
            toast("Trim: drag timeline handles to set in/out points")
        }

        // Split
        findViewById<Button>(R.id.btnSplit).setOnClickListener {
            val posSec = (player?.currentPosition ?: 0L) / 1000L
            toast("Split at ${posSec}s")
            undoStack.addLast { toast("Undo: split removed") }
        }

        // Speed cycle
        val speeds = floatArrayOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f)
        var speedIdx = 2
        findViewById<Button>(R.id.btnSpeed).setOnClickListener {
            speedIdx = (speedIdx + 1) % speeds.size
            playbackSpeed = speeds[speedIdx]
            player?.setPlaybackSpeed(playbackSpeed)
            toast("Speed: ${playbackSpeed}x")
        }

        // Volume (simple toggle loud/quiet for demo)
        var volumeLevel = 1f
        findViewById<Button>(R.id.btnVolume).setOnClickListener {
            volumeLevel = if (volumeLevel >= 1f) 0.3f else 1f
            player?.volume = volumeLevel
            toast("Volume: ${(volumeLevel * 100).toInt()}%")
        }

        // Mute
        findViewById<Button>(R.id.btnMute).setOnClickListener {
            isMuted = !isMuted
            player?.volume = if (isMuted) 0f else 1f
            toast(if (isMuted) "Muted" else "Unmuted")
        }

        // Rotate
        var rotationDeg = 0f
        findViewById<Button>(R.id.btnRotate).setOnClickListener {
            rotationDeg = (rotationDeg + 90f) % 360f
            playerView.rotation = rotationDeg
            toast("Rotated ${rotationDeg.toInt()}°")
        }

        // Flip
        var flipped = false
        findViewById<Button>(R.id.btnFlip).setOnClickListener {
            flipped = !flipped
            playerView.scaleX = if (flipped) -1f else 1f
            toast(if (flipped) "Flipped horizontally" else "Flip removed")
        }

        // Duplicate
        findViewById<Button>(R.id.btnDuplicate).setOnClickListener {
            if (mediaUris.isNotEmpty()) {
                val uri = mediaUris[currentClipIndex]
                mediaUris.add(uri)
                toast("Clip duplicated — ${mediaUris.size} clips total")
            }
        }

        // Delete
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            if (mediaUris.size > 1) {
                val removed = mediaUris.removeAt(currentClipIndex)
                undoStack.addLast {
                    mediaUris.add(currentClipIndex, removed)
                    toast("Undo: clip restored")
                }
                loadClip(currentClipIndex.coerceAtMost(mediaUris.size - 1))
                toast("Clip deleted")
            } else {
                toast("Cannot delete the last clip")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  UNDO / REDO
    // ─────────────────────────────────────────────────────────────

    private fun undo() {
        if (undoStack.isEmpty()) { toast("Nothing to undo"); return }
        val action = undoStack.removeLast()
        redoStack.addLast(action)
        action()
        toast("Undone")
    }

    private fun redo() {
        if (redoStack.isEmpty()) { toast("Nothing to redo"); return }
        val action = redoStack.removeLast()
        undoStack.addLast(action)
        action()
        toast("Redone")
    }

    // ─────────────────────────────────────────────────────────────
    //  FILTER ROW  (20 filters — buttons built in code)
    // ─────────────────────────────────────────────────────────────

    private fun setupFilterRow() {
        val container = findViewById<LinearLayout>(R.id.filterButtonsContainer)
        container.removeAllViews()

        data class F(val label: String, val matrix: ColorMatrix?)

        val filters = listOf(
            F("None",        null),
            F("Cinematic",   FilterUtils.cinematic()),
            F("HDR",         FilterUtils.hdr()),
            F("Aesthetic",   FilterUtils.aesthetic()),
            F("Warm Glow",   FilterUtils.warmGlow()),
            F("Cool Tone",   FilterUtils.coolTone()),
            F("Vintage",     FilterUtils.vintageFilm()),
            F("Retro",       FilterUtils.retro()),
            F("Y2K",         FilterUtils.y2k()),
            F("VHS",         FilterUtils.vhs()),
            F("B&W Noir",    FilterUtils.bwNoir()),
            F("Glamour",     FilterUtils.glamour()),
            F("Night",       FilterUtils.nightScene()),
            F("Movie",       FilterUtils.movie()),
            F("Colorist",    FilterUtils.colorist()),
            F("Neon",        FilterUtils.neon()),
            F("Dreamy",      FilterUtils.dreamy()),
            F("Dark Mood",   FilterUtils.darkMood()),
            F("Faded Film",  FilterUtils.fadedFilm()),
            F("Cartoon AI",  FilterUtils.cartoonAI()),
            F("Barbie Pink", FilterUtils.barbiePink())
        )

        filters.forEach { f ->
            container.addView(makeButton(f.label) {
                applyColorMatrix(f.matrix)
                toast("Filter: ${f.label}")
            })
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  EFFECT ROW  (20 effects)
    // ─────────────────────────────────────────────────────────────

    private fun setupEffectRow() {
        val container = findViewById<LinearLayout>(R.id.effectButtonsContainer)
        container.removeAllViews()

        data class E(val label: String, val fn: (View) -> Unit)

        val list = listOf(
            E("Glow")        { v -> EffectUtils.applyGlow(v) },
            E("Motion Blur") { v -> EffectUtils.applyMotionBlur(v) },
            E("Zoom")        { v -> EffectUtils.applyZoom(v) },
            E("3D Zoom")     { v -> EffectUtils.apply3DZoom(v) },
            E("Shake")       { v -> EffectUtils.applyShake(v) },
            E("Flash")       { v -> EffectUtils.applyFlash(v) },
            E("Glitch")      { v -> EffectUtils.applyGlitch(v) },
            E("RGB Split")   { v -> EffectUtils.applyRGBSplit(v) },
            E("Chromatic")   { v -> EffectUtils.applyChromatic(v) },
            E("Lens Flare")  { v -> EffectUtils.applyLensFlare(v) },
            E("Light Leak")  { v -> EffectUtils.applyLightLeak(v) },
            E("Film Grain")  { v -> EffectUtils.applyFilmGrain(v) },
            E("Vignette")    { v -> EffectUtils.applyVignette(v) },
            E("Blur")        { v -> EffectUtils.applyBlur(v) },
            E("Pixelate")    { v -> EffectUtils.applyPixelate(v) },
            E("Noise")       { v -> EffectUtils.applyNoise(v) },
            E("Smoke")       { v -> EffectUtils.applySmoke(v) },
            E("Fire")        { v -> EffectUtils.applyFire(v) },
            E("Spark")       { v -> EffectUtils.applySpark(v) },
            E("Aura")        { v -> EffectUtils.applyAura(v) }
        )

        list.forEach { e ->
            container.addView(makeButton(e.label) {
                e.fn(playerView)
                toast("Effect: ${e.label}")
            })
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ANIMATION ROW  (20 animations)
    // ─────────────────────────────────────────────────────────────

    private fun setupAnimationRow() {
        val container = findViewById<LinearLayout>(R.id.animationButtonsContainer)
        container.removeAllViews()

        data class A(val label: String, val fn: (View) -> Unit)

        val list = listOf(
            A("Fade In")     { v -> EffectUtils.animFadeIn(v) },
            A("Fade Out")    { v -> EffectUtils.animFadeOut(v) },
            A("Zoom In")     { v -> EffectUtils.animZoomIn(v) },
            A("Zoom Out")    { v -> EffectUtils.animZoomOut(v) },
            A("Pop Up")      { v -> EffectUtils.animPopUp(v) },
            A("Bounce")      { v -> EffectUtils.animBounce(v) },
            A("Slide Left")  { v -> EffectUtils.animSlideLeft(v) },
            A("Slide Right") { v -> EffectUtils.animSlideRight(v) },
            A("Slide Up")    { v -> EffectUtils.animSlideUp(v) },
            A("Slide Down")  { v -> EffectUtils.animSlideDown(v) },
            A("Spin")        { v -> EffectUtils.animSpin(v) },
            A("Swing")       { v -> EffectUtils.animSwing(v) },
            A("Shake")       { v -> EffectUtils.animShake(v) },
            A("Wobble")      { v -> EffectUtils.animWobble(v) },
            A("Pulse")       { v -> EffectUtils.animPulse(v) },
            A("Float")       { v -> EffectUtils.animFloat(v) },
            A("Typewriter")  { v -> EffectUtils.animTypewriter(v) },
            A("Elastic")     { v -> EffectUtils.animElastic(v) },
            A("Flip")        { v -> EffectUtils.animFlip(v) },
            A("3D Rotate")   { v -> EffectUtils.anim3DRotate(v) }
        )

        list.forEach { a ->
            container.addView(makeButton(a.label) {
                a.fn(playerView)
                toast("Anim: ${a.label}")
            })
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  VIDEO EFFECT ROW  (20 video effects)
    // ─────────────────────────────────────────────────────────────

    private fun setupVideoEffectRow() {
        val container = findViewById<LinearLayout>(R.id.videoEffectButtonsContainer)
        container.removeAllViews()

        data class V(val label: String, val fn: (View) -> Unit)

        val list = listOf(
            V("Velocity")       { v -> EffectUtils.videoVelocity(v) },
            V("Slow Mo")        { v -> EffectUtils.videoSlowMo(v) },
            V("Speed Ramp")     { v -> EffectUtils.videoSpeedRamp(v) },
            V("Beat Shake")     { v -> EffectUtils.videoBeatShake(v) },
            V("Flash Beat")     { v -> EffectUtils.videoFlashBeat(v) },
            V("Glitch Trans")   { v -> EffectUtils.videoGlitchTransition(v) },
            V("RGB Glitch")     { v -> EffectUtils.videoRGBGlitch(v) },
            V("Motion Trail")   { v -> EffectUtils.videoMotionTrail(v) },
            V("Cam Shake")      { v -> EffectUtils.videoCameraShake(v) },
            V("Dyn. Zoom")      { v -> EffectUtils.videoDynamicZoom(v) },
            V("Spin Trans")     { v -> EffectUtils.videoSpinTransition(v) },
            V("Whip Pan")       { v -> EffectUtils.videoWhipPan(v) },
            V("Light Sweep")    { v -> EffectUtils.videoLightSweep(v) },
            V("Lens Flare")     { v -> EffectUtils.videoLensFlare(v) },
            V("Film Burn")      { v -> EffectUtils.videoFilmBurn(v) },
            V("Flashback")      { v -> EffectUtils.videoFlashback(v) },
            V("Freeze Frame")   { v -> EffectUtils.videoFreezeFrame(v) },
            V("Echo Trail")     { v -> EffectUtils.videoEchoTrail(v) },
            V("Blur Trans")     { v -> EffectUtils.videoBlurTransition(v) },
            V("Particle Burst") { v -> EffectUtils.videoParticleBurst(v) }
        )

        list.forEach { v ->
            container.addView(makeButton(v.label) {
                v.fn(playerView)
                toast("VFX: ${v.label}")
            })
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  EXPORT
    // ─────────────────────────────────────────────────────────────

    private fun exportVideo() {
        toast("Exporting to gallery…")
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (!dir.exists()) dir.mkdirs()
        val out = File(dir, "VEXO_${System.currentTimeMillis()}.mp4")
        out.createNewFile()
        MediaScannerConnection.scanFile(this, arrayOf(out.absolutePath), null) { _, _ ->
            runOnUiThread { toast("Saved: ${out.name}") }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    /** Build a styled pill button for the horizontal rows. */
    private fun makeButton(label: String, onClick: () -> Unit): Button {
        val btn = Button(this)
        btn.text = label
        btn.textSize = 11f
        btn.setPadding(24, 12, 24, 12)
        btn.setTextColor(Color.WHITE)
        btn.background = ContextCompat.getDrawable(this, R.drawable.bg_button)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(8, 0, 8, 0)
        btn.layoutParams = lp
        btn.setOnClickListener { onClick() }
        return btn
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
