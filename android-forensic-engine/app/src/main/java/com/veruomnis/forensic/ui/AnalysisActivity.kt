package com.veruomnis.forensic.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.veruomnis.forensic.ForensicApplication
import com.veruomnis.forensic.R
import com.veruomnis.forensic.databinding.ActivityAnalysisBinding
import com.veruomnis.forensic.engine.ForensicEngine
import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * AnalysisActivity - Nine-Brain Forensic Analysis
 * 
 * Runs the complete forensic analysis pipeline:
 * 1. Contradiction Detection (Brain 1)
 * 2. Behavioral Diagnostics (Brain 2)
 * 3. Document Authenticity (Brain 3)
 * 4. Timeline & Geolocation (Brain 4)
 * 5. Voice Forensics (Brain 5)
 * 6. Image Validation (Brain 6)
 * 7. Legal Compliance (Brain 7)
 * 8. Predictive Analytics (Brain 8)
 * 9. Synthesis & Verdict (Brain 9)
 */
class AnalysisActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_EVIDENCE_IDS = "evidence_ids"
        const val EXTRA_EVIDENCE_PATHS = "evidence_paths"
    }
    
    private lateinit var binding: ActivityAnalysisBinding
    private val forensicEngine = ForensicEngine()
    
    // Location services for GPS sealing
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: GpsLocation? = null
    
    // Analysis result
    private var analysisReport: ForensicReport? = null
    
    // Permission launcher for location
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            getLastKnownLocation()
        }
        // Start analysis regardless of location permission
        startAnalysis()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupUI()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Check location permissions and start analysis
        checkLocationAndStart()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
    
    private fun setupUI() {
        binding.viewReportButton.setOnClickListener {
            analysisReport?.let { report ->
                navigateToReport(report)
            }
        }
    }
    
    private fun checkLocationAndStart() {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val hasPermissions = locationPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (hasPermissions) {
            getLastKnownLocation()
            startAnalysis()
        } else {
            // Request permissions, analysis will start after
            locationPermissionLauncher.launch(locationPermissions)
        }
    }
    
    private fun getLastKnownLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        currentLocation = GpsLocation(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            accuracy = it.accuracy,
                            timestamp = Date(it.time)
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            // Location not available
        }
    }
    
    private fun startAnalysis() {
        // Get evidence from intent
        val evidenceIds = intent.getStringArrayExtra(EXTRA_EVIDENCE_IDS) ?: emptyArray()
        val evidencePaths = intent.getStringArrayExtra(EXTRA_EVIDENCE_PATHS) ?: emptyArray()
        
        if (evidenceIds.isEmpty() || evidencePaths.isEmpty()) {
            Toast.makeText(this, R.string.error_no_evidence, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Reconstruct evidence list
        val evidenceList = evidenceIds.zip(evidencePaths).map { (id, path) ->
            val file = File(path)
            val mimeType = determineMimeType(file.name)
            Evidence(
                id = id,
                type = determineType(mimeType),
                filePath = path,
                fileName = file.name,
                mimeType = mimeType,
                fileSize = file.length(),
                dateAdded = Date(file.lastModified()),
                metadata = mapOf(
                    "created_date" to file.lastModified(),
                    "modified_date" to file.lastModified()
                )
            )
        }
        
        // Store for other activities
        MainActivity.setEvidenceList(evidenceList)
        
        // Run analysis
        lifecycleScope.launch {
            try {
                val report = forensicEngine.analyzeEvidence(
                    evidence = evidenceList,
                    gpsLocation = currentLocation,
                    callback = object : ForensicEngine.ProgressCallback {
                        override fun onBrainStarted(brainNumber: Int, brainName: String) {
                            runOnUiThread {
                                binding.currentBrain.text = brainName
                                binding.brainProgress.text = getString(
                                    R.string.brain_progress, brainName, brainNumber, 9
                                )
                            }
                        }
                        
                        override fun onBrainCompleted(
                            brainNumber: Int, 
                            brainName: String, 
                            result: BrainAnalysisResult
                        ) {
                            // Update progress
                        }
                        
                        override fun onProgressUpdate(percentage: Int, message: String) {
                            runOnUiThread {
                                binding.overallProgress.progress = percentage
                                binding.progressPercentage.text = "$percentage%"
                            }
                        }
                        
                        override fun onAnalysisComplete(report: ForensicReport) {
                            runOnUiThread {
                                showAnalysisComplete(report)
                            }
                        }
                        
                        override fun onError(error: String) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@AnalysisActivity,
                                    "Error: $error",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
                
                withContext(Dispatchers.Main) {
                    analysisReport = report
                    setLastReport(report) // Store for ReportActivity
                    showAnalysisComplete(report)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AnalysisActivity,
                        "Analysis failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun showAnalysisComplete(report: ForensicReport) {
        binding.currentBrain.text = getString(R.string.analysis_complete)
        binding.brainProgress.text = "All 9 brains completed"
        binding.overallProgress.progress = 100
        binding.progressPercentage.text = "100%"
        
        // Show TSI card
        binding.tsiCard.visibility = View.VISIBLE
        binding.tsiValue.text = String.format("%.1f", report.synthesisResult.truthStabilityIndex)
        
        // Show view report button
        binding.viewReportButton.visibility = View.VISIBLE
        binding.viewReportButton.isEnabled = true
    }
    
    private fun navigateToReport(report: ForensicReport) {
        val intent = Intent(this, ReportActivity::class.java)
        intent.putExtra(ReportActivity.EXTRA_REPORT_ID, report.reportId)
        startActivity(intent)
    }
    
    private fun determineMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt" -> "text/plain"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/m4a"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            else -> "application/octet-stream"
        }
    }
    
    private fun determineType(mimeType: String): EvidenceType {
        return when {
            mimeType.startsWith("image/") -> EvidenceType.IMAGE
            mimeType.startsWith("audio/") -> EvidenceType.AUDIO
            mimeType.startsWith("video/") -> EvidenceType.VIDEO
            mimeType.startsWith("application/pdf") ||
            mimeType.contains("document") ||
            mimeType.startsWith("text/") -> EvidenceType.DOCUMENT
            else -> EvidenceType.UNKNOWN
        }
    }
    
    companion object {
        // Store report for other activities
        private var lastReport: ForensicReport? = null
        
        fun setLastReport(report: ForensicReport) {
            lastReport = report
        }
        
        fun getLastReport(): ForensicReport? = lastReport
    }
}
