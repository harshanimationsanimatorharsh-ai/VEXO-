package com.vexo.app.editor

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File

class ExportController(private val context: Context) {

    interface ExportCallback {
        fun onProgress(percent: Int)
        fun onSuccess(filePath: String)
        fun onFailure(error: String)
    }

    private var transformer: Transformer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun export(state: EditorState, callback: ExportCallback) {
        if (state.clips.isEmpty()) {
            callback.onFailure("No clips to export")
            return
        }

        val outFile = createOutputFile()

        val editedItems = ArrayList<EditedMediaItem>()
        for (clip in state.clips) {
            val mediaItem = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                )
                .build()
            val editedItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(clip.volume == 0f)
                .build()
            editedItems.add(editedItem)
        }

        val sequence = EditedMediaItemSequence(editedItems)
        val composition = Composition.Builder(listOf(sequence)).build()

        transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(comp: Composition, result: ExportResult) {
                    saveToMediaStore(outFile)
                    handler.post { callback.onSuccess(outFile.absolutePath) }
                }
                override fun onError(
                    comp: Composition,
                    result: ExportResult,
                    ex: ExportException
                ) {
                    handler.post { callback.onFailure(ex.message ?: "Export failed") }
                }
            })
            .build()

        transformer?.start(composition, outFile.absolutePath)
        pollProgress(callback)
    }

    private fun pollProgress(callback: ExportCallback) {
        val t = transformer ?: return
        val progressHolder = ProgressHolder()
        val progressState = t.getProgress(progressHolder)
        if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
            callback.onProgress(progressHolder.progress)
        }
        if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
            handler.postDelayed({ pollProgress(callback) }, 300)
        }
    }

    private fun createOutputFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "Movies")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "VEXO_${System.currentTimeMillis()}.mp4")
    }

    private fun saveToMediaStore(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + "/VEXO"
                    )
                }
            }
            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
            )
            uri?.let { dest ->
                context.contentResolver.openOutputStream(dest)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel() {
        transformer?.cancel()
        transformer = null
    }
}
