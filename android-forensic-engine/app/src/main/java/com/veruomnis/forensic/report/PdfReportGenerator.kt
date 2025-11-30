package com.veruomnis.forensic.report

import android.graphics.Bitmap
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.veruomnis.forensic.crypto.CryptographicSealer
import com.veruomnis.forensic.models.*
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * PDF Report Generator
 * 
 * Generates court-admissible forensic reports with:
 * - Cryptographic sealing (SHA-512)
 * - QR code for verification
 * - Digital watermarking
 * - Constitutional compliance certification
 */
class PdfReportGenerator {
    
    private val sealer = CryptographicSealer()
    
    // Verum Omnis brand colors
    private val primaryColor = DeviceRgb(30, 58, 95) // #1E3A5F
    private val goldColor = DeviceRgb(212, 175, 55) // #D4AF37
    private val textColor = DeviceRgb(232, 244, 253) // #E8F4FD
    private val backgroundColor = DeviceRgb(10, 25, 41) // #0A1929
    
    /**
     * Generate a complete forensic report PDF
     */
    fun generateReport(report: ForensicReport, outputFile: File) {
        val writer = PdfWriter(outputFile)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        
        document.setMargins(40f, 40f, 40f, 40f)
        
        try {
            // Title Page
            addTitlePage(document, report)
            
            // Executive Summary
            addExecutiveSummary(document, report)
            
            // Truth Stability Index
            addTruthStabilitySection(document, report)
            
            // Nine Brain Analysis Results
            addBrainAnalysisSection(document, report)
            
            // Detailed Findings
            addFindingsSection(document, report)
            
            // Constitutional Compliance
            addComplianceSection(document, report)
            
            // Cryptographic Seal
            addSealSection(document, report)
            
            // Footer on all pages
            addFooter(document, report)
            
        } finally {
            document.close()
        }
    }
    
    private fun addTitlePage(document: Document, report: ForensicReport) {
        // Title
        val title = Paragraph("VERUM OMNIS")
            .setFontSize(36f)
            .setBold()
            .setFontColor(goldColor)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(title)
        
        val subtitle = Paragraph("FORENSIC ANALYSIS REPORT")
            .setFontSize(24f)
            .setFontColor(primaryColor)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(subtitle)
        
        document.add(Paragraph("\n"))
        
        // Motto
        val motto = Paragraph("Nine Brains. One Truth.")
            .setFontSize(16f)
            .setItalic()
            .setTextAlignment(TextAlignment.CENTER)
        document.add(motto)
        
        document.add(Paragraph("\n\n"))
        
        // Report Info Table
        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f)))
            .setWidth(UnitValue.createPercentValue(80f))
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
        
        addTableRow(infoTable, "Report ID:", report.reportId)
        addTableRow(infoTable, "Generated:", report.generatedAt.toString())
        addTableRow(infoTable, "Evidence Items:", report.evidence.size.toString())
        addTableRow(infoTable, "Brains Consulted:", report.brainResults.size.toString())
        addTableRow(infoTable, "Version:", "V5.2.6")
        
        document.add(infoTable)
        
        document.add(AreaBreak())
    }
    
    private fun addExecutiveSummary(document: Document, report: ForensicReport) {
        addSectionHeader(document, "EXECUTIVE SUMMARY")
        
        val verdict = Paragraph(report.synthesisResult.overallVerdict)
            .setFontSize(14f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(10f)
            .setBackgroundColor(DeviceRgb(240, 240, 240))
        document.add(verdict)
        
        document.add(Paragraph("\n"))
        
        // Key Findings
        if (report.synthesisResult.keyFindings.isNotEmpty()) {
            document.add(Paragraph("Key Findings:").setBold())
            val list = List()
            report.synthesisResult.keyFindings.forEach { finding ->
                list.add(ListItem(finding))
            }
            document.add(list)
        }
        
        // Recommendations
        if (report.synthesisResult.recommendations.isNotEmpty()) {
            document.add(Paragraph("\nRecommendations:").setBold())
            val recList = List()
            report.synthesisResult.recommendations.forEach { rec ->
                recList.add(ListItem(rec))
            }
            document.add(recList)
        }
        
        document.add(Paragraph("\n"))
    }
    
    private fun addTruthStabilitySection(document: Document, report: ForensicReport) {
        addSectionHeader(document, "TRUTH STABILITY INDEX")
        
        val tsi = report.synthesisResult.truthStabilityIndex
        val tsiText = Paragraph(String.format("%.1f / 100", tsi))
            .setFontSize(48f)
            .setBold()
            .setFontColor(goldColor)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(tsiText)
        
        val interpretation = when {
            tsi > 80 -> "HIGH STABILITY - Evidence strongly supports conclusions"
            tsi > 60 -> "MODERATE STABILITY - Evidence generally consistent"
            tsi > 40 -> "LOW STABILITY - Significant inconsistencies detected"
            else -> "CRITICAL - Major contradictions or missing evidence"
        }
        
        document.add(Paragraph(interpretation)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic())
        
        // Consensus Level
        document.add(Paragraph("\nCross-Brain Consensus: ${(report.synthesisResult.consensusLevel * 100).toInt()}%")
            .setTextAlignment(TextAlignment.CENTER))
        
        document.add(Paragraph("\n"))
    }
    
    private fun addBrainAnalysisSection(document: Document, report: ForensicReport) {
        addSectionHeader(document, "NINE-BRAIN ANALYSIS")
        
        val brainTable = Table(UnitValue.createPercentArray(floatArrayOf(0.5f, 2f, 1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        // Header
        brainTable.addHeaderCell(createHeaderCell("#"))
        brainTable.addHeaderCell(createHeaderCell("Brain"))
        brainTable.addHeaderCell(createHeaderCell("Confidence"))
        brainTable.addHeaderCell(createHeaderCell("Findings"))
        
        report.brainResults.forEachIndexed { index, result ->
            brainTable.addCell(Cell().add(Paragraph("${index + 1}")))
            brainTable.addCell(Cell().add(Paragraph(result.brainName)))
            brainTable.addCell(Cell().add(Paragraph("${(result.confidence * 100).toInt()}%")))
            brainTable.addCell(Cell().add(Paragraph("${result.findings.size}")))
        }
        
        document.add(brainTable)
        document.add(Paragraph("\n"))
    }
    
    private fun addFindingsSection(document: Document, report: ForensicReport) {
        addSectionHeader(document, "DETAILED FINDINGS")
        
        // Group findings by severity
        val allFindings = report.brainResults.flatMap { result ->
            result.findings.map { it to result.brainName }
        }
        
        val grouped = allFindings.groupBy { it.first.severity }
        
        // Severity order
        val severityOrder = listOf(
            Severity.CRITICAL to DeviceRgb(211, 47, 47),
            Severity.HIGH to DeviceRgb(244, 67, 54),
            Severity.MEDIUM to DeviceRgb(255, 152, 0),
            Severity.LOW to DeviceRgb(255, 193, 7),
            Severity.INFO to DeviceRgb(33, 150, 243)
        )
        
        severityOrder.forEach { (severity, color) ->
            val findings = grouped[severity] ?: return@forEach
            
            document.add(Paragraph("${severity.name} (${findings.size})")
                .setBold()
                .setFontColor(color))
            
            findings.forEach { (finding, brainName) ->
                val findingPara = Paragraph()
                    .add(Text("• ").setBold())
                    .add(Text("[${finding.category}] ").setItalic())
                    .add(Text(finding.description))
                    .add(Text(" (${brainName})").setFontSize(10f).setItalic())
                document.add(findingPara)
            }
            
            document.add(Paragraph(""))
        }
    }
    
    private fun addComplianceSection(document: Document, report: ForensicReport) {
        addSectionHeader(document, "CONSTITUTIONAL COMPLIANCE")
        
        val compliance = report.constitutionalCompliance
        val checkMark = "✓"
        val crossMark = "✗"
        
        val complianceTable = Table(UnitValue.createPercentArray(floatArrayOf(3f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        complianceTable.addCell(Cell().add(Paragraph("Zero-Loss Evidence Doctrine")))
        complianceTable.addCell(Cell().add(Paragraph(if (compliance.zeroLossEvidenceDoctrine) checkMark else crossMark)
            .setFontColor(if (compliance.zeroLossEvidenceDoctrine) DeviceRgb(76, 175, 80) else DeviceRgb(244, 67, 54))))
        
        complianceTable.addCell(Cell().add(Paragraph("Triple-AI Consensus Verification")))
        complianceTable.addCell(Cell().add(Paragraph(if (compliance.tripleAiConsensus) checkMark else crossMark)
            .setFontColor(if (compliance.tripleAiConsensus) DeviceRgb(76, 175, 80) else DeviceRgb(244, 67, 54))))
        
        complianceTable.addCell(Cell().add(Paragraph("Guardianship Model Enforced")))
        complianceTable.addCell(Cell().add(Paragraph(if (compliance.guardianshipModelEnforced) checkMark else crossMark)
            .setFontColor(if (compliance.guardianshipModelEnforced) DeviceRgb(76, 175, 80) else DeviceRgb(244, 67, 54))))
        
        document.add(complianceTable)
        
        // Compliance notes
        if (compliance.complianceNotes.isNotEmpty()) {
            document.add(Paragraph("\nNotes:").setItalic())
            compliance.complianceNotes.forEach { note ->
                document.add(Paragraph("• $note").setFontSize(10f))
            }
        }
        
        document.add(Paragraph("\n"))
    }
    
    private fun addSealSection(document: Document, report: ForensicReport) {
        addSectionHeader(document, "CRYPTOGRAPHIC SEAL")
        
        val seal = report.cryptographicSeal
        
        // Seal info
        document.add(Paragraph("Article X: The Verum Seal Rule")
            .setBold()
            .setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("\"NOTHING enters AND nothing leaves unless sealed\"")
            .setItalic()
            .setTextAlignment(TextAlignment.CENTER))
        
        document.add(Paragraph("\n"))
        
        // SHA-512 Hash
        document.add(Paragraph("SHA-512 Hash:").setBold())
        document.add(Paragraph(seal.sha512Hash)
            .setFontSize(8f)
            .setBackgroundColor(DeviceRgb(240, 240, 240))
            .setPadding(5f))
        
        // Timestamp
        document.add(Paragraph("\nSealed At: ${seal.timestamp}"))
        
        // GPS if available
        seal.gpsLocation?.let { gps ->
            document.add(Paragraph("GPS: ${gps.latitude}, ${gps.longitude} (±${gps.accuracy}m)"))
        }
        
        // Watermark
        document.add(Paragraph("\nDigital Watermark: ${seal.digitalWatermark}"))
        
        // QR Code
        document.add(Paragraph("\nVerification QR Code:").setBold())
        try {
            val qrBitmap = sealer.generateQrCodeBitmap(seal, 200)
            val qrBytes = bitmapToBytes(qrBitmap)
            val qrImage = Image(ImageDataFactory.create(qrBytes))
                .setWidth(150f)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
            document.add(qrImage)
        } catch (e: Exception) {
            document.add(Paragraph("QR Code: ${seal.qrCodeData}").setFontSize(10f))
        }
        
        document.add(Paragraph("Scan to verify report integrity")
            .setItalic()
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER))
    }
    
    private fun addFooter(document: Document, report: ForensicReport) {
        // Footer is added to each page
        val footer = Paragraph("Verum Omnis V5.2.6 | Report ID: ${report.reportId.take(8)} | ${report.generatedAt}")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
        
        // Note: In a full implementation, use PdfDocument.addEventHandler for proper page footers
    }
    
    private fun addSectionHeader(document: Document, title: String) {
        document.add(Paragraph(title)
            .setFontSize(18f)
            .setBold()
            .setFontColor(primaryColor)
            .setBorderBottom(Border.NO_BORDER)
            .setPaddingBottom(5f))
        
        document.add(Paragraph("─".repeat(60))
            .setFontColor(goldColor))
        
        document.add(Paragraph("\n"))
    }
    
    private fun addTableRow(table: Table, label: String, value: String) {
        table.addCell(Cell().add(Paragraph(label).setBold()).setBorder(Border.NO_BORDER))
        table.addCell(Cell().add(Paragraph(value)).setBorder(Border.NO_BORDER))
    }
    
    private fun createHeaderCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text).setBold())
            .setBackgroundColor(primaryColor)
            .setFontColor(ColorConstants.WHITE)
    }
    
    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
