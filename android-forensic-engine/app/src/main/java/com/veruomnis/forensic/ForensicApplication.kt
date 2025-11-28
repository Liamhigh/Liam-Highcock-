package com.veruomnis.forensic

import android.app.Application

/**
 * Verum Omnis Forensic Application
 * 
 * Main application class for the Nine-Brain Forensic Engine.
 */
class ForensicApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ForensicApplication
            private set
    }
}
