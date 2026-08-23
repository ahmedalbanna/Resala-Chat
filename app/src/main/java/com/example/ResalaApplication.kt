package com.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ResalaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Phase 2: Firebase offline persistence + anonymous auth warmup is handled
        // lazily by AuthManager / FirestoreSyncManager to allow builds without
        // google-services.json (local-only fallback).
    }
}
