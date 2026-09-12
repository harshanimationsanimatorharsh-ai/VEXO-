package com.vexo.app.ui

import android.content.ContentValues
import android.content.Intent
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
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.vexo.app.databinding.ActivityEditorBinding
import java.io.File

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private var player: ExoPlayer? = null
    private var mediaUris = ArrayList<Uri>()
    private var currentUri: Uri? = null
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaUris = intent.getParcelableArrayListExtra("media_uris") ?: ArrayList()
        if (mediaUris.isNotEmpty()) {
            currentUri = mediaUris[0]
            setupPlayer()
        }

        setupTopBar()
        setupClipTools()
        setupBottomTabs()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        currentUri?.let {
            player?.setMediaItem(MediaItem.fromUri(it))
            player?.prepare()
            player?.playWhenReady = true
        }
    }

    private fun setupTopBar() {
        binding.ivClose.setOnClickListener { finish() }

        binding.ivPlay.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
                binding.ivPlay.setImageResource(
                    android.R.drawable.ic_media_play)
            } else {
                player?.play()
                binding.ivPlay.setImageResource(
                    android.R.drawable.ic_media_pause)
            }
        }

        binding.btnExport.setOnClickListener {
            binding.exportPanel.visibility = View.VISIBLE
        }

        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val dur = player?.duration ?: 0L
                        player?.seekTo(p * dur / 100)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
    }

    private fun setupClipTools() {
        binding.toolTrim.setOnClickListener {
            hideAllPanels()
            binding.trimPanel.visibility = View.VISIBLE
        }
        binding.toolSplit.setOnClickListener {
            val pos = player?.currentPosition ?: 0L
            Toast.makeText(this, "Split at ${pos/1000}s", Toast.LENGTH_SHORT).show()
        }
        binding.toolSpeed.setOnClickListener {
            hideAllPanels()
            binding.speedPanel.visibility = View.VISIBLE
        }
        binding.toolVolume.setOnClickListener {
            hideAllPanels()
            binding.volumePanel.visibility = View.VISIBLE
        }
        binding.toolRotate.setOnClickListener {
            applyFFmpeg("rotate")
        }
        binding.toolFlip.setOnClickListener {
            applyFFmpeg("flip")
        }
        binding.toolReverse.setOnClickListener {
            applyFFmpeg("reverse")
        }
        binding.toolDuplicate.setOnClickListener {
            currentUri?.let { mediaUris.add(it) }
            Toast.makeText(this, "Clip duplicated", Toast.LENGTH_SHORT).show()
        }
        binding.toolDelete.setOnClickListener {
            if (mediaUris.isNotEmpty()) mediaUris.removeAt(0)
            Toast.makeText(this, "Clip deleted", Toast.LENGTH_SHORT).show()
        }
        binding.toolMute.setOnClickListener {
            isMuted = !isMuted
            player?.volume = if (isMuted) 0f else 1f
            Toast.makeText(this,
                if (isMuted) "Muted" else "Unmuted",
                Toast.LENGTH_SHORT).show()
        }

        // Trim
        binding.btnTrimDone.setOnClickListener {
            applyTrim(
                binding.trimStart.progress * 1000L,
                binding.trimEnd.progress * 1000L
            )
            binding.trimPanel.visibility = View.GONE
        }
        binding.btnTrimCancel.setOnClickListener {
            binding.trimPanel.visibility = View.GONE
        }

        // Speed
        binding.btnSpeed025.setOnClickListener { setSpeed(0.25f) }
        binding.btnSpeed05.setOnClickListener { setSpeed(0.5f) }
        binding.btnSpeed1.setOnClickListener { setSpeed(1.0f) }
        binding.btnSpeed15.setOnClickListener { setSpeed(1.5f) }
        binding.btnSpeed2.setOnClickListener { setSpeed(2.0f) }
        binding.btnSpeed3.setOnClickListener { setSpeed(3.0f) }
        binding.btnSpeedDone.setOnClickListener {
            binding.speedPanel.visibility = View.GONE
        }

        // Volume
        binding.seekVolume.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    sb: SeekBar?, p: Int, fromUser: Boolean) {
                    player?.volume = p / 100f
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        binding.btnVolumeDone.setOnClickListener {
            binding.volumePanel.visibility = View.GONE
        }

        // Export
        binding.btnExport480.setOnClickListener {
            binding.exportPanel.visibility = View.GONE
            exportVideo("854x480")
        }
        binding.btnExport720.setOnClickListener {
            binding.exportPanel.visibility = View.GONE
            exportVideo("1280x720")
        }
        binding.btnExport1080.setOnClickListener {
            binding.exportPanel.visibility = View.GONE
            exportVideo("1920x1080")
        }
        binding.btnExportCancel.setOnClickListener {
            binding.exportPanel.visibility = View.GONE
        }
    }

    private fun setupBottomTabs() {
        binding.tabAudio.setOnClickListener {
            hideAllPanels()
            binding.audioPanel.visibility = View.VISIBLE
        }
        binding.tabText.setOnClickListener {
            hideAllPanels()
            binding.textPanel.visibility = View.VISIBLE
        }
        binding.tabFilters.setOnClickListener {
            hideAllPanels()
            binding.filtersPanel.visibility = View.VISIBLE
        }
        binding.tabAdjust.setOnClickListener {
            hideAllPanels()
            binding.adjustPanel.visibility = View.VISIBLE
        }
        binding.tabEffects.setOnClickListener {
            Toast.makeText(this, "Effects Coming Soon", Toast.LENGTH_SHORT).show()
        }
        binding.tabTransitions.setOnClickListener {
            Toast.makeText(this, "Transitions Coming Soon", Toast.LENGTH_SHORT).show()
        }
        binding.tabOverlay.setOnClickListener {
            Toast.makeText(this, "Overlay Coming Soon", Toast.LENGTH_SHORT).show()
        }
        binding.tabStickers.setOnClickListener {
            Toast.makeText(this, "Stickers Coming Soon", Toast.LENGTH_SHORT).show()
        }
        binding.tabCaptions.setOnClickListener {
            Toast.makeText(this, "Captions Coming Soon", Toast.LENGTH_SHORT).show()
        }
        binding.tabCanvas.setOnClickListener {
            Toast.makeText(this, "Canvas Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Adjust seekbars
        binding.seekBrightness.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {}
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
    }

    private fun hideAllPanels() {
        binding.trimPanel.visibility = View.GONE
        binding.speedPanel.visibility = View.GONE
        binding.volumePanel.visibility = View.GONE
        binding.audioPanel.visibility = View.GONE
        binding.textPanel.visibility = View.GONE
        binding.filtersPanel.visibility = View.GONE
        binding.adjustPanel.visibility = View.GONE
        binding.exportPanel.visibility = View.GONE
    }

    private fun setSpeed(speed: Float) {
        player?.playbackParameters = PlaybackParameters(speed)
        Toast.makeText(this, "${speed}x", Toast.LENGTH_SHORT).show()
        binding.speedPanel.visibility = View.GONE
    }

    private fun applyTrim(startMs: Long, endMs: Long) {
        val input = getPath(currentUri!!) ?: return
        val out = File(cacheDir, "trim_${System.currentTimeMillis()}.mp4")
        val cmd = "-i \"$input\" -ss ${startMs/1000.0} " +
                "-t ${(endMs-startMs)/1000.0} -c copy \"${out.absolutePath}\""
        showProgress(true)
        FFmpegKit.executeAsync(cmd) { session ->
            runOnUiThread {
                showProgress(false)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    currentUri = Uri.fromFile(out)
                    player?.setMediaItem(MediaItem.fromUri(currentUri!!))
                    player?.prepare()
                    Toast.makeText(this, "Trim Done!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Trim Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyFFmpeg(type: String) {
        val input = getPath(currentUri!!) ?: return
        val out = File(cacheDir, "${type}_${System.currentTimeMillis()}.mp4")
        val cmd = when (type) {
            "rotate" -> "-i \"$input\" -vf transpose=1 \"${out.absolutePath}\""
            "flip" -> "-i \"$input\" -vf hflip \"${out.absolutePath}\""
            "reverse" -> "-i \"$input\" -vf reverse -af areverse \"${out.absolutePath}\""
            else -> return
        }
        showProgress(true)
        FFmpegKit.executeAsync(cmd) { session ->
            runOnUiThread {
                showProgress(false)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    currentUri = Uri.fromFile(out)
                    player?.setMediaItem(MediaItem.fromUri(currentUri!!))
                    player?.prepare()
                    Toast.makeText(this, "$type Done!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "$type Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun exportVideo(res: String) {
        val input = getPath(currentUri!!) ?: return
        val out = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES),
            "VEXO_${System.currentTimeMillis()}.mp4"
        )
        val cmd = "-i \"$input\" -vf scale=$res -c:a aac \"${out.absolutePath}\""
        showProgress(true)
        FFmpegKit.executeAsync(cmd) { session ->
            runOnUiThread {
                showProgress(false)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    saveToGallery(out)
                    binding.shareBar.visibility = View.VISIBLE
                    binding.btnShareExported.setOnClickListener {
                        startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out))
                            }, "Share via"))
                    }
                    Toast.makeText(this,
                        "✅ Exported to Gallery!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveToGallery(file: File) {
        contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES)
            })
    }

    private fun showProgress(show: Boolean) {
        binding.progressOverlay.visibility =
            if (show) View.VISIBLE else View.GONE
    }

    private fun getPath(uri: Uri): String? {
        return try {
            val cursor = contentResolver.query(
                uri, arrayOf(MediaStore.Video.Media.DATA),
                null, null, null)
            cursor?.use {
                it.moveToFirst()
                it.getString(it.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DATA))
            } ?: uri.path
        } catch (e: Exception) {
            uri.path
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
