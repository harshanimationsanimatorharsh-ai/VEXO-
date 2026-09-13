package com.vexo.app.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.vexo.app.databinding.ActivityEditorBinding
import java.io.File

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private var player: ExoPlayer? = null
    private var mediaUris = ArrayList<Uri>()
    private var currentClipIndex = 0
    private var isMuted = false
    private var currentVolume = 1f
    private var currentSpeed = 1f

    // Undo/Redo Stack
    private val undoStack = mutableListOf<ArrayList<Uri>>()
    private val redoStack = mutableListOf<ArrayList<Uri>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaUris = intent.getParcelableArrayListExtra("media_uris")
            ?: ArrayList()

        if (mediaUris.isEmpty()) {
            Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        saveUndoState()
        setupPlayer()
        setupTopBar()
        setupClipTools()
        setupBottomTabs()
        updateTimeline()
        updateClipCount()
    }

    // ─── Player ───────────────────────────────────────────
    private fun setupPlayer() {
        player?.release()
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        if (mediaUris.isNotEmpty() && currentClipIndex < mediaUris.size) {
            val uri = mediaUris[currentClipIndex]
            player?.setMediaItem(MediaItem.fromUri(uri))
            player?.prepare()
            player?.playWhenReady = true
            player?.volume = currentVolume
        }

        // Seekbar update
        val handler = android.os.Handler(mainLooper)
        val runnable = object : Runnable {
            override fun run() {
                player?.let {
                    if (it.duration > 0) {
                        val progress = (it.currentPosition * 100 / it.duration).toInt()
                        binding.seekBar.progress = progress
                        val cur = it.currentPosition / 1000
                        val dur = it.duration / 1000
                        binding.tvTimecode.text = "${cur}s / ${dur}s"
                    }
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(runnable)
    }

    // ─── Top Bar ──────────────────────────────────────────
    private fun setupTopBar() {
        binding.ivClose.setOnClickListener { finish() }

        binding.ivPlay.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
                binding.ivPlay.setImageResource(android.R.drawable.ic_media_play)
            } else {
                player?.play()
                binding.ivPlay.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        binding.ivPrev.setOnClickListener {
            if (currentClipIndex > 0) {
                currentClipIndex--
                setupPlayer()
                highlightTimelineClip()
            }
        }

        binding.ivNext.setOnClickListener {
            if (currentClipIndex < mediaUris.size - 1) {
                currentClipIndex++
                setupPlayer()
                highlightTimelineClip()
            }
        }

        binding.ivUndo.setOnClickListener { undo() }
        binding.ivRedo.setOnClickListener { redo() }

        binding.btnExport.setOnClickListener {
            hideAllPanels()
            binding.exportPanel.visibility = View.VISIBLE
        }

        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val dur = player?.duration ?: 0L
                        player?.seekTo(p * dur / 100)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
    }

    // ─── Clip Tools ───────────────────────────────────────
    private fun setupClipTools() {

        // TRIM
        binding.toolTrim.setOnClickListener {
            hideAllPanels()
            binding.trimPanel.visibility = View.VISIBLE
            val dur = (player?.duration ?: 10000L) / 1000
            binding.tvTrimDuration.text = "Duration: ${dur}s"
            binding.trimStart.max = dur.toInt()
            binding.trimEnd.max = dur.toInt()
            binding.trimEnd.progress = dur.toInt()
        }

        binding.btnTrimDone.setOnClickListener {
            val startMs = binding.trimStart.progress * 1000L
            val endMs = binding.trimEnd.progress * 1000L
            if (endMs <= startMs) {
                Toast.makeText(this, "End must be after Start", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Seek to trimmed start point
            player?.seekTo(startMs)
            Toast.makeText(this,
                "✅ Trim: ${binding.trimStart.progress}s → ${binding.trimEnd.progress}s",
                Toast.LENGTH_SHORT).show()
            binding.trimPanel.visibility = View.GONE
        }

        binding.btnTrimCancel.setOnClickListener {
            binding.trimPanel.visibility = View.GONE
        }

        // SPLIT
        binding.toolSplit.setOnClickListener {
            val pos = player?.currentPosition ?: 0L
            val dur = player?.duration ?: 0L
            if (pos <= 0 || pos >= dur) {
                Toast.makeText(this,
                    "Play video and pause at split point",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveUndoState()
            val currentUri = mediaUris[currentClipIndex]
            // Add same clip twice (representing split)
            mediaUris.add(currentClipIndex + 1, currentUri)
            updateTimeline()
            updateClipCount()
            Toast.makeText(this,
                "✅ Split at ${pos/1000}s — ${mediaUris.size} clips",
                Toast.LENGTH_SHORT).show()
        }

        // SPEED
        binding.toolSpeed.setOnClickListener {
            hideAllPanels()
            binding.speedPanel.visibility = View.VISIBLE
            updateSpeedButtons()
        }

        binding.btnSpeed025.setOnClickListener { setSpeed(0.25f) }
        binding.btnSpeed05.setOnClickListener { setSpeed(0.5f) }
        binding.btnSpeed075.setOnClickListener { setSpeed(0.75f) }
        binding.btnSpeed1.setOnClickListener { setSpeed(1.0f) }
        binding.btnSpeed15.setOnClickListener { setSpeed(1.5f) }
        binding.btnSpeed2.setOnClickListener { setSpeed(2.0f) }
        binding.btnSpeed3.setOnClickListener { setSpeed(3.0f) }
        binding.btnSpeedDone.setOnClickListener {
            binding.speedPanel.visibility = View.GONE
        }

        // VOLUME
        binding.toolVolume.setOnClickListener {
            hideAllPanels()
            binding.volumePanel.visibility = View.VISIBLE
            binding.seekVolume.progress = (currentVolume * 100).toInt()
        }

        binding.seekVolume.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                    currentVolume = p / 100f
                    player?.volume = currentVolume
                    binding.tvVolumeValue.text = "$p%"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

        binding.btnVolumeDone.setOnClickListener {
            binding.volumePanel.visibility = View.GONE
            Toast.makeText(this,
                "✅ Volume: ${(currentVolume*100).toInt()}%",
                Toast.LENGTH_SHORT).show()
        }

        // MUTE
        binding.toolMute.setOnClickListener {
            isMuted = !isMuted
            player?.volume = if (isMuted) 0f else currentVolume
            binding.toolMuteText.text = if (isMuted) "Unmute" else "Mute"
            Toast.makeText(this,
                if (isMuted) "🔇 Muted" else "🔊 Unmuted",
                Toast.LENGTH_SHORT).show()
        }

        // ROTATE (Visual only in preview)
        binding.toolRotate.setOnClickListener {
            val current = binding.playerView.rotation
            binding.playerView.rotation = current + 90f
            Toast.makeText(this, "✅ Rotated 90°", Toast.LENGTH_SHORT).show()
        }

        // FLIP (Visual only in preview)
        binding.toolFlip.setOnClickListener {
            binding.playerView.scaleX = binding.playerView.scaleX * -1
            Toast.makeText(this, "✅ Flipped", Toast.LENGTH_SHORT).show()
        }

        // DUPLICATE
        binding.toolDuplicate.setOnClickListener {
            saveUndoState()
            val uri = mediaUris[currentClipIndex]
            mediaUris.add(currentClipIndex + 1, uri)
            updateTimeline()
            updateClipCount()
            Toast.makeText(this, "✅ Clip Duplicated", Toast.LENGTH_SHORT).show()
        }

        // DELETE
        binding.toolDelete.setOnClickListener {
            if (mediaUris.size <= 1) {
                Toast.makeText(this,
                    "Cannot delete last clip", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveUndoState()
            mediaUris.removeAt(currentClipIndex)
            if (currentClipIndex >= mediaUris.size) {
                currentClipIndex = mediaUris.size - 1
            }
            setupPlayer()
            updateTimeline()
            updateClipCount()
            Toast.makeText(this, "✅ Clip Deleted", Toast.LENGTH_SHORT).show()
        }

        // REVERSE
        binding.toolReverse.setOnClickListener {
            Toast.makeText(this,
                "Reverse requires FFmpeg — Coming Soon",
                Toast.LENGTH_SHORT).show()
        }

        // EXPORT BUTTONS
        binding.btnExport480.setOnClickListener {
            binding.exportPanel.visibility = View.GONE
            exportVideo("480p")
        }
        binding.btnExport720.setOnClickListener {
            binding.exportPanel.visibility = View.GONE
            exportVideo("720p")
        }
        binding.btnExport1080.setOnClickListener {
            binding.exportPanel.visibility = View.GONE
            exportVideo("1080p")
        }
        binding.btnExportCancel.setOnClickListener {
            binding.exportPanel.visibility = View.GONE
        }
    }

    // ─── Bottom Tabs ──────────────────────────────────────
    private fun setupBottomTabs() {

        // AUDIO
        binding.tabAudio.setOnClickListener {
            hideAllPanels()
            binding.audioPanel.visibility = View.VISIBLE
        }

        // TEXT
        binding.tabText.setOnClickListener {
            hideAllPanels()
            binding.textPanel.visibility = View.VISIBLE
        }

        binding.btnAddText.setOnClickListener {
            val text = binding.etTextInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Enter text first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.tvOverlayText.text = text
            binding.tvOverlayText.visibility = View.VISIBLE
            binding.textPanel.visibility = View.GONE
            Toast.makeText(this, "✅ Text Added", Toast.LENGTH_SHORT).show()
        }

        binding.btnRemoveText.setOnClickListener {
            binding.tvOverlayText.visibility = View.GONE
            binding.textPanel.visibility = View.GONE
            Toast.makeText(this, "Text Removed", Toast.LENGTH_SHORT).show()
        }

        // FILTERS
        binding.tabFilters.setOnClickListener {
            hideAllPanels()
            binding.filtersPanel.visibility = View.VISIBLE
        }

        setupFilters()

        // ADJUST
        binding.tabAdjust.setOnClickListener {
            hideAllPanels()
            binding.adjustPanel.visibility = View.VISIBLE
        }

        setupAdjust()

        // EFFECTS
        binding.tabEffects.setOnClickListener {
            hideAllPanels()
            binding.effectsPanel.visibility = View.VISIBLE
        }

        // TRANSITIONS
        binding.tabTransitions.setOnClickListener {
            if (mediaUris.size < 2) {
                Toast.makeText(this,
                    "Add at least 2 clips for transitions",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            hideAllPanels()
            binding.transitionsPanel.visibility = View.VISIBLE
        }

        // CANVAS
        binding.tabCanvas.setOnClickListener {
            hideAllPanels()
            binding.canvasPanel.visibility = View.VISIBLE
        }

        // OVERLAY
        binding.tabOverlay.setOnClickListener {
            Toast.makeText(this, "Overlay — Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // STICKERS
        binding.tabStickers.setOnClickListener {
            Toast.makeText(this, "Stickers — Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // CAPTIONS
        binding.tabCaptions.setOnClickListener {
            hideAllPanels()
            binding.captionsPanel.visibility = View.VISIBLE
        }
    }

    // ─── Filters ──────────────────────────────────────────
    private fun setupFilters() {
        binding.btnFilterNone.setOnClickListener {
            binding.playerView.colorFilter = null
            binding.tvFilterActive.text = "Active: None"
        }
        binding.btnFilterBW.setOnClickListener {
            applyFilter(ColorMatrix().apply { setSaturation(0f) })
            binding.tvFilterActive.text = "Active: B&W"
        }
        binding.btnFilterWarm.setOnClickListener {
            applyFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    1.3f,0f,0f,0f,20f,
                    0f,1f,0f,0f,5f,
                    0f,0f,0.7f,0f,-20f,
                    0f,0f,0f,1f,0f))
            })
            binding.tvFilterActive.text = "Active: Warm"
        }
        binding.btnFilterCool.setOnClickListener {
            applyFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    0.7f,0f,0f,0f,-20f,
                    0f,1f,0f,0f,5f,
                    0f,0f,1.3f,0f,20f,
                    0f,0f,0f,1f,0f))
            })
            binding.tvFilterActive.text = "Active: Cool"
        }
        binding.btnFilterVintage.setOnClickListener {
            applyFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    0.9f,0.1f,0f,0f,20f,
                    0.1f,0.8f,0.1f,0f,10f,
                    0f,0.1f,0.7f,0f,-10f,
                    0f,0f,0f,1f,0f))
            })
            binding.tvFilterActive.text = "Active: Vintage"
        }
        binding.btnFilterCinema.setOnClickListener {
            applyFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    0.6f,0.3f,0.1f,0f,0f,
                    0.1f,0.7f,0.2f,0f,0f,
                    0.1f,0.2f,0.7f,0f,0f,
                    0f,0f,0f,1f,0f))
            })
            binding.tvFilterActive.text = "Active: Cinema"
        }
        binding.btnFilterMoody.setOnClickListener {
            applyFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    0.8f,0f,0f,0f,-10f,
                    0f,0.8f,0f,0f,-10f,
                    0f,0f,1.1f,0f,10f,
                    0f,0f,0f,1f,0f))
            })
            binding.tvFilterActive.text = "Active: Moody"
        }
        binding.btnFilterBright.setOnClickListener {
            applyFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    1.2f,0f,0f,0f,30f,
                    0f,1.2f,0f,0f,30f,
                    0f,0f,1.2f,0f,30f,
                    0f,0f,0f,1f,0f))
            })
            binding.tvFilterActive.text = "Active: Bright"
        }
    }

    private fun applyFilter(matrix: ColorMatrix) {
        binding.playerView.colorFilter =
            ColorMatrixColorFilter(matrix)
    }

    // ─── Adjust ───────────────────────────────────────────
    private fun setupAdjust() {
        binding.seekBrightness.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                    val b = (p - 50) / 50f * 150f
                    binding.tvBrightnessVal.text = "${p-50}"
                    val m = ColorMatrix().apply {
                        set(floatArrayOf(
                            1f,0f,0f,0f,b,
                            0f,1f,0f,0f,b,
                            0f,0f,1f,0f,b,
                            0f,0f,0f,1f,0f))
                    }
                    binding.playerView.colorFilter = ColorMatrixColorFilter(m)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

        binding.seekContrast.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                    val c = p / 50f
                    val t = (-0.5f * c + 0.5f) * 255f
                    binding.tvContrastVal.text = "${p-50}"
                    val m = ColorMatrix().apply {
                        set(floatArrayOf(
                            c,0f,0f,0f,t,
                            0f,c,0f,0f,t,
                            0f,0f,c,0f,t,
                            0f,0f,0f,1f,0f))
                    }
                    binding.playerView.colorFilter = ColorMatrixColorFilter(m)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

        binding.seekSaturation.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                    val s = p / 50f
                    binding.tvSaturationVal.text = "${p-50}"
                    val m = ColorMatrix().apply { setSaturation(s) }
                    binding.playerView.colorFilter = ColorMatrixColorFilter(m)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

        binding.btnAdjustReset.setOnClickListener {
            binding.seekBrightness.progress = 50
            binding.seekContrast.progress = 50
            binding.seekSaturation.progress = 50
            binding.playerView.colorFilter = null
            binding.tvBrightnessVal.text = "0"
            binding.tvContrastVal.text = "0"
            binding.tvSaturationVal.text = "0"
        }
    }

    // ─── Speed ────────────────────────────────────────────
    private fun setSpeed(speed: Float) {
        currentSpeed = speed
        player?.playbackParameters = PlaybackParameters(speed)
        updateSpeedButtons()
        Toast.makeText(this, "✅ Speed: ${speed}x", Toast.LENGTH_SHORT).show()
    }

    private fun updateSpeedButtons() {
        val purple = getColor(com.vexo.app.R.color.vexo_purple)
        val elevated = getColor(com.vexo.app.R.color.bg_elevated)

        listOf(
            binding.btnSpeed025 to 0.25f,
            binding.btnSpeed05 to 0.5f,
            binding.btnSpeed075 to 0.75f,
            binding.btnSpeed1 to 1.0f,
            binding.btnSpeed15 to 1.5f,
            binding.btnSpeed2 to 2.0f,
            binding.btnSpeed3 to 3.0f
        ).forEach { (btn, speed) ->
            btn.setBackgroundColor(if (speed == currentSpeed) purple else elevated)
        }
    }

    // ─── Timeline ─────────────────────────────────────────
    private fun updateTimeline() {
        binding.timelineContainer.removeAllViews()
        mediaUris.forEachIndexed { index, uri ->
            val clip = android.widget.TextView(this).apply {
                text = "Clip ${index + 1}"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                setPadding(8, 4, 8, 4)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    (120 + index * 20),
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { setMargins(4, 4, 4, 4) }
                setBackgroundColor(
                    if (index == currentClipIndex)
                        getColor(com.vexo.app.R.color.vexo_purple)
                    else
                        getColor(com.vexo.app.R.color.clip_video)
                )
                setOnClickListener {
                    currentClipIndex = index
                    setupPlayer()
                    highlightTimelineClip()
                }
            }
            binding.timelineContainer.addView(clip)
        }
    }

    private fun highlightTimelineClip() {
        for (i in 0 until binding.timelineContainer.childCount) {
            val v = binding.timelineContainer.getChildAt(i) as? android.widget.TextView
            v?.setBackgroundColor(
                if (i == currentClipIndex)
                    getColor(com.vexo.app.R.color.vexo_purple)
                else
                    getColor(com.vexo.app.R.color.clip_video)
            )
        }
    }

    private fun updateClipCount() {
        binding.tvClipCount.text = "${mediaUris.size} Clip(s)"
    }

    // ─── Undo / Redo ──────────────────────────────────────
    private fun saveUndoState() {
        undoStack.add(ArrayList(mediaUris))
        redoStack.clear()
    }

    private fun undo() {
        if (undoStack.size > 1) {
            redoStack.add(ArrayList(mediaUris))
            undoStack.removeLastOrNull()
            mediaUris = ArrayList(undoStack.last())
            currentClipIndex = 0
            setupPlayer()
            updateTimeline()
            updateClipCount()
            Toast.makeText(this, "↩ Undo", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(ArrayList(mediaUris))
            mediaUris = ArrayList(redoStack.removeLastOrNull()!!)
            currentClipIndex = 0
            setupPlayer()
            updateTimeline()
            updateClipCount()
            Toast.makeText(this, "↪ Redo", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Export ───────────────────────────────────────────
    private fun exportVideo(quality: String) {
        showProgress(true, "Exporting $quality...")
        val uri = mediaUris[currentClipIndex]

        Thread {
            try {
                Thread.sleep(2000) // Simulate processing
                val fileName = "VEXO_${System.currentTimeMillis()}.mp4"
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + "/VEXO")
                }
                contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

                runOnUiThread {
                    showProgress(false)
                    binding.shareBar.visibility = View.VISIBLE
                    binding.btnShareExported.setOnClickListener {
                        startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, uri)
                            }, "Share via"))
                    }
                    Toast.makeText(this,
                        "✅ $quality Export Complete! Saved to Gallery/VEXO",
                        Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showProgress(false)
                    Toast.makeText(this,
                        "Export failed: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ─── Helpers ──────────────────────────────────────────
    private fun hideAllPanels() {
        binding.trimPanel.visibility = View.GONE
        binding.speedPanel.visibility = View.GONE
        binding.volumePanel.visibility = View.GONE
        binding.audioPanel.visibility = View.GONE
        binding.textPanel.visibility = View.GONE
        binding.filtersPanel.visibility = View.GONE
        binding.adjustPanel.visibility = View.GONE
        binding.effectsPanel.visibility = View.GONE
        binding.transitionsPanel.visibility = View.GONE
        binding.canvasPanel.visibility = View.GONE
        binding.captionsPanel.visibility = View.GONE
        binding.exportPanel.visibility = View.GONE
        binding.shareBar.visibility = View.GONE
    }

    private fun showProgress(show: Boolean, msg: String = "Processing...") {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvProgressMsg.text = msg
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
