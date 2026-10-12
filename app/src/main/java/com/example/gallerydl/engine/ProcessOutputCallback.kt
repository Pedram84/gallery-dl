package com.example.gallerydl.engine

/**
 * Callback interface invoked from Python when stdout or stderr lines are written,
 * with cancellation support.
 */
interface ProcessOutputCallback {
    fun onStdout(line: String)
    fun onStderr(line: String)
    fun isCancelled(): Boolean = false
}
