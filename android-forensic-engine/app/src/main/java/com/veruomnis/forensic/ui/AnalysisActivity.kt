package com.veruomnis.forensic.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.veruomnis.forensic.R
import com.veruomnis.forensic.engine.AnalysisProgress
import com.veruomnis.forensic.engine.ForensicEngine
import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * AnalysisActivity - Displays real-time forensic analysis progress
 */
class AnalysisActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVIDENCE_IDS = "evidence_ids"
        const val EXTRA_EVIDENCE_PATHS = "evidence_paths"
    }

    private lateinit var forensicEngine: ForensicEngine
    private lateinit var progressBar: ProgressBar
    private lateinit var textProgress: TextView
    private lateinit var textCurrentBrain: TextView
    private lateinit var recyclerBrains: RecyclerView
    private lateinit var btnViewReport: Button

    private val brainStatuses = mutableListOf<BrainStatus>()
    private var forensicReport: ForensicReport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Forensic Analysis"

        forensicEngine = ForensicEngine(this)
        initViews()
        initBrainStatuses()
        startAnalysis()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBarAnalysis)
        textProgress = findViewById(R.id.textProgress)
        textCurrentBrain = findViewById(R.id.textCurrentBrain)
        recyclerBrains = findViewById(R.id.recyclerBrains)
        btnViewReport = findViewById(R.id.btnViewReport)

        btnViewReport.visibility = View.GONE
        btnViewReport.setOnClickListener {
            forensicReport?.let { report ->
                launchReportActivity(report)
            }
        }

        recyclerBrains.layoutManager = LinearLayoutManager(this)
        recyclerBrains.adapter = BrainStatusAdapter(brainStatuses)
    }

    private fun initBrainStatuses() {
        val brainNames = listOf(
            "Contradiction Brain",
            "Behavioral Diagnostics Brain",
            "Document Authenticity Brain",
            "Timeline & Geolocation Brain",
            "Voice Forensics Brain",
            "Image Validation Brain",
            "Legal & Compliance Brain",
            "Predictive Analytics Brain",
            "Synthesis & Verdict Brain"
        )

        brainNames.forEachIndexed { index, name ->
            brainStatuses.add(BrainStatus(
                brainNumber = index + 1,
                brainName = name,
                status = "Pending",
                progress = 0
            ))
        }
    }

    private fun startAnalysis() {
        val evidenceIds = intent.getStringArrayExtra(EXTRA_EVIDENCE_IDS) ?: return
        val evidencePaths = intent.getStringArrayExtra(EXTRA_EVIDENCE_PATHS) ?: return

        if (evidenceIds.isEmpty()) {
            textProgress.text = "No evidence to analyze"
            return
        }

        // Build evidence list
        val evidenceList = evidenceIds.mapIndexed { index, id ->
            val path = evidencePaths.getOrNull(index) ?: return
            val file = File(path)
            Evidence(
                id = id,
                type = determineEvidenceType(file.extension),
                filePath = path,
                fileName = file.name,
                mimeType = getMimeType(file.extension),
                fileSize = file.length(),
                dateAdded = Date(),
                metadata = emptyMap()
            )
        }

        // Set progress callback
        forensicEngine.setProgressCallback { progress ->
            runOnUiThread {
                updateProgress(progress)
            }
        }

        // Run analysis
        lifecycleScope.launch {
            try {
                forensicReport = forensicEngine.analyzeEvidence(evidenceList)

                withContext(Dispatchers.Main) {
                    textProgress.text = "Analysis Complete!"
                    textCurrentBrain.text = "Nine-Brain Analysis Finished"
                    progressBar.progress = 100
                    btnViewReport.visibility = View.VISIBLE

                    // Update all brains to complete
                    brainStatuses.forEach { it.status = "Complete"; it.progress = 100 }
                    recyclerBrains.adapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textProgress.text = "Analysis Failed: ${e.message}"
                    textCurrentBrain.text = ""
                }
            }
        }
    }

    private fun updateProgress(progress: AnalysisProgress) {
        progressBar.progress = progress.overallProgress.toInt()
        textProgress.text = "${progress.overallProgress.toInt()}% Complete"
        textCurrentBrain.text = if (progress.currentBrainName.isNotEmpty()) {
            "Processing: ${progress.currentBrainName}"
        } else {
            "Initializing..."
        }

        // Update brain statuses
        if (progress.currentBrainIndex > 0) {
            for (i in 0 until progress.currentBrainIndex - 1) {
                if (i < brainStatuses.size) {
                    brainStatuses[i].status = "Complete"
                    brainStatuses[i].progress = 100
                }
            }

            if (progress.currentBrainIndex - 1 < brainStatuses.size) {
                brainStatuses[progress.currentBrainIndex - 1].status = "Processing..."
                brainStatuses[progress.currentBrainIndex - 1].progress = 50
            }

            recyclerBrains.adapter?.notifyDataSetChanged()
        }
    }

    private fun launchReportActivity(report: ForensicReport) {
        val intent = Intent(this, ReportActivity::class.java)
        // Pass report data
        intent.putExtra(ReportActivity.EXTRA_REPORT_ID, report.reportId)
        intent.putExtra(ReportActivity.EXTRA_TSI, report.synthesisResult.truthStabilityIndex)
        intent.putExtra(ReportActivity.EXTRA_VERDICT, report.synthesisResult.overallVerdict)
        intent.putExtra(ReportActivity.EXTRA_CONSENSUS, report.synthesisResult.consensusLevel)
        intent.putExtra(ReportActivity.EXTRA_EVIDENCE_COUNT, report.evidence.size)
        intent.putExtra(ReportActivity.EXTRA_FINDINGS_COUNT, 
            report.brainResults.sumOf { it.findings.size })
        startActivity(intent)
    }

    private fun determineEvidenceType(extension: String): EvidenceType {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic" -> EvidenceType.IMAGE
            "mp3", "wav", "m4a", "ogg", "flac", "aac" -> EvidenceType.AUDIO
            "mp4", "mov", "avi", "mkv", "webm", "3gp" -> EvidenceType.VIDEO
            "pdf", "doc", "docx", "txt", "rtf", "xls", "xlsx" -> EvidenceType.DOCUMENT
            else -> EvidenceType.UNKNOWN
        }
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "mp4" -> "video/mp4"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Data class for brain status
    data class BrainStatus(
        val brainNumber: Int,
        val brainName: String,
        var status: String,
        var progress: Int
    )

    // Adapter for brain status list
    inner class BrainStatusAdapter(
        private val items: List<BrainStatus>
    ) : RecyclerView.Adapter<BrainStatusAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textNumber: TextView = view.findViewById(R.id.textBrainNumber)
            val textName: TextView = view.findViewById(R.id.textBrainName)
            val textStatus: TextView = view.findViewById(R.id.textBrainStatus)
            val progressBar: ProgressBar = view.findViewById(R.id.progressBrain)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_brain_status, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val brain = items[position]
            holder.textNumber.text = "Brain ${brain.brainNumber}"
            holder.textName.text = brain.brainName
            holder.textStatus.text = brain.status
            holder.progressBar.progress = brain.progress

            // Set color based on status
            val textColor = when (brain.status) {
                "Complete" -> getColor(R.color.success_green)
                "Processing..." -> getColor(R.color.primary_blue)
                else -> getColor(R.color.text_secondary)
            }
            holder.textStatus.setTextColor(textColor)
        }

        override fun getItemCount() = items.size
    }
}
