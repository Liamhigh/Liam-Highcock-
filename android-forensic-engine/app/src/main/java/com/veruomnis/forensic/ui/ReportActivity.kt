package com.veruomnis.forensic.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.veruomnis.forensic.R
import com.veruomnis.forensic.engine.CryptographicSealingSystem
import com.veruomnis.forensic.engine.PdfReportGenerator
import com.veruomnis.forensic.models.*
import java.io.File
import java.util.Date

/**
 * ReportActivity - Displays the forensic report and allows export
 */
class ReportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REPORT_ID = "report_id"
        const val EXTRA_TSI = "tsi"
        const val EXTRA_VERDICT = "verdict"
        const val EXTRA_CONSENSUS = "consensus"
        const val EXTRA_EVIDENCE_COUNT = "evidence_count"
        const val EXTRA_FINDINGS_COUNT = "findings_count"
    }

    private lateinit var textReportId: TextView
    private lateinit var textTsi: TextView
    private lateinit var textVerdict: TextView
    private lateinit var textConsensus: TextView
    private lateinit var textEvidenceCount: TextView
    private lateinit var textFindingsCount: TextView
    private lateinit var imgQrCode: ImageView
    private lateinit var btnExportPdf: Button
    private lateinit var btnShareReport: Button

    private lateinit var sealingSystem: CryptographicSealingSystem
    private var reportId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Forensic Report"

        sealingSystem = CryptographicSealingSystem(this)
        initViews()
        loadReportData()
    }

    private fun initViews() {
        textReportId = findViewById(R.id.textReportId)
        textTsi = findViewById(R.id.textTsi)
        textVerdict = findViewById(R.id.textVerdict)
        textConsensus = findViewById(R.id.textConsensus)
        textEvidenceCount = findViewById(R.id.textEvidenceCount)
        textFindingsCount = findViewById(R.id.textFindingsCount)
        imgQrCode = findViewById(R.id.imgQrCode)
        btnExportPdf = findViewById(R.id.btnExportPdf)
        btnShareReport = findViewById(R.id.btnShareReport)

        btnExportPdf.setOnClickListener {
            exportPdf()
        }

        btnShareReport.setOnClickListener {
            shareReport()
        }
    }

    private fun loadReportData() {
        reportId = intent.getStringExtra(EXTRA_REPORT_ID) ?: "N/A"
        val tsi = intent.getDoubleExtra(EXTRA_TSI, 0.0)
        val verdict = intent.getStringExtra(EXTRA_VERDICT) ?: "INCONCLUSIVE"
        val consensus = intent.getDoubleExtra(EXTRA_CONSENSUS, 0.0)
        val evidenceCount = intent.getIntExtra(EXTRA_EVIDENCE_COUNT, 0)
        val findingsCount = intent.getIntExtra(EXTRA_FINDINGS_COUNT, 0)

        textReportId.text = "Report ID: $reportId"
        textTsi.text = "Truth Stability Index: ${tsi.toInt()}/100"
        textVerdict.text = "Verdict: $verdict"
        textConsensus.text = "Consensus: ${(consensus * 100).toInt()}%"
        textEvidenceCount.text = "Evidence Analyzed: $evidenceCount"
        textFindingsCount.text = "Total Findings: $findingsCount"

        // Set verdict color
        val verdictColor = when (verdict) {
            "VERIFIED" -> getColor(R.color.success_green)
            "CONDITIONAL" -> getColor(R.color.warning_orange)
            else -> getColor(R.color.alert_red)
        }
        textVerdict.setTextColor(verdictColor)

        // Generate and display QR code
        generateQrCode()
    }

    private fun generateQrCode() {
        try {
            val qrData = "VO|$reportId|${System.currentTimeMillis()}"
            val qrBitmap = sealingSystem.generateQrCodeBitmap(qrData, 256)
            imgQrCode.setImageBitmap(qrBitmap)
        } catch (e: Exception) {
            imgQrCode.visibility = View.GONE
        }
    }

    private fun exportPdf() {
        try {
            val reportFile = File(filesDir, "reports/$reportId.pdf")
            reportFile.parentFile?.mkdirs()

            // Create a minimal report for PDF generation
            val report = createMinimalReport()
            val qrData = "VO|$reportId|${System.currentTimeMillis()}"
            val qrBitmap = sealingSystem.generateQrCodeBitmap(qrData, 256)

            val pdfGenerator = PdfReportGenerator(this)
            pdfGenerator.generateReport(report, reportFile, qrBitmap)

            // Open the PDF
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                reportFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this,
                "Error exporting PDF: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun shareReport() {
        try {
            val reportFile = File(filesDir, "reports/$reportId.pdf")
            if (!reportFile.exists()) {
                exportPdf() // Generate first
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                reportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Verum Omnis Forensic Report - $reportId")
                putExtra(Intent.EXTRA_TEXT, 
                    "Forensic Report $reportId\n" +
                    "Truth Stability Index: ${textTsi.text}\n" +
                    "Verdict: ${textVerdict.text}"
                )
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(shareIntent, "Share Report"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this,
                "Error sharing report: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createMinimalReport(): ForensicReport {
        val tsi = intent.getDoubleExtra(EXTRA_TSI, 0.0)
        val verdict = intent.getStringExtra(EXTRA_VERDICT) ?: "INCONCLUSIVE"
        val consensus = intent.getDoubleExtra(EXTRA_CONSENSUS, 0.0)

        return ForensicReport(
            reportId = reportId,
            evidence = emptyList(),
            brainResults = emptyList(),
            synthesisResult = SynthesisResult(
                truthStabilityIndex = tsi,
                overallVerdict = verdict,
                consensusLevel = consensus,
                keyFindings = listOf(
                    "Analysis completed with ${intent.getIntExtra(EXTRA_FINDINGS_COUNT, 0)} findings"
                ),
                contradictions = emptyList(),
                recommendations = listOf("Review detailed findings in the full report")
            ),
            cryptographicSeal = CryptographicSeal(
                sha512Hash = "Generated on export",
                timestamp = Date(),
                gpsLocation = null,
                qrCodeData = "VO|$reportId|${System.currentTimeMillis()}",
                digitalWatermark = "VO-SEAL-$reportId"
            ),
            generatedAt = Date(),
            constitutionalCompliance = ConstitutionalCompliance(
                zeroLossEvidenceDoctrine = true,
                tripleAiConsensus = consensus >= 0.75,
                guardianshipModelEnforced = true,
                complianceNotes = listOf("Report generated by Verum Omnis Forensic Engine V5.2.6")
            )
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
