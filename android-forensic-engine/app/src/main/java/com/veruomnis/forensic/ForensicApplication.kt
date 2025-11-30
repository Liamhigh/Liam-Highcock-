package com.veruomnis.forensic

import android.app.Application
import java.io.File

/**
 * Verum Omnis Forensic Application
 * 
 * Main application class that initializes the forensic engine
 * and manages application-wide state.
 * 
 * Constitutional Compliance:
 * - Zero-Loss Evidence Doctrine: All evidence is preserved
 * - No telemetry or analytics: Complete privacy
 * - Offline operation: No network required
 */
class ForensicApplication : Application() {
    
    companion object {
        const val VERSION = "5.2.6"
        const val VERSION_CODE = 1
        
        @Volatile
        private var instance: ForensicApplication? = null
        
        fun getInstance(): ForensicApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
    
    // Directories for forensic data
    lateinit var evidenceDir: File
        private set
    lateinit var reportsDir: File
        private set
    lateinit var cacheDir: File
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize directories
        initializeDirectories()
    }
    
    private fun initializeDirectories() {
        // Evidence storage directory
        evidenceDir = File(filesDir, "evidence").apply {
            if (!exists()) mkdirs()
        }
        
        // Reports storage directory
        reportsDir = File(filesDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        
        // Cache directory for temporary files
        cacheDir = File(this.cacheDir, "forensic_cache").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Get the evidence storage directory
     */
    fun getEvidenceDirectory(): File = evidenceDir
    
    /**
     * Get the reports storage directory
     */
    fun getReportsDirectory(): File = reportsDir
    
    /**
     * Clear the forensic cache
     */
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
