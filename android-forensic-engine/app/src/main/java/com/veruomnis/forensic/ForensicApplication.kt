package com.veruomnis.forensic

import android.app.Application

/**
 * Application class for Verum Omnis Forensic Engine
 * Initializes the Nine-Brain Architecture and constitutional compliance framework
 */
class ForensicApplication : Application() {
    
    companion object {
        const val TAG = "VerumOmnisForensic"
        const val VERSION = "5.2.6"
        const val CODENAME = "Nine Brains. One Truth."
        
        @Volatile
        private var instance: ForensicApplication? = null
        
        fun getInstance(): ForensicApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize forensic engine components
        initializeForensicEngine()
    }
    
    private fun initializeForensicEngine() {
        // Initialization is done lazily when needed
        // This ensures 100% offline capability
        android.util.Log.i(TAG, "Verum Omnis Forensic Engine v$VERSION initialized")
        android.util.Log.i(TAG, CODENAME)
    }
}
