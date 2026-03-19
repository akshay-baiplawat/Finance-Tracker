package com.example.financetracker.core.util

import android.util.Log

/**
 * Centralized logging utility for the Finance Tracker app.
 *
 * Benefits:
 * - Single point for all app logging
 * - Easy to integrate crash reporting (Firebase Crashlytics, Sentry) later
 * - Consistent log format across the app
 * - Can be easily disabled for release builds if needed
 */
object AppLogger {
    private const val APP_TAG = "FinanceTracker"

    /**
     * Log an error message.
     * Use for critical failures that should be investigated.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$APP_TAG.$tag"
        if (throwable != null) {
            Log.e(fullTag, message, throwable)
        } else {
            Log.e(fullTag, message)
        }
    }

    /**
     * Log a warning message.
     * Use for recoverable issues (e.g., invalid color hex, using fallback value).
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$APP_TAG.$tag"
        if (throwable != null) {
            Log.w(fullTag, message, throwable)
        } else {
            Log.w(fullTag, message)
        }
    }

    /**
     * Log a debug message.
     * Use for development/debugging information.
     */
    fun d(tag: String, message: String) {
        Log.d("$APP_TAG.$tag", message)
    }

    /**
     * Log an info message.
     * Use for general informational messages.
     */
    fun i(tag: String, message: String) {
        Log.i("$APP_TAG.$tag", message)
    }
}
