package com.vexo.app.editor

class UndoRedoManager(private val maxHistory: Int = 50) {

    private val undoStack = ArrayDeque<EditorState>()
    private val redoStack = ArrayDeque<EditorState>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Call before applying any edit — saves current state */
    fun push(state: EditorState) {
        undoStack.addLast(state)
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: EditorState): EditorState? {
        if (!canUndo) return null
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        return prev
    }

    fun redo(current: EditorState): EditorState? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
