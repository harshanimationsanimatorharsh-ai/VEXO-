package com.vexo.app.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

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

    private val pxPerMs: Float
        get() = if (totalDurationMs > 0L) width.toFloat() / totalDurationMs.toFloat() else 1f

    private val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 3f
        color = Color.RED
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
    }
    private val trimHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
        clips: List<Clip>,
        totalMs: Long,
        playheadMs: Long,
        selectedId: String?
    ) {
        this.clips          = clips
        this.totalDurationMs = if (totalMs > 0L) totalMs else 1L
        this.playheadMs     = playheadMs
        this.selectedClipId = selectedId
        invalidate()
    }

    fun updatePlayhead(ms: Long) {
        playheadMs = ms
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0) return

        val h = height.toFloat()
        val clipH = h * 0.65f
        val clipTop = (h - clipH) / 2f

        var offsetPx = 0f

        for (i in clips.indices) {
            val clip = clips[i]
            val clipW = clip.trimmedDurationMs.toFloat() * pxPerMs
            if (clipW < 1f) { offsetPx += clipW; continue }

            val color = clipColors[i % clipColors.size]
            clipPaint.color = color

            val rect = RectF(offsetPx, clipTop, offsetPx + clipW, clipTop + clipH)
            canvas.drawRoundRect(rect, 10f, 10f, clipPaint)

            if (clip.id == selectedClipId) {
                canvas.drawRoundRect(rect, 10f, 10f, borderPaint)
                // Left trim handle
                canvas.drawRoundRect(
                    RectF(offsetPx, clipTop, offsetPx + 18f, clipTop + clipH),
                    6f, 6f, trimHandlePaint
                )
                // Right trim handle
                canvas.drawRoundRect(
                    RectF(offsetPx + clipW - 18f, clipTop, offsetPx + clipW, clipTop + clipH),
                    6f, 6f, trimHandlePaint
                )
            }

            val label = "Clip ${i + 1}"
            val tw = textPaint.measureText(label)
            if (tw < clipW - 8f) {
                canvas.drawText(
                    label,
                    offsetPx + (clipW - tw) / 2f,
                    clipTop + clipH / 2f + 10f,
                    textPaint
                )
            }

            offsetPx += clipW
        }

        // Playhead
        val phX = playheadMs.toFloat() * pxPerMs
        canvas.drawLine(phX, 0f, phX, h, playheadPaint)
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
        val ms = (x / pxPerMs).toLong().coerceIn(0L, totalDurationMs)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragMode = DragMode.NONE

                // Check trim handles on selected clip
                val selId = selectedClipId
                if (selId != null) {
                    val selClip = clips.firstOrNull { it.id == selId }
                    if (selClip != null) {
                        var startOffset = 0L
                        for (c in clips) {
                            if (c.id == selId) break
                            startOffset += c.trimmedDurationMs
                        }
                        val startPx = startOffset.toFloat() * pxPerMs
                        val endPx   = startPx + selClip.trimmedDurationMs.toFloat() * pxPerMs

                        if (x >= startPx && x <= startPx + 30f) {
                            dragMode   = DragMode.TRIM_LEFT
                            dragClipId = selId
                            return true
                        }
                        if (x >= endPx - 30f && x <= endPx) {
                            dragMode   = DragMode.TRIM_RIGHT
                            dragClipId = selId
                            return true
                        }
                    }
                }

                // Check playhead
                val phX = playheadMs.toFloat() * pxPerMs
                if (Math.abs(x - phX) < 40f) {
                    dragMode = DragMode.PLAYHEAD
                    return true
                }

                // Check clip tap
                var offsetMs = 0L
                for (clip in clips) {
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
                        val cid  = dragClipId ?: return false
                        val clip = clips.firstOrNull { it.id == cid } ?: return false
                        var startOffset = 0L
                        for (c in clips) {
                            if (c.id == cid) break
                            startOffset += c.trimmedDurationMs
                        }
                        val posInSource = clip.trimStartMs + (ms - startOffset)
                        val newStart = posInSource.coerceIn(0L, clip.trimEndMs - 500L)
                        listener?.onTrimChanged(cid, newStart, clip.trimEndMs)
                    }
                    DragMode.TRIM_RIGHT -> {
                        val cid  = dragClipId ?: return false
                        val clip = clips.firstOrNull { it.id == cid } ?: return false
                        var startOffset = 0L
                        for (c in clips) {
                            if (c.id == cid) break
                            startOffset += c.trimmedDurationMs
                        }
                        val posInSource = clip.trimStartMs + (ms - startOffset)
                        val newEnd = posInSource.coerceIn(clip.trimStartMs + 500L, clip.sourceDurationMs)
                        listener?.onTrimChanged(cid, clip.trimStartMs, newEnd)
                    }
                    else -> {}
                }
            }

            MotionEvent.ACTION_UP -> {
                dragMode   = DragMode.NONE
                dragClipId = null
            }
        }
        return true
    }
}
