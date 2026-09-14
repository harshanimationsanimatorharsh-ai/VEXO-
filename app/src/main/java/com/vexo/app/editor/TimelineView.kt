package com.vexo.app.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class TimelineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
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

    // px per ms
    private val pxPerMs: Float get() = width.toFloat() / max(totalDurationMs, 1L)

    // paints
    private val clipPaint   = Paint().apply { style = Paint.Style.FILL }
    private val selectedPaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.WHITE
    }
    private val playheadPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE; strokeWidth = 3f; color = Color.RED
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE; textSize = 28f; isAntiAlias = true
    }
    private val trimPaint = Paint().apply {
        style = Paint.Style.FILL; color = Color.parseColor("#AA6C63FF")
    }

    private val clipColors = listOf(
        Color.parseColor("#6C63FF"),
        Color.parseColor("#43E97B"),
        Color.parseColor("#FF6584"),
        Color.parseColor("#FFD700"),
        Color.parseColor("#00B4D8")
    )

    // Drag state
    private enum class DragMode { NONE, PLAYHEAD, TRIM_LEFT, TRIM_RIGHT }
    private var dragMode = DragMode.NONE
    private var dragClipId: String? = null

    fun setClips(clips: List<Clip>, totalMs: Long, playheadMs: Long, selectedId: String?) {
        this.clips       = clips
        this.totalDurationMs = max(totalMs, 1L)
        this.playheadMs  = playheadMs
        this.selectedClipId = selectedId
        invalidate()
    }

    fun updatePlayhead(ms: Long) {
        playheadMs = ms
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val clipH = h * 0.7f
        val clipTop = (h - clipH) / 2f

        var offsetPx = 0f
        clips.forEachIndexed { i, clip ->
            val clipW = clip.trimmedDurationMs * pxPerMs
            val color = clipColors[i % clipColors.size]

            // Clip body
            clipPaint.color = color
            val rect = RectF(offsetPx, clipTop, offsetPx + clipW, clipTop + clipH)
            canvas.drawRoundRect(rect, 12f, 12f, clipPaint)

            // Selected border
            if (clip.id == selectedClipId) {
                canvas.drawRoundRect(rect, 12f, 12f, selectedPaint)

                // Trim handles
                trimPaint.color = Color.parseColor("#AA000000")
                // Left handle
                canvas.drawRoundRect(
                    RectF(offsetPx, clipTop, offsetPx + 20f, clipTop + clipH),
                    6f, 6f, trimPaint
                )
                // Right handle
                canvas.drawRoundRect(
                    RectF(offsetPx + clipW - 20f, clipTop, offsetPx + clipW, clipTop + clipH),
                    6f, 6f, trimPaint
                )
            }

            // Clip label
            val label = "Clip ${i + 1}"
            val tw = textPaint.measureText(label)
            if (tw < clipW - 8) {
                canvas.drawText(label, offsetPx + (clipW - tw) / 2, clipTop + clipH / 2 + 10, textPaint)
            }

            offsetPx += clipW
        }

        // Playhead line
        val phX = playheadMs * pxPerMs
        canvas.drawLine(phX, 0f, phX, h, playheadPaint)
        // Playhead triangle
        val tri = Path().apply {
            moveTo(phX - 12f, 0f)
            lineTo(phX + 12f, 0f)
            lineTo(phX, 24f)
            close()
        }
        canvas.drawPath(tri, playheadPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val ms = (x / pxPerMs).toLong().coerceIn(0, totalDurationMs)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragMode = DragMode.NONE
                // Check trim handles on selected clip
                selectedClipId?.let { selId ->
                    val clip = clips.firstOrNull { it.id == selId }
                    if (clip != null) {
                        val startPx = clips.take(clips.indexOf(clip))
                            .sumOf { it.trimmedDurationMs } * pxPerMs
                        val endPx = startPx + clip.trimmedDurationMs * pxPerMs
                        if (x in startPx..(startPx + 30f)) {
                            dragMode = DragMode.TRIM_LEFT; dragClipId = selId; return true
                        }
                        if (x in (endPx - 30f)..endPx) {
                            dragMode = DragMode.TRIM_RIGHT; dragClipId = selId; return true
                        }
                    }
                }
                // Check playhead
                val phX = playheadMs * pxPerMs
                if (Math.abs(x - phX) < 40f) {
                    dragMode = DragMode.PLAYHEAD; return true
                }
                // Check clip tap
                var offsetMs = 0L
                clips.forEach { clip ->
                    val endMs = offsetMs + clip.trimmedDurationMs
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
                        val clip = clips.firstOrNull { it.id == cid } ?: return false
                        // ms here is absolute timeline ms → convert to source ms
                        val startOffset = clips.take(clips.indexOf(clip))
                            .sumOf { it.trimmedDurationMs }
                        val newTrimStart = (clip.trimStartMs + (ms - startOffset))
                            .coerceIn(0, clip.trimEndMs - 500)
                        listener?.onTrimChanged(cid, newTrimStart, clip.trimEndMs)
                    }
                    DragMode.TRIM_RIGHT -> {
                        val cid = dragClipId ?: return false
                        val clip = clips.firstOrNull { it.id == cid } ?: return false
                        val startOffset = clips.take(clips.indexOf(clip))
                            .sumOf { it.trimmedDurationMs }
                        val newTrimEnd = (clip.trimStartMs + (ms - startOffset))
                            .coerceIn(clip.trimStartMs + 500, clip.sourceDurationMs)
                        listener?.onTrimChanged(cid, clip.trimStartMs, newTrimEnd)
                    }
                    else -> {}
                }
            }
            MotionEvent.ACTION_UP -> { dragMode = DragMode.NONE; dragClipId = null }
        }
        return true
    }
}
