package com.vexo.app.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onClipSelected(clipId: String)
        fun onPlayheadSeeked(positionMs: Long)
        fun onTrimChanged(clipId: String, newStartMs: Long, newEndMs: Long)
    }

    var listener: Listener? = null

    private var clips: List<Clip> = emptyList()
    private var totalDurationMs: Long = 1L
    private var playheadMs: Long = 0L
    private var selectedClipId: String? = null

    private fun pxPerMs(): Float {
        val w = width
        if (w <= 0 || totalDurationMs <= 0L) return 1.0f
        return w.toFloat() / totalDurationMs.toFloat()
    }

    private val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.0f
        color = Color.WHITE
    }
    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 3.0f
        color = Color.RED
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28.0f
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#CC000000")
    }

    private val clipColors = listOf(
        Color.parseColor("#6C63FF"),
        Color.parseColor("#43E97B"),
        Color.parseColor("#FF6584"),
        Color.parseColor("#FFD700"),
        Color.parseColor("#00B4D8")
    )

    private enum class DragMode { NONE, PLAYHEAD, TRIM_LEFT, TRIM_RIGHT }
    private var dragMode = DragMode.NONE
    private var dragClipId: String? = null

    fun setClips(
        newClips: List<Clip>,
        totalMs: Long,
        currentPlayheadMs: Long,
        selectedId: String?
    ) {
        clips = newClips
        totalDurationMs = if (totalMs > 0L) totalMs else 1L
        playheadMs = currentPlayheadMs
        selectedClipId = selectedId
        invalidate()
    }

    fun updatePlayhead(ms: Long) {
        playheadMs = ms
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val ppm = pxPerMs()
        val h = height.toFloat()
        val clipH = h * 0.65f
        val clipTop = (h - clipH) / 2.0f

        var offsetPx = 0.0f

        for (i in clips.indices) {
            val clip = clips[i]
            val clipW: Float = clip.trimmedDurationMs.toFloat() * ppm
            if (clipW < 1.0f) {
                offsetPx = offsetPx + clipW
                continue
            }

            clipPaint.color = clipColors[i % clipColors.size]
            val rect = RectF(offsetPx, clipTop, offsetPx + clipW, clipTop + clipH)
            canvas.drawRoundRect(rect, 10.0f, 10.0f, clipPaint)

            if (clip.id == selectedClipId) {
                canvas.drawRoundRect(rect, 10.0f, 10.0f, borderPaint)
                // Left handle
                canvas.drawRoundRect(
                    RectF(offsetPx, clipTop, offsetPx + 18.0f, clipTop + clipH),
                    6.0f, 6.0f, handlePaint
                )
                // Right handle
                canvas.drawRoundRect(
                    RectF(offsetPx + clipW - 18.0f, clipTop, offsetPx + clipW, clipTop + clipH),
                    6.0f, 6.0f, handlePaint
                )
            }

            val label = "Clip ${i + 1}"
            val tw = textPaint.measureText(label)
            if (tw < clipW - 8.0f) {
                canvas.drawText(
                    label,
                    offsetPx + (clipW - tw) / 2.0f,
                    clipTop + clipH / 2.0f + 10.0f,
                    textPaint
                )
            }

            offsetPx = offsetPx + clipW
        }

        // Playhead
        val phX: Float = playheadMs.toFloat() * ppm
        canvas.drawLine(phX, 0.0f, phX, h, playheadPaint)
        val tri = Path()
        tri.moveTo(phX - 12.0f, 0.0f)
        tri.lineTo(phX + 12.0f, 0.0f)
        tri.lineTo(phX, 24.0f)
        tri.close()
        canvas.drawPath(tri, playheadPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val ppm = pxPerMs()
        val ms: Long = (x / ppm).toLong().coerceIn(0L, totalDurationMs)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragMode = DragMode.NONE

                // Check trim handles
                val selId = selectedClipId
                if (selId != null) {
                    var startOffsetMs = 0L
                    var selClip: Clip? = null
                    for (c in clips) {
                        if (c.id == selId) { selClip = c; break }
                        startOffsetMs = startOffsetMs + c.trimmedDurationMs
                    }
                    if (selClip != null) {
                        val startPx: Float = startOffsetMs.toFloat() * ppm
                        val endPx: Float = startPx + selClip.trimmedDurationMs.toFloat() * ppm
                        if (x >= startPx && x <= startPx + 30.0f) {
                            dragMode = DragMode.TRIM_LEFT
                            dragClipId = selId
                            return true
                        }
                        if (x >= endPx - 30.0f && x <= endPx) {
                            dragMode = DragMode.TRIM_RIGHT
                            dragClipId = selId
                            return true
                        }
                    }
                }

                // Check playhead
                val phX: Float = playheadMs.toFloat() * ppm
                if (Math.abs(x - phX) < 40.0f) {
                    dragMode = DragMode.PLAYHEAD
                    return true
                }

                // Check clip tap
                var offsetMs = 0L
                for (clip in clips) {
                    val endMs: Long = offsetMs + clip.trimmedDurationMs
                    if (ms in offsetMs..endMs) {
                        listener?.onClipSelected(clip.id)
                        return true
                    }
                    offsetMs = endMs
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (dragMode) {
                    DragMode.PLAYHEAD -> {
                        playheadMs = ms
                        listener?.onPlayheadSeeked(ms)
                        invalidate()
                    }
                    DragMode.TRIM_LEFT -> {
                        val cid = dragClipId ?: return false
                        var clip: Clip? = null
                        var startOffsetMs = 0L
                        for (c in clips) {
                            if (c.id == cid) { clip = c; break }
                            startOffsetMs = startOffsetMs + c.trimmedDurationMs
                        }
                        if (clip == null) return false
                        val posInSource: Long = clip.trimStartMs + (ms - startOffsetMs)
                        val newStart: Long = posInSource.coerceIn(0L, clip.trimEndMs - 500L)
                        listener?.onTrimChanged(cid, newStart, clip.trimEndMs)
                    }
                    DragMode.TRIM_RIGHT -> {
                        val cid = dragClipId ?: return false
                        var clip: Clip? = null
                        var startOffsetMs = 0L
                        for (c in clips) {
                            if (c.id == cid) { clip = c; break }
                            startOffsetMs = startOffsetMs + c.trimmedDurationMs
                        }
                        if (clip == null) return false
                        val posInSource: Long = clip.trimStartMs + (ms - startOffsetMs)
                        val newEnd: Long = posInSource.coerceIn(clip.trimStartMs + 500L, clip.sourceDurationMs)
                        listener?.onTrimChanged(cid, clip.trimStartMs, newEnd)
                    }
                    else -> {}
                }
            }

            MotionEvent.ACTION_UP -> {
                dragMode = DragMode.NONE
                dragClipId = null
            }
        }
        return true
    }
}
