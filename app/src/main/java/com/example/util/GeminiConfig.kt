package com.example.util

import com.example.BuildConfig

/**
 * Phase 0: Gemini/Firebase AI not wired (local mock only). This wrapper safely exposes the key
 * if present, without crashing when BuildConfig field is the placeholder.
 * Phase 2 will wire firebase-ai + BuildConfig.GEMINI_API_KEY for smart reply/summarize.
 */
object GeminiConfig {
    val apiKey: String? by lazy {
        try {
            val raw = BuildConfig.GEMINI_API_KEY
            if (raw.isBlank() || raw == "MY_GEMINI_API_KEY") null else raw
        } catch (_: Exception) {
            null
        }
    }

    val isConfigured: Boolean get() = apiKey != null
}
