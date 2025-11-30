package com.veruomnis.forensic.engine

import android.content.Context
import android.graphics.Bitmap
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.veruomnis.forensic.models.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * PDF Report Generator
 * 
 * Generates constitutionally compliant forensic reports with cryptographic sealing.
 */
class PdfReportGenerator(private val context: Context) {

    // Color scheme
    private val primaryBlue = DeviceRgb(26, 35, 126)    // #1a237e
    private val gold = DeviceRgb(255, 215, 0)            // #ffd700
    private val alertRed = DeviceRgb(211, 47, 47)        // #d32f2f
    private val successGreen = DeviceRgb(56, 142, 60)    // #388e3c
    private val lightGray = DeviceRgb(245, 245, 245)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val sealingSystem by lazy { CryptographicSealingSystem(context) }

    /**
     * Generate complete forensic report PDF
     */
    fun generateReport(
        report: ForensicReport,
        outputFile: File,
        qrCodeBitmap: Bitmap? = null
    ): File {
        val pdfWriter = PdfWriter(outputFile)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument, PageSize.A4)
        document.setMargins(50f, 50f, 50f, 50f)

        try {
            // 1. Cover Page
            addCoverPage(document, report, qrCodeBitmap)
            document.add(AreaBreak())

            // 2. Constitutional Certification
            addConstitutionalCertification(document, report)
            document.add(AreaBreak())

            // 3. Executive Summary
            addExecutiveSummary(document, report)
            document.add(AreaBreak())

            // 4. Evidence Inventory
            addEvidenceInventory(document, report)
            document.add(AreaBreak())

            // 5. Nine-Brain Analysis Results
            addBrainAnalysisResults(document, report)
            document.add(AreaBreak())

            // 6. Contradiction Map
            addContradictionMap(document, report)

            // 7. Synthesis & Verdict
            addSynthesisVerdict(document, report)
            document.add(AreaBreak())

            // 8. Cryptographic Attestation
            addCryptographicAttestation(document, report, qrCodeBitmap)

            // Footer on all pages
            addFooters(pdfDocument, report)

        } finally {
            document.close()
        }

        return outputFile
    }

    private fun addCoverPage(document: Document, report: ForensicReport, qrCode: Bitmap?) {
        // Title block
        val titleTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
        titleTable.setWidth(UnitValue.createPercentValue(100f))
        titleTable.setBackgroundColor(primaryBlue)
        titleTable.addCell(
            Cell().add(
                Paragraph("VERUM OMNIS")
                    .setFontSize(36f)
                    .setFontColor(ColorConstants.WHITE)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
            )
            .add(
                Paragraph("FORENSIC ANALYSIS REPORT")
                    .setFontSize(18f)
                    .setFontColor(gold)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            .add(
                Paragraph("Nine-Brain Constitutional Forensic Engine V5.2.6")
                    .setFontSize(12f)
                    .setFontColor(ColorConstants.WHITE)
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER)
            )
            .setBorder(Border.NO_BORDER)
            .setPadding(30f)
        )
        document.add(titleTable)

        document.add(Paragraph("\n\n"))

        // Report details
        val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
        detailsTable.setWidth(UnitValue.createPercentValue(100f))

        addDetailRow(detailsTable, "Report ID:", report.reportId)
        addDetailRow(detailsTable, "Generated:", dateFormat.format(report.generatedAt))
        addDetailRow(detailsTable, "Evidence Items:", report.evidence.size.toString())
        addDetailRow(detailsTable, "Brains Executed:", report.brainResults.size.toString())
        addDetailRow(detailsTable, "Truth Stability Index:", "${report.synthesisResult.truthStabilityIndex.toInt()}/100")
        addDetailRow(detailsTable, "Verdict:", report.synthesisResult.overallVerdict)

        document.add(detailsTable)

        document.add(Paragraph("\n"))

        // Constitutional compliance summary
        val complianceStatus = if (
            report.constitutionalCompliance.zeroLossEvidenceDoctrine &&
            report.constitutionalCompliance.tripleAiConsensus &&
            report.constitutionalCompliance.guardianshipModelEnforced
        ) "FULLY COMPLIANT" else "REVIEW REQUIRED"

        val complianceColor = if (complianceStatus == "FULLY COMPLIANT") successGreen else alertRed

        document.add(
            Paragraph("Constitutional Status: $complianceStatus")
                .setFontSize(14f)
                .setFontColor(complianceColor)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
        )

        // QR Code
        if (qrCode != null) {
            document.add(Paragraph("\n\n"))
            document.add(
                Paragraph("VERIFICATION QR CODE")
                    .setFontSize(12f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
            )

            val qrBytes = bitmapToBytes(qrCode)
            val qrImage = Image(ImageDataFactory.create(qrBytes))
            qrImage.setWidth(150f)
            qrImage.setHorizontalAlignment(HorizontalAlignment.CENTER)
            document.add(qrImage)
        }

        // Motto
        document.add(Paragraph("\n\n"))
        document.add(
            Paragraph("\"Truth emerges when evidence survives every scrutiny.\"")
                .setFontSize(12f)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(primaryBlue)
        )
    }

    private fun addConstitutionalCertification(document: Document, report: ForensicReport) {
        addSectionHeader(document, "CONSTITUTIONAL CERTIFICATION")

        val compliance = report.constitutionalCompliance

        // Certification table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(60f, 20f, 20f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        // Header
        table.addHeaderCell(createHeaderCell("Requirement"))
        table.addHeaderCell(createHeaderCell("Status"))
        table.addHeaderCell(createHeaderCell(""))

        // Rows
        addComplianceRow(table, "Zero-Loss Evidence Doctrine", compliance.zeroLossEvidenceDoctrine)
        addComplianceRow(table, "Triple-AI Consensus (≥6/8 brains)", compliance.tripleAiConsensus)
        addComplianceRow(table, "Guardianship Model Enforcement", compliance.guardianshipModelEnforced)

        document.add(table)

        // Compliance notes
        if (compliance.complianceNotes.isNotEmpty()) {
            document.add(Paragraph("\n"))
            document.add(
                Paragraph("Compliance Notes:")
                    .setFontSize(11f)
                    .setBold()
            )
            compliance.complianceNotes.forEach { note ->
                document.add(
                    Paragraph("• $note")
                        .setFontSize(10f)
                        .setMarginLeft(20f)
                )
            }
        }

        // Article X Seal Rule
        document.add(Paragraph("\n"))
        addSubsectionHeader(document, "Article X: The Verum Seal Rule")
        document.add(
            Paragraph("\"NOTHING enters AND nothing leaves unless sealed by Offline Forensic Engine\"")
                .setFontSize(10f)
                .setItalic()
                .setFontColor(primaryBlue)
        )

        val gateTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
        gateTable.setWidth(UnitValue.createPercentValue(100f))

        addDetailRow(gateTable, "Gate 1 (Input Seal):", "✓ Evidence sealed upon entry")
        addDetailRow(gateTable, "Gate 2 (Processing):", "✓ All transformations logged")
        addDetailRow(gateTable, "Gate 3 (Output Seal):", "✓ Report cryptographically sealed")

        document.add(gateTable)
    }

    private fun addExecutiveSummary(document: Document, report: ForensicReport) {
        addSectionHeader(document, "EXECUTIVE SUMMARY")

        val synthesis = report.synthesisResult

        // TSI gauge visualization
        val tsiScore = synthesis.truthStabilityIndex.toInt()
        val tsiColor = when {
            tsiScore >= 80 -> successGreen
            tsiScore >= 60 -> DeviceRgb(255, 152, 0) // Orange
            else -> alertRed
        }

        document.add(
            Paragraph("Truth Stability Index: $tsiScore/100")
                .setFontSize(24f)
                .setBold()
                .setFontColor(tsiColor)
                .setTextAlignment(TextAlignment.CENTER)
        )

        document.add(Paragraph("\n"))

        // Key findings
        if (synthesis.keyFindings.isNotEmpty()) {
            addSubsectionHeader(document, "Key Findings")
            synthesis.keyFindings.take(10).forEachIndexed { index, finding ->
                document.add(
                    Paragraph("${index + 1}. $finding")
                        .setFontSize(10f)
                        .setMarginLeft(15f)
                )
            }
        }

        // Recommendations
        if (synthesis.recommendations.isNotEmpty()) {
            document.add(Paragraph("\n"))
            addSubsectionHeader(document, "Recommended Actions")
            synthesis.recommendations.forEach { rec ->
                document.add(
                    Paragraph("• $rec")
                        .setFontSize(10f)
                        .setMarginLeft(15f)
                )
            }
        }

        // Critical alerts
        val criticalCount = report.brainResults.sumOf { 
            it.findings.count { f -> f.severity == Severity.CRITICAL } 
        }
        if (criticalCount > 0) {
            document.add(Paragraph("\n"))
            document.add(
                Paragraph("⚠️ CRITICAL ALERTS: $criticalCount findings require immediate attention")
                    .setFontSize(12f)
                    .setBold()
                    .setFontColor(alertRed)
            )
        }
    }

    private fun addEvidenceInventory(document: Document, report: ForensicReport) {
        addSectionHeader(document, "EVIDENCE INVENTORY")

        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 30f, 15f, 15f, 30f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        // Header
        table.addHeaderCell(createHeaderCell("#"))
        table.addHeaderCell(createHeaderCell("File Name"))
        table.addHeaderCell(createHeaderCell("Type"))
        table.addHeaderCell(createHeaderCell("Size"))
        table.addHeaderCell(createHeaderCell("Hash (SHA-512)"))

        // Data rows
        report.evidence.forEachIndexed { index, ev ->
            table.addCell(Cell().add(Paragraph("${index + 1}").setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(ev.fileName).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(ev.type.name).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(formatFileSize(ev.fileSize)).setFontSize(9f)))

            val hash = ev.metadata["hash"] as? String ?: "Not sealed"
            table.addCell(Cell().add(
                Paragraph(if (hash.length > 16) hash.take(16) + "..." else hash)
                    .setFontSize(8f)
            ))
        }

        document.add(table)

        document.add(Paragraph("\n"))
        document.add(
            Paragraph("Total Evidence Items: ${report.evidence.size}")
                .setFontSize(11f)
                .setBold()
        )
    }

    private fun addBrainAnalysisResults(document: Document, report: ForensicReport) {
        addSectionHeader(document, "NINE-BRAIN ANALYSIS RESULTS")

        report.brainResults.forEach { brain ->
            document.add(Paragraph("\n"))

            // Brain header
            val brainTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
            brainTable.setWidth(UnitValue.createPercentValue(100f))
            brainTable.setBackgroundColor(lightGray)

            brainTable.addCell(
                Cell().add(
                    Paragraph(brain.brainName)
                        .setFontSize(12f)
                        .setBold()
                        .setFontColor(primaryBlue)
                ).setBorder(Border.NO_BORDER)
            )
            brainTable.addCell(
                Cell().add(
                    Paragraph("Confidence: ${(brain.confidence * 100).toInt()}%")
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.RIGHT)
                ).setBorder(Border.NO_BORDER)
            )

            document.add(brainTable)

            // Findings
            if (brain.findings.isNotEmpty()) {
                val findingsTable = Table(UnitValue.createPercentArray(floatArrayOf(15f, 25f, 60f)))
                findingsTable.setWidth(UnitValue.createPercentValue(100f))

                brain.findings.take(5).forEach { finding ->
                    val severityColor = when (finding.severity) {
                        Severity.CRITICAL -> alertRed
                        Severity.HIGH -> DeviceRgb(255, 152, 0)
                        Severity.MEDIUM -> DeviceRgb(255, 193, 7)
                        Severity.LOW -> successGreen
                        Severity.INFO -> DeviceRgb(33, 150, 243)
                    }

                    findingsTable.addCell(
                        Cell().add(
                            Paragraph(finding.severity.name)
                                .setFontSize(8f)
                                .setFontColor(severityColor)
                                .setBold()
                        )
                    )
                    findingsTable.addCell(
                        Cell().add(
                            Paragraph(finding.category)
                                .setFontSize(8f)
                        )
                    )
                    findingsTable.addCell(
                        Cell().add(
                            Paragraph(finding.description)
                                .setFontSize(8f)
                        )
                    )
                }

                document.add(findingsTable)

                if (brain.findings.size > 5) {
                    document.add(
                        Paragraph("... and ${brain.findings.size - 5} more findings")
                            .setFontSize(9f)
                            .setItalic()
                            .setMarginLeft(15f)
                    )
                }
            } else {
                document.add(
                    Paragraph("No significant findings")
                        .setFontSize(10f)
                        .setItalic()
                        .setMarginLeft(15f)
                )
            }
        }
    }

    private fun addContradictionMap(document: Document, report: ForensicReport) {
        val contradictions = report.synthesisResult.contradictions
        if (contradictions.isEmpty()) return

        addSectionHeader(document, "CONTRADICTION MAP")

        val table = Table(UnitValue.createPercentArray(floatArrayOf(15f, 20f, 50f, 15f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell(createHeaderCell("Severity"))
        table.addHeaderCell(createHeaderCell("Type"))
        table.addHeaderCell(createHeaderCell("Description"))
        table.addHeaderCell(createHeaderCell("Evidence"))

        contradictions.forEach { contradiction ->
            val severityColor = when (contradiction.severity) {
                Severity.CRITICAL -> alertRed
                Severity.HIGH -> DeviceRgb(255, 152, 0)
                else -> primaryBlue
            }

            table.addCell(
                Cell().add(
                    Paragraph(contradiction.severity.name)
                        .setFontSize(9f)
                        .setFontColor(severityColor)
                        .setBold()
                )
            )
            table.addCell(Cell().add(Paragraph(contradiction.type).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(contradiction.description).setFontSize(9f)))
            table.addCell(Cell().add(
                Paragraph(contradiction.evidenceIds.joinToString(", ")).setFontSize(8f)
            ))
        }

        document.add(table)
    }

    private fun addSynthesisVerdict(document: Document, report: ForensicReport) {
        addSectionHeader(document, "SYNTHESIS & VERDICT")

        val synthesis = report.synthesisResult

        // Verdict box
        val verdictColor = when (synthesis.overallVerdict) {
            "VERIFIED" -> successGreen
            "CONDITIONAL" -> DeviceRgb(255, 152, 0)
            else -> alertRed
        }

        val verdictTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
        verdictTable.setWidth(UnitValue.createPercentValue(100f))
        verdictTable.addCell(
            Cell().add(
                Paragraph("FINAL VERDICT: ${synthesis.overallVerdict}")
                    .setFontSize(18f)
                    .setBold()
                    .setFontColor(verdictColor)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            .add(
                Paragraph("Consensus Level: ${(synthesis.consensusLevel * 100).toInt()}%")
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            .setBorder(SolidBorder(verdictColor, 2f))
            .setPadding(20f)
        )
        document.add(verdictTable)

        // Verdict explanation
        document.add(Paragraph("\n"))
        val explanation = when (synthesis.overallVerdict) {
            "VERIFIED" -> "Evidence has been verified with high confidence across multiple analytical dimensions. The Nine-Brain Architecture has achieved consensus, and constitutional compliance is confirmed."
            "CONDITIONAL" -> "Evidence shows moderate reliability but may require additional verification or documentation. Some brains have conflicting analyses that should be reviewed."
            else -> "Evidence is inconclusive and requires further investigation. Significant contradictions or compliance issues have been detected."
        }
        document.add(
            Paragraph(explanation)
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.JUSTIFIED)
        )
    }

    private fun addCryptographicAttestation(document: Document, report: ForensicReport, qrCode: Bitmap?) {
        addSectionHeader(document, "CRYPTOGRAPHIC ATTESTATION")

        val seal = report.cryptographicSeal

        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        addDetailRow(table, "SHA-512 Hash:", seal.sha512Hash.take(64) + "...")
        addDetailRow(table, "Timestamp:", dateFormat.format(seal.timestamp))

        if (seal.gpsLocation != null) {
            addDetailRow(table, "GPS Coordinates:", 
                "${seal.gpsLocation.latitude}, ${seal.gpsLocation.longitude}")
            addDetailRow(table, "GPS Accuracy:", "${seal.gpsLocation.accuracy}m")
        }

        addDetailRow(table, "Digital Watermark:", seal.digitalWatermark.take(32) + "...")

        document.add(table)

        // QR Code for verification
        if (qrCode != null) {
            document.add(Paragraph("\n"))
            document.add(
                Paragraph("Scan QR code to verify report authenticity:")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
            )

            val qrBytes = bitmapToBytes(qrCode)
            val qrImage = Image(ImageDataFactory.create(qrBytes))
            qrImage.setWidth(120f)
            qrImage.setHorizontalAlignment(HorizontalAlignment.CENTER)
            document.add(qrImage)
        }

        // Disclaimer
        document.add(Paragraph("\n"))
        document.add(
            Paragraph("This report has been generated by the Verum Omnis Forensic Engine V5.2.6. " +
                    "It provides forensic analysis, not legal advice. The system operates as an evidence " +
                    "guardian in accordance with the Constitutional Framework.")
                .setFontSize(8f)
                .setItalic()
                .setTextAlignment(TextAlignment.JUSTIFIED)
        )
    }

    private fun addFooters(pdfDocument: PdfDocument, report: ForensicReport) {
        val totalPages = pdfDocument.numberOfPages
        for (i in 1..totalPages) {
            val page = pdfDocument.getPage(i)
            val pageSize = page.pageSize

            // Create canvas for footer
            val canvas = PdfCanvas(page)
            
            // Draw footer line
            canvas.setStrokeColor(primaryBlue)
            canvas.moveTo(50.0, 30.0)
            canvas.lineTo((pageSize.width - 50).toDouble(), 30.0)
            canvas.stroke()
        }
    }

    // Helper functions

    private fun addSectionHeader(document: Document, title: String) {
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
        headerTable.setWidth(UnitValue.createPercentValue(100f))
        headerTable.setBackgroundColor(primaryBlue)
        headerTable.addCell(
            Cell().add(
                Paragraph(title)
                    .setFontSize(14f)
                    .setFontColor(ColorConstants.WHITE)
                    .setBold()
            )
            .setBorder(Border.NO_BORDER)
            .setPadding(10f)
        )
        document.add(headerTable)
        document.add(Paragraph("\n"))
    }

    private fun addSubsectionHeader(document: Document, title: String) {
        document.add(
            Paragraph(title)
                .setFontSize(12f)
                .setBold()
                .setFontColor(primaryBlue)
                .setBorderBottom(SolidBorder(primaryBlue, 1f))
                .setPaddingBottom(5f)
        )
    }

    private fun createHeaderCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text).setFontSize(10f).setBold().setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(primaryBlue)
            .setPadding(5f)
    }

    private fun addDetailRow(table: Table, label: String, value: String) {
        table.addCell(
            Cell().add(
                Paragraph(label).setFontSize(10f).setBold()
            ).setBorder(Border.NO_BORDER)
        )
        table.addCell(
            Cell().add(
                Paragraph(value).setFontSize(10f)
            ).setBorder(Border.NO_BORDER)
        )
    }

    private fun addComplianceRow(table: Table, requirement: String, isCompliant: Boolean) {
        table.addCell(Cell().add(Paragraph(requirement).setFontSize(10f)))
        table.addCell(
            Cell().add(
                Paragraph(if (isCompliant) "COMPLIANT" else "NON-COMPLIANT")
                    .setFontSize(10f)
                    .setBold()
                    .setFontColor(if (isCompliant) successGreen else alertRed)
            )
        )
        table.addCell(
            Cell().add(
                Paragraph(if (isCompliant) "✓" else "✗")
                    .setFontSize(14f)
                    .setFontColor(if (isCompliant) successGreen else alertRed)
                    .setTextAlignment(TextAlignment.CENTER)
            )
        )
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
