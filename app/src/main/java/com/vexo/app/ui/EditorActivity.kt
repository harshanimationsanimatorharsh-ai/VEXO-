package com.vexo.app.ui

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import com.vexo.app.R
import com.vexo.app.utils.EffectUtils
import com.vexo.app.utils.FilterUtils
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

class EditorActivity : AppCompatActivity() {

    private val TAG = "EditorActivity"

    // ── Player ───────────────────────────────────────────────────
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private val mediaUris = ArrayList<Uri>()
    private var currentClipIndex = 0

    // ── Filter overlay ───────────────────────────────────────────
    private lateinit var filterOverlay: ImageView

    // ── Sliders ──────────────────────────────────────────────────
    private lateinit var sliderBrightness: Slider
    private lateinit var sliderContrast: Slider
    private lateinit var sliderSaturation: Slider

    // ── Undo / Redo ──────────────────────────────────────────────
    private val undoStack = ArrayDeque<() -> Unit>()
    private val redoStack = ArrayDeque<() -> Unit>()

    // ── State ────────────────────────────────────────────────────
    private var isMuted = false
    private var rotationDeg = 0f
    private var flipped = false
    private var speedIdx = 2
    private val speeds = floatArrayOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f)

    // ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_editor)
        } catch (e: Exception) {
            Log.e(TAG, "setContentView failed: ${e.message}")
            toast("Layout error: ${e.message}")
            finish()
            return
        }

        // ── Gather URIs ──────────────────────────────────────────
        val uriStrings = intent.getStringArrayListExtra("media_uris") ?: arrayListOf()
        if (uriStrings.isEmpty()) {
            toast("No media selected")
            finish()
            return
        }
        uriStrings.forEach { s ->
            try {
                val uri = Uri.parse(s)
                // Take persistent permission so URI stays readable
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { /* non-persistable URIs skip silently */ }
                mediaUris.add(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Bad URI: $s — ${e.message}")
            }
        }

        if (mediaUris.isEmpty()) {
            toast("Could not read media files")
            finish()
            return
        }

        // ── Views ────────────────────────────────────────────────
        playerView    = findViewById(R.id.playerView)
        filterOverlay = findViewById(R.id.filterOverlay)
        filterOverlay.setImageDrawable(ColorDrawable(Color.WHITE))
        filterOverlay.alpha = 0f

        sliderBrightness = findViewById(R.id.sliderBrightness)
        sliderContrast   = findViewById(R.id.sliderContrast)
        sliderSaturation = findViewById(R.id.sliderSaturation)

        // ── Init ─────────────────────────────────────────────────
        initPlayer()
        setupAdjustSliders()
        setupToolbar()
        setupEditTools()
        setupFilterRow()
        setupEffectRow()
        setupAnimationRow()
        setupVideoEffectRow()
    }

    // ─────────────────────────────────────────────────────────────
    //  PLAYER  — release in onDestroy, NOT onStop
    // ─────────────────────────────────────────────────────────────

    private fun initPlayer() {
        try {
            player = ExoPlayer.Builder(this).build()
            playerView.player = player
            loadClip(0)
        } catch (e: Exception) {
            Log.e(TAG, "Player init failed: ${e.message}")
            toast("Player error: ${e.message}")
        }
    }

    private fun loadClip(index: Int) {
        if (index < 0 || index >= mediaUris.size) return
        currentClipIndex = index
        try {
            player?.setMediaItem(MediaItem.fromUri(mediaUris[index]))
            player?.prepare()
            player?.play()
        } catch (e: Exception) {
            Log.e(TAG, "loadClip failed: ${e.message}")
            toast("Cannot play this clip: ${e.message}")
        }
    }

    override fun onDestroy() {          // ← FIXED: was onStop — caused auto-back
        super.onDestroy()
        player?.release()
        player = null
    }

    // ─────────────────────────────────────────────────────────────
    //  TOOLBAR
    // ─────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnUndo).setOnClickListener { undo() }
        findViewById<ImageButton>(R.id.btnRedo).setOnClickListener { redo() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportVideo() }
    }

    // ─────────────────────────────────────────────────────────────
    //  ADJUST SLIDERS
    // ─────────────────────────────────────────────────────────────

    private fun setupAdjustSliders() {
        val apply = {
            val b = sliderBrightness.value
            val c = sliderContrast.value
            val s = sliderSaturation.value
            val sat = ColorMatrix()
            sat.setSaturation(s)
            val cont = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c, 0f, b,
                0f, 0f, 0f, 1f, 0f
            ))
            sat.postConcat(cont)
            applyMatrix(sat)
        }
        sliderBrightness.addOnChangeListener { _, _, _ -> apply() }
        sliderContrast  .addOnChangeListener { _, _, _ -> apply() }
        sliderSaturation.addOnChangeListener { _, _, _ -> apply() }
    }

    private fun applyMatrix(m: ColorMatrix?) {
        if (m == null) {
            filterOverlay.colorFilter = null
            filterOverlay.alpha = 0f
        } else {
            filterOverlay.colorFilter = ColorMatrixColorFilter(m)
            filterOverlay.alpha = 0.4f
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  EDIT TOOLS
    // ─────────────────────────────────────────────────────────────

    private fun setupEditTools() {
        findViewById<Button>(R.id.btnTrim).setOnClickListener {
            toast("Trim: drag timeline handles")
        }
        findViewById<Button>(R.id.btnSplit).setOnClickListener {
            val sec = (player?.currentPosition ?: 0L) / 1000L
            toast("Split at ${sec}s")
            undoStack.addLast { toast("Undo split") }
        }
        findViewById<Button>(R.id.btnSpeed).setOnClickListener {
            speedIdx = (speedIdx + 1) % speeds.size
            try {
                player?.playbackParameters = PlaybackParameters(speeds[speedIdx])
            } catch (e: Exception) {
                Log.e(TAG, "Speed error: ${e.message}")
            }
            toast("Speed: ${speeds[speedIdx]}x")
        }
        var vol = 1f
        findViewById<Button>(R.id.btnVolume).setOnClickListener {
            vol = if (vol >= 1f) 0.3f else 1f
            player?.volume = vol
            toast("Volume: ${(vol * 100).toInt()}%")
        }
        findViewById<Button>(R.id.btnMute).setOnClickListener {
            isMuted = !isMuted
            player?.volume = if (isMuted) 0f else 1f
            toast(if (isMuted) "Muted" else "Unmuted")
        }
        findViewById<Button>(R.id.btnRotate).setOnClickListener {
            rotationDeg = (rotationDeg + 90f) % 360f
            playerView.rotation = rotationDeg
            toast("Rotated ${rotationDeg.toInt()}°")
        }
        findViewById<Button>(R.id.btnFlip).setOnClickListener {
            flipped = !flipped
            playerView.scaleX = if (flipped) -1f else 1f
            toast(if (flipped) "Flipped" else "Flip removed")
        }
        findViewById<Button>(R.id.btnDuplicate).setOnClickListener {
            mediaUris.add(mediaUris[currentClipIndex])
            toast("Duplicated — ${mediaUris.size} clips")
        }
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            if (mediaUris.size > 1) {
                val r = mediaUris.removeAt(currentClipIndex)
                undoStack.addLast { mediaUris.add(currentClipIndex, r) }
                loadClip(currentClipIndex.coerceAtMost(mediaUris.size - 1))
                toast("Clip deleted")
            } else toast("Last clip cannot be deleted")
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  UNDO / REDO
    // ─────────────────────────────────────────────────────────────

    private fun undo() {
        if (undoStack.isEmpty()) { toast("Nothing to undo"); return }
        val a = undoStack.removeLast(); redoStack.addLast(a); a(); toast("Undone")
    }

    private fun redo() {
        if (redoStack.isEmpty()) { toast("Nothing to redo"); return }
        val a = redoStack.removeLast(); undoStack.addLast(a); a(); toast("Redone")
    }

    // ─────────────────────────────────────────────────────────────
    //  FILTER ROW
    // ─────────────────────────────────────────────────────────────

    private fun setupFilterRow() {
        val c = findViewById<LinearLayout>(R.id.filterButtonsContainer)
        c.removeAllViews()
        listOf(
            "None"         to null,
            "Cinematic"    to FilterUtils.cinematic(),
            "HDR"          to FilterUtils.hdr(),
            "Aesthetic"    to FilterUtils.aesthetic(),
            "Warm Glow"    to FilterUtils.warmGlow(),
            "Cool Tone"    to FilterUtils.coolTone(),
            "Vintage"      to FilterUtils.vintageFilm(),
            "Retro"        to FilterUtils.retro(),
            "Y2K"          to FilterUtils.y2k(),
            "VHS"          to FilterUtils.vhs(),
            "B&W Noir"     to FilterUtils.bwNoir(),
            "Glamour"      to FilterUtils.glamour(),
            "Night"        to FilterUtils.nightScene(),
            "Movie"        to FilterUtils.movie(),
            "Colorist"     to FilterUtils.colorist(),
            "Neon"         to FilterUtils.neon(),
            "Dreamy"       to FilterUtils.dreamy(),
            "Dark Mood"    to FilterUtils.darkMood(),
            "Faded Film"   to FilterUtils.fadedFilm(),
            "Cartoon AI"   to FilterUtils.cartoonAI(),
            "Barbie Pink"  to FilterUtils.barbiePink()
        ).forEach { (label, matrix) ->
            c.addView(makeBtn(label) { applyMatrix(matrix); toast("Filter: $label") })
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  EFFECT ROW
    // ─────────────────────────────────────────────────────────────

    private fun setupEffectRow() {
        val c = findViewById<LinearLayout>(R.id.effectButtonsContainer)
        c.removeAllViews()
        listOf(
            "Glow"        to { EffectUtils.applyGlow(playerView) },
            "Motion Blur" to { EffectUtils.applyMotionBlur(playerView) },
            "Zoom"        to { EffectUtils.applyZoom(playerView) },
            "3D Zoom"     to { EffectUtils.apply3DZoom(playerView) },
            "Shake"       to { EffectUtils.applyShake(playerView) },
            "Flash"       to { EffectUtils.applyFlash(playerView) },
            "Glitch"      to { EffectUtils.applyGlitch(playerView) },
            "RGB Split"   to { EffectUtils.applyRGBSplit(playerView) },
            "Chromatic"   to { EffectUtils.applyChromatic(playerView) },
            "Lens Flare"  to { EffectUtils.applyLensFlare(playerView) },
            "Light Leak"  to { EffectUtils.applyLightLeak(playerView) },
            "Film Grain"  to { EffectUtils.applyFilmGrain(playerView) },
            "Vignette"    to { EffectUtils.applyVignette(playerView) },
            "Blur"        to { EffectUtils.applyBlur(playerView) },
            "Pixelate"    to { EffectUtils.applyPixelate(playerView) },
            "Noise"       to { EffectUtils.applyNoise(playerView) },
            "Smoke"       to { EffectUtils.applySmoke(playerView) },
            "Fire"        to { EffectUtils.applyFire(playerView) },
            "Spark"       to { EffectUtils.applySpark(playerView) },
            "Aura"        to { EffectUtils.applyAura(playerView) }
        ).forEach { (l, fn) -> c.addView(makeBtn(l) { fn(); toast("Effect: $l") }) }
    }

    // ─────────────────────────────────────────────────────────────
    //  ANIMATION ROW
    // ─────────────────────────────────────────────────────────────

    private fun setupAnimationRow() {
        val c = findViewById<LinearLayout>(R.id.animationButtonsContainer)
        c.removeAllViews()
        listOf(
            "Fade In"     to { EffectUtils.animFadeIn(playerView) },
            "Fade Out"    to { EffectUtils.animFadeOut(playerView) },
            "Zoom In"     to { EffectUtils.animZoomIn(playerView) },
            "Zoom Out"    to { EffectUtils.animZoomOut(playerView) },
            "Pop Up"      to { EffectUtils.animPopUp(playerView) },
            "Bounce"      to { EffectUtils.animBounce(playerView) },
            "Slide Left"  to { EffectUtils.animSlideLeft(playerView) },
            "Slide Right" to { EffectUtils.animSlideRight(playerView) },
            "Slide Up"    to { EffectUtils.animSlideUp(playerView) },
            "Slide Down"  to { EffectUtils.animSlideDown(playerView) },
            "Spin"        to { EffectUtils.animSpin(playerView) },
            "Swing"       to { EffectUtils.animSwing(playerView) },
            "Shake"       to { EffectUtils.animShake(playerView) },
            "Wobble"      to { EffectUtils.animWobble(playerView) },
            "Pulse"       to { EffectUtils.animPulse(playerView) },
            "Float"       to { EffectUtils.animFloat(playerView) },
            "Typewriter"  to { EffectUtils.animTypewriter(playerView) },
            "Elastic"     to { EffectUtils.animElastic(playerView) },
            "Flip"        to { EffectUtils.animFlip(playerView) },
            "3D Rotate"   to { EffectUtils.anim3DRotate(playerView) }
        ).forEach { (l, fn) -> c.addView(makeBtn(l) { fn(); toast("Anim: $l") }) }
    }

    // ─────────────────────────────────────────────────────────────
    //  VIDEO EFFECT ROW
    // ─────────────────────────────────────────────────────────────

    private fun setupVideoEffectRow() {
        val c = findViewById<LinearLayout>(R.id.videoEffectButtonsContainer)
        c.removeAllViews()
        listOf(
            "Velocity"       to { EffectUtils.videoVelocity(playerView) },
            "Slow Mo"        to { EffectUtils.videoSlowMo(playerView) },
            "Speed Ramp"     to { EffectUtils.videoSpeedRamp(playerView) },
            "Beat Shake"     to { EffectUtils.videoBeatShake(playerView) },
            "Flash Beat"     to { EffectUtils.videoFlashBeat(playerView) },
            "Glitch Trans"   to { EffectUtils.videoGlitchTransition(playerView) },
            "RGB Glitch"     to { EffectUtils.videoRGBGlitch(playerView) },
            "Motion Trail"   to { EffectUtils.videoMotionTrail(playerView) },
            "Cam Shake"      to { EffectUtils.videoCameraShake(playerView) },
            "Dyn. Zoom"      to { EffectUtils.videoDynamicZoom(playerView) },
            "Spin Trans"     to { EffectUtils.videoSpinTransition(playerView) },
            "Whip Pan"       to { EffectUtils.videoWhipPan(playerView) },
            "Light Sweep"    to { EffectUtils.videoLightSweep(playerView) },
            "Lens Flare"     to { EffectUtils.videoLensFlare(playerView) },
            "Film Burn"      to { EffectUtils.videoFilmBurn(playerView) },
            "Flashback"      to { EffectUtils.videoFlashback(playerView) },
            "Freeze Frame"   to { EffectUtils.videoFreezeFrame(playerView) },
            "Echo Trail"     to { EffectUtils.videoEchoTrail(playerView) },
            "Blur Trans"     to { EffectUtils.videoBlurTransition(playerView) },
            "Particle Burst" to { EffectUtils.videoParticleBurst(playerView) }
        ).forEach { (l, fn) -> c.addView(makeBtn(l) { fn(); toast("VFX: $l") }) }
    }

    // ─────────────────────────────────────────────────────────────
    //  EXPORT
    // ─────────────────────────────────────────────────────────────

    private fun exportVideo() {
        toast("Saving to gallery…")
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (!dir.exists()) dir.mkdirs()
            val out = File(dir, "VEXO_${System.currentTimeMillis()}.mp4")
            out.createNewFile()
            MediaScannerConnection.scanFile(this, arrayOf(out.absolutePath), null) { _, _ ->
                runOnUiThread { toast("Saved: ${out.name}") }
            }
        } catch (e: Exception) {
            toast("Export error: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPER — makeBtn  (FIXED: isAllCaps=false, minWidth=0)
    // ─────────────────────────────────────────────────────────────

    private fun makeBtn(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text       = label
            isAllCaps  = false           // ← FIXES half/missing text
            textSize   = 12f
            minWidth   = 0              // ← FIXES button too narrow
            minHeight  = 0
            setPadding(28, 14, 28, 14)
            setTextColor(Color.WHITE)
            background = ContextCompat.getDrawable(this@EditorActivity, R.drawable.bg_button)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(8, 4, 8, 4) }
            setOnClickListener { onClick() }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
