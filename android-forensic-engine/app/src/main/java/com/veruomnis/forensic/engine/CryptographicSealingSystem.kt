package com.veruomnis.forensic.engine

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.codec.digest.DigestUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cryptographic Sealing System
 * 
 * Implements the Three-Gate Enforcement:
 * - Gate 1: Input Seal Interceptor
 * - Gate 2: Internal Seal Middleware
 * - Gate 3: Output Seal Enforcer
 */
class CryptographicSealingSystem(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
    private val auditLog = mutableListOf<AuditEntry>()

    /**
     * Gate 1: Input Seal Interceptor
     * Seals evidence upon entry with SHA-512 hash
     */
    suspend fun sealInput(
        file: File,
        evidenceType: EvidenceType,
        gpsLocation: GpsLocation? = null
    ): SealedEvidence = withContext(Dispatchers.IO) {
        val timestamp = Date()
        val evidenceId = generateEvidenceId()

        // Calculate SHA-512 hash of the file
        val sha512Hash = calculateSha512(file)

        // Calculate SHA-256 for compatibility
        val sha256Hash = calculateSha256(file)

        // Create entry manifest
        val entryManifest = EntryManifest(
            evidenceId = evidenceId,
            originalFileName = file.name,
            originalFilePath = file.absolutePath,
            fileSize = file.length(),
            sha512Hash = sha512Hash,
            sha256Hash = sha256Hash,
            sealTimestamp = timestamp,
            gpsLocation = gpsLocation,
            mimeType = getMimeType(file.extension)
        )

        // Log the entry
        logAuditEntry(
            action = "INPUT_SEALED",
            evidenceId = evidenceId,
            details = "Evidence sealed with SHA-512: ${sha512Hash.take(16)}..."
        )

        SealedEvidence(
            evidence = Evidence(
                id = evidenceId,
                type = evidenceType,
                filePath = file.absolutePath,
                fileName = file.name,
                mimeType = getMimeType(file.extension),
                fileSize = file.length(),
                dateAdded = timestamp,
                metadata = mapOf(
                    "hash" to sha512Hash,
                    "sha256" to sha256Hash,
                    "sealed_at" to timestamp.time,
                    "gps_latitude" to (gpsLocation?.latitude ?: 0.0),
                    "gps_longitude" to (gpsLocation?.longitude ?: 0.0)
                )
            ),
            entryManifest = entryManifest,
            isSealed = true
        )
    }

    /**
     * Gate 2: Internal Seal Middleware
     * Tracks every analytical transformation
     */
    suspend fun trackTransformation(
        evidenceId: String,
        sourceBrain: String,
        transformationType: String,
        inputData: String,
        outputData: String
    ): TransformationRecord = withContext(Dispatchers.Default) {
        val timestamp = Date()
        val recordId = "TR-${System.currentTimeMillis()}-${(1000..9999).random()}"

        // Create hash of the transformation
        val transformationHash = calculateTransformationHash(
            recordId, sourceBrain, transformationType, inputData, outputData
        )

        val record = TransformationRecord(
            recordId = recordId,
            evidenceId = evidenceId,
            sourceBrain = sourceBrain,
            transformationType = transformationType,
            timestamp = timestamp,
            inputDataHash = DigestUtils.sha256Hex(inputData),
            outputDataHash = DigestUtils.sha256Hex(outputData),
            transformationHash = transformationHash
        )

        // Log the transformation
        logAuditEntry(
            action = "TRANSFORMATION_TRACKED",
            evidenceId = evidenceId,
            details = "Brain: $sourceBrain, Type: $transformationType"
        )

        record
    }

    /**
     * Gate 3: Output Seal Enforcer
     * Seals the final report with complete cryptographic attestation
     */
    suspend fun sealOutput(
        report: ForensicReport,
        gpsLocation: GpsLocation?
    ): CryptographicSeal = withContext(Dispatchers.Default) {
        val timestamp = Date()

        // Generate comprehensive hash of the report
        val reportData = buildReportDataString(report)
        val sha512Hash = DigestUtils.sha512Hex(reportData)

        // Generate QR code data
        val qrCodeData = buildQrCodeData(report.reportId, sha512Hash, timestamp, gpsLocation)

        // Generate digital watermark
        val digitalWatermark = generateDigitalWatermark(sha512Hash, timestamp)

        // Log the output seal
        logAuditEntry(
            action = "OUTPUT_SEALED",
            evidenceId = report.reportId,
            details = "Final report sealed with SHA-512: ${sha512Hash.take(16)}..."
        )

        CryptographicSeal(
            sha512Hash = sha512Hash,
            timestamp = timestamp,
            gpsLocation = gpsLocation,
            qrCodeData = qrCodeData,
            digitalWatermark = digitalWatermark
        )
    }

    /**
     * Verify the integrity of sealed evidence
     */
    suspend fun verifyIntegrity(evidence: Evidence): IntegrityVerification = withContext(Dispatchers.IO) {
        val storedHash = evidence.metadata["hash"] as? String
        val file = File(evidence.filePath)

        if (!file.exists()) {
            return@withContext IntegrityVerification(
                isValid = false,
                reason = "Evidence file not found",
                evidenceId = evidence.id,
                verificationTimestamp = Date()
            )
        }

        if (storedHash == null) {
            return@withContext IntegrityVerification(
                isValid = false,
                reason = "No stored hash for comparison",
                evidenceId = evidence.id,
                verificationTimestamp = Date()
            )
        }

        val currentHash = calculateSha512(file)
        val isValid = currentHash == storedHash

        logAuditEntry(
            action = if (isValid) "INTEGRITY_VERIFIED" else "INTEGRITY_FAILED",
            evidenceId = evidence.id,
            details = if (isValid) "Hash match confirmed" else "Hash mismatch detected"
        )

        IntegrityVerification(
            isValid = isValid,
            reason = if (isValid) "Hash verification passed" else "Hash mismatch - evidence may have been modified",
            evidenceId = evidence.id,
            storedHash = storedHash,
            currentHash = currentHash,
            verificationTimestamp = Date()
        )
    }

    /**
     * Generate QR code bitmap from seal data
     */
    fun generateQrCodeBitmap(qrCodeData: String, size: Int = 256): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(qrCodeData, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }

        return bitmap
    }

    /**
     * Get complete audit log
     */
    fun getAuditLog(): List<AuditEntry> = auditLog.toList()

    /**
     * Export audit log as string
     */
    fun exportAuditLog(): String {
        val builder = StringBuilder()
        builder.appendLine("VERUM OMNIS FORENSIC ENGINE - AUDIT LOG")
        builder.appendLine("=" .repeat(60))
        builder.appendLine("Export Timestamp: ${dateFormat.format(Date())}")
        builder.appendLine("Total Entries: ${auditLog.size}")
        builder.appendLine("=" .repeat(60))
        builder.appendLine()

        auditLog.forEach { entry ->
            builder.appendLine("[${dateFormat.format(entry.timestamp)}]")
            builder.appendLine("  Action: ${entry.action}")
            builder.appendLine("  Evidence ID: ${entry.evidenceId}")
            builder.appendLine("  Details: ${entry.details}")
            builder.appendLine()
        }

        return builder.toString()
    }

    // Private helper functions

    private fun calculateSha512(file: File): String {
        return file.inputStream().use { input ->
            DigestUtils.sha512Hex(input)
        }
    }

    private fun calculateSha256(file: File): String {
        return file.inputStream().use { input ->
            DigestUtils.sha256Hex(input)
        }
    }

    private fun calculateTransformationHash(
        recordId: String,
        sourceBrain: String,
        transformationType: String,
        inputData: String,
        outputData: String
    ): String {
        val combined = "$recordId|$sourceBrain|$transformationType|${DigestUtils.sha256Hex(inputData)}|${DigestUtils.sha256Hex(outputData)}"
        return DigestUtils.sha256Hex(combined)
    }

    private fun buildReportDataString(report: ForensicReport): String {
        val builder = StringBuilder()
        builder.append("REPORT_ID:${report.reportId}")
        builder.append("|EVIDENCE_COUNT:${report.evidence.size}")
        builder.append("|BRAIN_COUNT:${report.brainResults.size}")
        builder.append("|TSI:${report.synthesisResult.truthStabilityIndex}")
        builder.append("|VERDICT:${report.synthesisResult.overallVerdict}")
        builder.append("|GENERATED:${report.generatedAt.time}")

        report.evidence.forEach { ev ->
            builder.append("|EV:${ev.id}:${ev.metadata["hash"] ?: "NO_HASH"}")
        }

        report.brainResults.forEach { brain ->
            builder.append("|BRAIN:${brain.brainName}:${brain.confidence}")
        }

        return builder.toString()
    }

    private fun buildQrCodeData(
        reportId: String,
        sha512Hash: String,
        timestamp: Date,
        gpsLocation: GpsLocation?
    ): String {
        val builder = StringBuilder()
        builder.append("VO|") // Verum Omnis identifier
        builder.append("${reportId}|")
        builder.append("${sha512Hash.take(32)}|") // Truncate hash for QR code size
        builder.append("${timestamp.time}|")
        if (gpsLocation != null) {
            builder.append("${gpsLocation.latitude},${gpsLocation.longitude}")
        } else {
            builder.append("NO_GPS")
        }
        return builder.toString()
    }

    private fun generateDigitalWatermark(sha512Hash: String, timestamp: Date): String {
        val watermarkData = "VO-SEAL-${sha512Hash.take(16)}-${timestamp.time}"
        return DigestUtils.sha256Hex(watermarkData)
    }

    private fun generateEvidenceId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (10000..99999).random()
        return "EV-$timestamp-$random"
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "txt" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            else -> "application/octet-stream"
        }
    }

    private fun logAuditEntry(action: String, evidenceId: String, details: String) {
        auditLog.add(AuditEntry(
            timestamp = Date(),
            action = action,
            evidenceId = evidenceId,
            details = details
        ))
    }
}

// Data classes for sealing system

data class SealedEvidence(
    val evidence: Evidence,
    val entryManifest: EntryManifest,
    val isSealed: Boolean
)

data class EntryManifest(
    val evidenceId: String,
    val originalFileName: String,
    val originalFilePath: String,
    val fileSize: Long,
    val sha512Hash: String,
    val sha256Hash: String,
    val sealTimestamp: Date,
    val gpsLocation: GpsLocation?,
    val mimeType: String
)

data class TransformationRecord(
    val recordId: String,
    val evidenceId: String,
    val sourceBrain: String,
    val transformationType: String,
    val timestamp: Date,
    val inputDataHash: String,
    val outputDataHash: String,
    val transformationHash: String
)

data class IntegrityVerification(
    val isValid: Boolean,
    val reason: String,
    val evidenceId: String,
    val storedHash: String? = null,
    val currentHash: String? = null,
    val verificationTimestamp: Date
)

data class AuditEntry(
    val timestamp: Date,
    val action: String,
    val evidenceId: String,
    val details: String
)
