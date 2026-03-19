package com.example.financetracker.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Utility for highlighting search query matches in text.
 */
object TextHighlightUtil {

    /**
     * Builds an AnnotatedString with highlighted portions matching the search query.
     *
     * @param text The original text to display
     * @param query The search query to highlight (case-insensitive)
     * @param highlightColor The background color for highlighted portions
     * @return AnnotatedString with highlighted matches, or plain text if query is blank
     */
    fun buildHighlightedText(
        text: String,
        query: String,
        highlightColor: Color
    ): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(text)

        return buildAnnotatedString {
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            var lastIndex = 0

            while (true) {
                val index = lowerText.indexOf(lowerQuery, lastIndex)
                if (index == -1) {
                    // Append remaining text after last match
                    append(text.substring(lastIndex))
                    break
                }

                // Append text before the match
                append(text.substring(lastIndex, index))

                // Append the matched portion with highlight
                withStyle(SpanStyle(background = highlightColor)) {
                    append(text.substring(index, index + query.length))
                }

                lastIndex = index + query.length
            }
        }
    }
}
