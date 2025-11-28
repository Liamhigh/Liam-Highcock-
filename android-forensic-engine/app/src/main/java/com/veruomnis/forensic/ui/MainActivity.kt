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
import com.veruomnis.forensic.R
import com.veruomnis.forensic.databinding.ActivityMainBinding
import com.veruomnis.forensic.models.Evidence
import com.veruomnis.forensic.models.EvidenceType
import java.util.Date
import java.util.UUID

/**
 * Main Activity - Entry point for the Verum Omnis Forensic Engine
 * Allows users to import evidence and start forensic analysis
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val evidenceList = mutableListOf<Evidence>()
    private var evidenceAdapter: EvidenceAdapter? = null
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris?.forEach { uri ->
            addEvidence(uri)
        }
        updateEvidenceUI()
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            openFilePicker()
        } else {
            Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupRecyclerView()
    }
    
    private fun setupUI() {
        binding.importButton.setOnClickListener {
            checkPermissionsAndOpenPicker()
        }
        
        binding.startAnalysisButton.setOnClickListener {
            startAnalysis()
        }
        
        binding.viewReportsButton.setOnClickListener {
            // TODO: Navigate to reports list
            Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupRecyclerView() {
        evidenceAdapter = EvidenceAdapter(evidenceList) { evidence ->
            removeEvidence(evidence)
        }
        binding.evidenceRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = evidenceAdapter
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
        filePickerLauncher.launch(arrayOf("*/*"))
    }
    
    private fun addEvidence(uri: Uri) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                
                val fileName = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                val fileSize = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                
                val evidence = Evidence(
                    id = UUID.randomUUID().toString(),
                    type = determineEvidenceType(mimeType),
                    filePath = uri.toString(),
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = fileSize,
                    dateAdded = Date(),
                    metadata = mapOf("source" to "user_import")
                )
                
                evidenceList.add(evidence)
            }
        }
    }
    
    private fun determineEvidenceType(mimeType: String): EvidenceType {
        return when {
            mimeType.startsWith("image/") -> EvidenceType.IMAGE
            mimeType.startsWith("audio/") -> EvidenceType.AUDIO
            mimeType.startsWith("video/") -> EvidenceType.VIDEO
            mimeType.startsWith("application/pdf") -> EvidenceType.DOCUMENT
            mimeType.startsWith("application/msword") -> EvidenceType.DOCUMENT
            mimeType.contains("document") -> EvidenceType.DOCUMENT
            mimeType.startsWith("text/") -> EvidenceType.DOCUMENT
            else -> EvidenceType.UNKNOWN
        }
    }
    
    private fun removeEvidence(evidence: Evidence) {
        evidenceList.remove(evidence)
        updateEvidenceUI()
    }
    
    private fun updateEvidenceUI() {
        evidenceAdapter?.notifyDataSetChanged()
        
        if (evidenceList.isEmpty()) {
            binding.evidenceCountText.text = getString(R.string.no_evidence)
            binding.evidenceRecyclerView.visibility = View.GONE
            binding.startAnalysisButton.isEnabled = false
        } else {
            binding.evidenceCountText.text = getString(R.string.evidence_count, evidenceList.size)
            binding.evidenceRecyclerView.visibility = View.VISIBLE
            binding.startAnalysisButton.isEnabled = true
        }
    }
    
    private fun startAnalysis() {
        if (evidenceList.isEmpty()) {
            Toast.makeText(this, R.string.error_no_evidence, Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, AnalysisActivity::class.java).apply {
            putExtra(AnalysisActivity.EXTRA_EVIDENCE_COUNT, evidenceList.size)
            // In a real app, we'd pass evidence via a shared repository or database
        }
        startActivity(intent)
    }
}

/**
 * Simple adapter for displaying evidence items
 */
class EvidenceAdapter(
    private val evidenceList: List<Evidence>,
    private val onRemoveClick: (Evidence) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<EvidenceAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val nameText: android.widget.TextView = itemView.findViewById(R.id.evidenceNameText)
        val typeText: android.widget.TextView = itemView.findViewById(R.id.evidenceTypeText)
        val removeButton: android.widget.ImageButton = itemView.findViewById(R.id.removeButton)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evidence, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val evidence = evidenceList[position]
        holder.nameText.text = evidence.fileName
        holder.typeText.text = "${evidence.type.name} • ${formatFileSize(evidence.fileSize)}"
        holder.removeButton.setOnClickListener { onRemoveClick(evidence) }
    }
    
    override fun getItemCount() = evidenceList.size
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
