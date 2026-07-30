package com.docapp.editor

import android.app.Application
import com.docapp.core.security.LicenseCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DocApp : Application() {

    lateinit var licenseCache: LicenseCache
        private set

    override fun onCreate() {
        super.onCreate()
        licenseCache = LicenseCache(this)
        licenseCache.loadIntoMemory() // sync StateVault dari cache terenkripsi, offline-ready
    }
}
