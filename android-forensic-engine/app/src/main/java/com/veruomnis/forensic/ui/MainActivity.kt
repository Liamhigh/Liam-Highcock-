package com.veruomnis.forensic.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.veruomnis.forensic.ForensicApplication
import com.veruomnis.forensic.R
import com.veruomnis.forensic.databinding.ActivityMainBinding
import com.veruomnis.forensic.models.Evidence
import com.veruomnis.forensic.models.EvidenceType
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID

/**
 * MainActivity - Main entry point for Verum Omnis Forensic Engine
 * 
 * Features:
 * - Import evidence files (documents, images, audio, video)
 * - View loaded evidence
 * - Start Nine-Brain forensic analysis
 * - View generated reports
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    // Evidence storage
    private val evidenceList = mutableListOf<Evidence>()
    
    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            importFiles(uris)
        }
    }
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            openFilePicker()
        } else {
            Toast.makeText(this, R.string.storage_permission_rationale, Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupButtons()
        updateUI()
        
        // Handle files shared with the app
        handleIncomingIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }
    
    private fun setupButtons() {
        binding.importButton.setOnClickListener {
            checkPermissionsAndOpenPicker()
        }
        
        binding.analyzeButton.setOnClickListener {
            if (evidenceList.isNotEmpty()) {
                startAnalysis()
            } else {
                Toast.makeText(this, R.string.error_no_evidence, Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.viewReportsButton.setOnClickListener {
            // Navigate to reports list
            val reportsDir = ForensicApplication.getInstance().getReportsDirectory()
            val reports = reportsDir.listFiles()?.filter { it.extension == "pdf" } ?: emptyList()
            
            if (reports.isEmpty()) {
                Toast.makeText(this, "No reports generated yet", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "${reports.size} report(s) available", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun checkPermissionsAndOpenPicker() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            openFilePicker()
        } else {
            permissionLauncher.launch(permissions)
        }
    }
    
    private fun openFilePicker() {
        val mimeTypes = arrayOf(
            "image/*",
            "audio/*",
            "video/*",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/*"
        )
        filePickerLauncher.launch(mimeTypes)
    }
    
    private fun importFiles(uris: List<Uri>) {
        val evidenceDir = ForensicApplication.getInstance().getEvidenceDirectory()
        
        uris.forEach { uri ->
            try {
                val fileName = getFileName(uri) ?: "unknown_${System.currentTimeMillis()}"
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                
                // Copy file to evidence directory
                val destFile = File(evidenceDir, fileName)
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Create evidence object
                val evidence = Evidence(
                    id = UUID.randomUUID().toString(),
                    type = determineEvidenceType(mimeType),
                    filePath = destFile.absolutePath,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = destFile.length(),
                    dateAdded = Date(),
                    metadata = extractMetadata(destFile, mimeType)
                )
                
                evidenceList.add(evidence)
                
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error importing file: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        updateUI()
        Toast.makeText(
            this,
            "${uris.size} file(s) imported",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            name = cursor.getString(nameIndex)
        }
        return name
    }
    
    private fun determineEvidenceType(mimeType: String): EvidenceType {
        return when {
            mimeType.startsWith("image/") -> EvidenceType.IMAGE
            mimeType.startsWith("audio/") -> EvidenceType.AUDIO
            mimeType.startsWith("video/") -> EvidenceType.VIDEO
            mimeType.startsWith("application/pdf") || 
            mimeType.startsWith("application/msword") ||
            mimeType.contains("document") ||
            mimeType.startsWith("text/") -> EvidenceType.DOCUMENT
            else -> EvidenceType.UNKNOWN
        }
    }
    
    private fun extractMetadata(file: File, mimeType: String): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        
        metadata["created_date"] = file.lastModified()
        metadata["modified_date"] = file.lastModified()
        
        // Extract EXIF for images (placeholder for actual implementation)
        if (mimeType.startsWith("image/")) {
            try {
                // In production, use metadata-extractor library
                metadata["source"] = "device_gallery"
            } catch (e: Exception) {
                // Ignore metadata extraction errors
            }
        }
        
        return metadata
    }
    
    private fun updateUI() {
        if (evidenceList.isEmpty()) {
            binding.evidenceStatus.text = getString(R.string.no_evidence)
            binding.evidenceRecyclerView.visibility = View.GONE
            binding.analyzeButton.isEnabled = false
        } else {
            binding.evidenceStatus.text = getString(R.string.evidence_count, evidenceList.size)
            binding.evidenceRecyclerView.visibility = View.VISIBLE
            binding.analyzeButton.isEnabled = true
            
            // Setup RecyclerView (simplified - would use adapter in production)
            binding.evidenceRecyclerView.layoutManager = LinearLayoutManager(this)
        }
        
        // Update reports count
        val reportsDir = ForensicApplication.getInstance().getReportsDirectory()
        val reportsCount = reportsDir.listFiles()?.count { it.extension == "pdf" } ?: 0
        binding.reportsCount.text = if (reportsCount > 0) {
            "$reportsCount report(s) available"
        } else {
            "No reports generated yet"
        }
    }
    
    private fun startAnalysis() {
        val intent = Intent(this, AnalysisActivity::class.java)
        intent.putExtra(AnalysisActivity.EXTRA_EVIDENCE_IDS, 
            evidenceList.map { it.id }.toTypedArray())
        intent.putExtra(AnalysisActivity.EXTRA_EVIDENCE_PATHS,
            evidenceList.map { it.filePath }.toTypedArray())
        startActivity(intent)
    }
    
    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    importFiles(listOf(uri))
                }
            }
            Intent.ACTION_SEND -> {
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                    importFiles(listOf(uri))
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris ->
                    importFiles(uris)
                }
            }
        }
    }
    
    companion object {
        // Singleton access to evidence list for other activities
        private var sharedEvidenceList: List<Evidence>? = null
        
        fun setEvidenceList(evidence: List<Evidence>) {
            sharedEvidenceList = evidence
        }
        
        fun getEvidenceList(): List<Evidence>? = sharedEvidenceList
    }
}
