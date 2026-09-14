package com.vexo.app.editor

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlaybackController(private val context: Context) {

    var player: ExoPlayer? = null
        private set

    fun init(playerView: PlayerView) {
        release()
        val p = ExoPlayer.Builder(context).build()
        playerView.player = p
        p.playWhenReady = false
        player = p
    }

    fun loadState(state: EditorState, playWhenReady: Boolean = false) {
        val p = player ?: return
        p.stop()
        p.clearMediaItems()

        for (clip in state.clips) {
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
        p.playWhenReady = playWhenReady
    }

    fun play()  { player?.play() }
    fun pause() { player?.pause() }

    fun seekToMs(positionMs: Long, state: EditorState) {
        val p = player ?: return
        var offset = 0L
        for (i in state.clips.indices) {
            val clip = state.clips[i]
            val end = offset + clip.trimmedDurationMs
            if (positionMs <= end) {
                val posInClip = (positionMs - offset).coerceIn(0L, clip.trimmedDurationMs)
                p.seekTo(i, clip.trimStartMs + posInClip)
                return
            }
            offset = end
        }
        if (state.clips.isNotEmpty()) {
            val last = state.clips.last()
            p.seekTo(state.clips.size - 1, last.trimEndMs)
        }
    }

    fun currentPositionMs(state: EditorState): Long {
        val p = player ?: return 0L
        var offset = 0L
        val idx = p.currentMediaItemIndex
        for (i in state.clips.indices) {
            val clip = state.clips[i]
            if (i == idx) {
                val posInClip = (p.currentPosition - clip.trimStartMs).coerceAtLeast(0L)
                return offset + posInClip
            }
            offset += clip.trimmedDurationMs
        }
        return offset
    }

    fun addListener(listener: Player.Listener) { player?.addListener(listener) }
    fun removeListener(listener: Player.Listener) { player?.removeListener(listener) }

    fun release() {
        player?.release()
        player = null
    }
}
