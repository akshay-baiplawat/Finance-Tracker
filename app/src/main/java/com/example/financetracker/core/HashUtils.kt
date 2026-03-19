package com.example.financetracker.core

import java.security.MessageDigest

object HashUtils {
    // Generates a SHA-256 hash so we can uniquely identify an SMS
    fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}