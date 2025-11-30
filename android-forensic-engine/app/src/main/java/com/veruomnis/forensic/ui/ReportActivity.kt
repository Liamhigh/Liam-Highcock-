package com.veruomnis.forensic.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.veruomnis.forensic.ForensicApplication
import com.veruomnis.forensic.R
import com.veruomnis.forensic.crypto.CryptographicSealer
import com.veruomnis.forensic.databinding.ActivityReportBinding
import com.veruomnis.forensic.models.*
import com.veruomnis.forensic.report.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ReportActivity - Display and export forensic report
 * 
 * Features:
 * - Display cryptographic seal with QR code
 * - Show Truth Stability Index
 * - List findings by severity
 * - Display constitutional compliance
 * - Export to PDF
 * - Share report
 */
class ReportActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_REPORT_ID = "report_id"
    }
    
    private lateinit var binding: ActivityReportBinding
    private var currentReport: ForensicReport? = null
    private val sealer = CryptographicSealer()
    private val pdfGenerator = PdfReportGenerator()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupButtons()
        
        // Get report
        val reportId = intent.getStringExtra(EXTRA_REPORT_ID)
        currentReport = AnalysisActivity.getLastReport()
        
        if (currentReport == null) {
            Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        displayReport(currentReport!!)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
    
    private fun setupButtons() {
        binding.exportPdfButton.setOnClickListener {
            currentReport?.let { exportToPdf(it) }
        }
        
        binding.shareButton.setOnClickListener {
            currentReport?.let { shareReport(it) }
        }
    }
    
    private fun displayReport(report: ForensicReport) {
        // Display seal information
        binding.sealHash.text = "SHA-512: ${report.cryptographicSeal.sha512Hash.take(64)}..."
        
        // Generate and display QR code
        lifecycleScope.launch {
            val qrBitmap = withContext(Dispatchers.Default) {
                sealer.generateQrCodeBitmap(report.cryptographicSeal, 300)
            }
            binding.qrCodeImage.setImageBitmap(qrBitmap)
        }
        
        // Display Truth Stability Index
        val tsi = report.synthesisResult.truthStabilityIndex.toInt()
        binding.tsiValue.text = tsi.toString()
        binding.tsiProgress.progress = tsi
        
        // Display findings summary
        val allFindings = report.brainResults.flatMap { it.findings }
        binding.criticalCount.text = allFindings.count { it.severity == Severity.CRITICAL }.toString()
        binding.highCount.text = allFindings.count { it.severity == Severity.HIGH }.toString()
        binding.mediumCount.text = allFindings.count { it.severity == Severity.MEDIUM }.toString()
        binding.lowCount.text = allFindings.count { it.severity == Severity.LOW }.toString()
        
        // Setup findings RecyclerView (simplified)
        binding.findingsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Display constitutional compliance
        val compliance = report.constitutionalCompliance
        updateComplianceStatus(binding.zeroLossStatus, compliance.zeroLossEvidenceDoctrine)
        updateComplianceStatus(binding.tripleVerifyStatus, compliance.tripleAiConsensus)
        updateComplianceStatus(binding.sealRuleStatus, compliance.guardianshipModelEnforced)
    }
    
    private fun updateComplianceStatus(view: View, isCompliant: Boolean) {
        val textView = view as? android.widget.TextView ?: return
        textView.setTextColor(
            if (isCompliant) getColor(R.color.accent_green) else getColor(R.color.accent_red)
        )
    }
    
    private fun exportToPdf(report: ForensicReport) {
        lifecycleScope.launch {
            try {
                binding.exportPdfButton.isEnabled = false
                binding.exportPdfButton.text = "Generating..."
                
                val pdfFile = withContext(Dispatchers.IO) {
                    val reportsDir = ForensicApplication.getInstance().getReportsDirectory()
                    val fileName = "verum_omnis_report_${report.reportId.take(8)}.pdf"
                    val file = File(reportsDir, fileName)
                    
                    pdfGenerator.generateReport(report, file)
                    file
                }
                
                binding.exportPdfButton.text = getString(R.string.export_pdf)
                binding.exportPdfButton.isEnabled = true
                
                Toast.makeText(
                    this@ReportActivity,
                    "PDF saved: ${pdfFile.name}",
                    Toast.LENGTH_LONG
                ).show()
                
                // Offer to open/share
                openPdf(pdfFile)
                
            } catch (e: Exception) {
                binding.exportPdfButton.text = getString(R.string.export_pdf)
                binding.exportPdfButton.isEnabled = true
                
                Toast.makeText(
                    this@ReportActivity,
                    "PDF generation failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun openPdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            startActivity(Intent.createChooser(intent, "Open PDF"))
        } catch (e: Exception) {
            Toast.makeText(this, "No PDF viewer available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareReport(report: ForensicReport) {
        lifecycleScope.launch {
            try {
                // Generate PDF first
                val pdfFile = withContext(Dispatchers.IO) {
                    val reportsDir = ForensicApplication.getInstance().getReportsDirectory()
                    val fileName = "verum_omnis_report_${report.reportId.take(8)}.pdf"
                    val file = File(reportsDir, fileName)
                    
                    if (!file.exists()) {
                        pdfGenerator.generateReport(report, file)
                    }
                    file
                }
                
                val uri = FileProvider.getUriForFile(
                    this@ReportActivity,
                    "${packageName}.fileprovider",
                    pdfFile
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Verum Omnis Forensic Report")
                    putExtra(Intent.EXTRA_TEXT, buildShareText(report))
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                startActivity(Intent.createChooser(shareIntent, "Share Report"))
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@ReportActivity,
                    "Share failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun buildShareText(report: ForensicReport): String {
        return buildString {
            appendLine("VERUM OMNIS FORENSIC REPORT")
            appendLine("============================")
            appendLine()
            appendLine("Report ID: ${report.reportId}")
            appendLine("Generated: ${report.generatedAt}")
            appendLine()
            appendLine("Truth Stability Index: ${report.synthesisResult.truthStabilityIndex.toInt()}/100")
            appendLine("Verdict: ${report.synthesisResult.overallVerdict}")
            appendLine()
            appendLine("Evidence Items: ${report.evidence.size}")
            appendLine("Brains Consulted: ${report.brainResults.size}")
            appendLine("Total Findings: ${report.brainResults.sumOf { it.findings.size }}")
            appendLine()
            appendLine("SHA-512 Seal: ${report.cryptographicSeal.sha512Hash.take(32)}...")
            appendLine()
            appendLine("Nine Brains. One Truth.")
            appendLine("Verum Omnis V5.2.6")
        }
    }
}
