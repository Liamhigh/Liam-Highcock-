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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.ProgressBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.veruomnis.forensic.R
import com.veruomnis.forensic.engine.CryptographicSealingSystem
import com.veruomnis.forensic.models.Evidence
import com.veruomnis.forensic.models.EvidenceType
import com.veruomnis.forensic.models.GpsLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * MainActivity - Main entry point for the Verum Omnis Forensic Engine
 * 
 * Provides evidence upload and management functionality.
 */
class MainActivity : AppCompatActivity() {

    private val evidenceList = mutableListOf<Evidence>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateView: View
    private lateinit var fabAddEvidence: FloatingActionButton
    private lateinit var btnAnalyze: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var sealingSystem: CryptographicSealingSystem
    private var currentGpsLocation: GpsLocation? = null

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Some permissions were denied. Limited functionality available.", Toast.LENGTH_LONG).show()
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            processSelectedFiles(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sealingSystem = CryptographicSealingSystem(this)

        initViews()
        checkPermissions()
        setupRecyclerView()
        setupClickListeners()

        // Handle intent if app was opened with a file
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewEvidence)
        emptyStateView = findViewById(R.id.emptyStateLayout)
        fabAddEvidence = findViewById(R.id.fabAddEvidence)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun checkPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = EvidenceAdapter(evidenceList) { evidence ->
            // Handle evidence item click - show details
            showEvidenceDetails(evidence)
        }
        updateEmptyState()
    }

    private fun setupClickListeners() {
        fabAddEvidence.setOnClickListener {
            openFilePicker()
        }

        btnAnalyze.setOnClickListener {
            if (evidenceList.isEmpty()) {
                Toast.makeText(this, "Please add evidence first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startAnalysis()
        }
    }

    private fun openFilePicker() {
        filePickerLauncher.launch("*/*")
    }

    private fun processSelectedFiles(uris: List<Uri>) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE

            uris.forEach { uri ->
                try {
                    val evidence = processFile(uri)
                    if (evidence != null) {
                        evidenceList.add(evidence)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error processing file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                recyclerView.adapter?.notifyDataSetChanged()
                updateEmptyState()
                Toast.makeText(this@MainActivity, "${uris.size} file(s) added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun processFile(uri: Uri): Evidence? = withContext(Dispatchers.IO) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        var fileName = "unknown"
        var fileSize = 0L

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) fileName = it.getString(nameIndex)
                if (sizeIndex >= 0) fileSize = it.getLong(sizeIndex)
            }
        }

        // Copy file to app's internal storage
        val internalFile = File(filesDir, "evidence/$fileName")
        internalFile.parentFile?.mkdirs()

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(internalFile).use { output ->
                input.copyTo(output)
            }
        }

        // Determine evidence type
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val evidenceType = when {
            mimeType.startsWith("image/") -> EvidenceType.IMAGE
            mimeType.startsWith("audio/") -> EvidenceType.AUDIO
            mimeType.startsWith("video/") -> EvidenceType.VIDEO
            mimeType.startsWith("application/pdf") -> EvidenceType.DOCUMENT
            mimeType.startsWith("application/msword") -> EvidenceType.DOCUMENT
            mimeType.startsWith("text/") -> EvidenceType.DOCUMENT
            else -> EvidenceType.UNKNOWN
        }

        // Seal the evidence
        val sealedEvidence = sealingSystem.sealInput(internalFile, evidenceType, currentGpsLocation)
        sealedEvidence.evidence
    }

    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    processSelectedFiles(listOf(uri))
                }
            }
            Intent.ACTION_SEND -> {
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                    processSelectedFiles(listOf(uri))
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris ->
                    processSelectedFiles(uris)
                }
            }
        }
    }

    private fun showEvidenceDetails(evidence: Evidence) {
        // Could launch a detail activity or show a dialog
        val message = """
            File: ${evidence.fileName}
            Type: ${evidence.type.name}
            Size: ${formatFileSize(evidence.fileSize)}
            Added: ${evidence.dateAdded}
            Hash: ${(evidence.metadata["hash"] as? String)?.take(32) ?: "N/A"}...
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Evidence Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Remove") { _, _ ->
                removeEvidence(evidence)
            }
            .show()
    }

    private fun removeEvidence(evidence: Evidence) {
        evidenceList.remove(evidence)
        recyclerView.adapter?.notifyDataSetChanged()
        updateEmptyState()
        
        // Delete file
        File(evidence.filePath).delete()
    }

    private fun startAnalysis() {
        val intent = Intent(this, AnalysisActivity::class.java)
        intent.putExtra(AnalysisActivity.EXTRA_EVIDENCE_IDS, evidenceList.map { it.id }.toTypedArray())
        intent.putExtra(AnalysisActivity.EXTRA_EVIDENCE_PATHS, evidenceList.map { it.filePath }.toTypedArray())
        startActivity(intent)
    }

    private fun updateEmptyState() {
        if (evidenceList.isEmpty()) {
            emptyStateView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            btnAnalyze.isEnabled = false
        } else {
            emptyStateView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            btnAnalyze.isEnabled = true
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    // RecyclerView Adapter
    inner class EvidenceAdapter(
        private val items: List<Evidence>,
        private val onItemClick: (Evidence) -> Unit
    ) : RecyclerView.Adapter<EvidenceAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.iconEvidence)
            val name: TextView = view.findViewById(R.id.textFileName)
            val type: TextView = view.findViewById(R.id.textFileType)
            val size: TextView = view.findViewById(R.id.textFileSize)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_evidence, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val evidence = items[position]
            holder.name.text = evidence.fileName
            holder.type.text = evidence.type.name
            holder.size.text = formatFileSize(evidence.fileSize)

            // Set icon based on type
            val iconRes = when (evidence.type) {
                EvidenceType.IMAGE -> R.drawable.ic_image
                EvidenceType.AUDIO -> R.drawable.ic_audio
                EvidenceType.VIDEO -> R.drawable.ic_video
                EvidenceType.DOCUMENT -> R.drawable.ic_document
                EvidenceType.UNKNOWN -> R.drawable.ic_file
            }
            holder.icon.setImageResource(iconRes)

            holder.itemView.setOnClickListener {
                onItemClick(evidence)
            }
        }

        override fun getItemCount() = items.size
    }
}
