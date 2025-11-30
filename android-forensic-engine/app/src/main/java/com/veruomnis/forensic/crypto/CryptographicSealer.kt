package com.veruomnis.forensic.crypto

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.veruomnis.forensic.models.*
import org.apache.commons.codec.digest.DigestUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * Cryptographic Sealing System
 * 
 * Implements the Three-Gate Enforcement system:
 * - Gate 1: Input Seal Interceptor (SHA-512 on entry)
 * - Gate 2: Internal Seal Middleware (transformation tracking)
 * - Gate 3: Output Seal Enforcer (final report sealing)
 * 
 * Article X: The Verum Seal Rule
 * "NOTHING enters AND nothing leaves unless sealed"
 */
class CryptographicSealer {
    
    companion object {
        const val ALGORITHM = "SHA-512"
        const val QR_SIZE = 400
    }
    
    /**
     * Gate 1: Seal evidence on input
     * 
     * Creates an initial seal for evidence when it enters the system.
     * Uses delimiters to prevent collision attacks.
     */
    fun sealEvidence(evidence: Evidence): String {
        val delimiter = "|"
        val dataToHash = buildString {
            append(evidence.id)
            append(delimiter)
            append(evidence.fileName)
            append(delimiter)
            append(evidence.fileSize)
            append(delimiter)
            append(evidence.mimeType)
            append(delimiter)
            append(evidence.dateAdded.time)
            append(delimiter)
            evidence.metadata.entries.sortedBy { it.key }.forEach { (k, v) -> 
                append(k)
                append("=")
                append(v.toString())
                append(delimiter)
            }
        }
        return calculateSHA512(dataToHash)
    }
    
    /**
     * Gate 1: Seal a file on input
     */
    fun sealFile(file: File): String {
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
        }
        return file.inputStream().use { input ->
            DigestUtils.sha512Hex(input)
        }
    }
    
    /**
     * Gate 2: Track transformation (middleware seal)
     * 
     * Creates a seal that tracks transformations during processing.
     */
    fun sealTransformation(
        inputHash: String,
        operation: String,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val dataToHash = "$inputHash|$operation|$timestamp"
        return calculateSHA512(dataToHash)
    }
    
    /**
     * Gate 3: Generate final cryptographic seal for a report
     * 
     * This is the output seal that ensures the report integrity.
     */
    fun generateSeal(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>,
        gpsLocation: GpsLocation? = null
    ): CryptographicSeal {
        val timestamp = Date()
        
        // Build the data to hash
        val dataToHash = buildString {
            // Evidence hashes
            evidence.forEach { ev ->
                append(sealEvidence(ev))
            }
            
            // Brain results
            brainResults.forEach { result ->
                append(result.brainName)
                append(result.confidence)
                append(result.timestamp.time)
                result.findings.forEach { finding ->
                    append(finding.severity.name)
                    append(finding.description)
                }
            }
            
            // Timestamp
            append(timestamp.time)
            
            // GPS if available
            gpsLocation?.let {
                append(it.latitude)
                append(it.longitude)
                append(it.accuracy)
            }
        }
        
        val sha512Hash = calculateSHA512(dataToHash)
        
        // Generate QR code data
        val qrData = buildQrCodeData(sha512Hash, timestamp, gpsLocation)
        
        // Generate digital watermark
        val watermark = generateWatermark(sha512Hash)
        
        return CryptographicSeal(
            sha512Hash = sha512Hash,
            timestamp = timestamp,
            gpsLocation = gpsLocation,
            qrCodeData = qrData,
            digitalWatermark = watermark
        )
    }
    
    /**
     * Verify a seal against evidence and results
     */
    fun verifySeal(
        seal: CryptographicSeal,
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Boolean {
        // Regenerate the hash
        val regeneratedSeal = generateSeal(
            evidence = evidence,
            brainResults = brainResults,
            gpsLocation = seal.gpsLocation
        )
        
        // Compare hashes
        return seal.sha512Hash == regeneratedSeal.sha512Hash
    }
    
    /**
     * Calculate SHA-512 hash of a string
     */
    fun calculateSHA512(data: String): String {
        return DigestUtils.sha512Hex(data)
    }
    
    /**
     * Calculate SHA-512 hash of byte array
     */
    fun calculateSHA512(data: ByteArray): String {
        return DigestUtils.sha512Hex(data)
    }
    
    /**
     * Build QR code data string
     */
    private fun buildQrCodeData(
        hash: String,
        timestamp: Date,
        gpsLocation: GpsLocation?
    ): String {
        return buildString {
            append("VERUM-OMNIS|")
            append("V5.2.6|")
            append("SHA512:${hash.take(32)}|") // First 32 chars of hash
            append("TS:${timestamp.time}|")
            gpsLocation?.let {
                append("GPS:${it.latitude},${it.longitude}|")
            }
            append("SEALED")
        }
    }
    
    /**
     * Generate a Bitmap QR code from the seal
     */
    fun generateQrCodeBitmap(seal: CryptographicSeal, size: Int = QR_SIZE): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        
        val bitMatrix = writer.encode(
            seal.qrCodeData,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        
        return bitmap
    }
    
    /**
     * Generate digital watermark string
     */
    private fun generateWatermark(hash: String): String {
        // Create a compact watermark from the hash
        val watermarkId = UUID.nameUUIDFromBytes(hash.toByteArray()).toString()
        return "VO-SEAL-$watermarkId"
    }
    
    /**
     * Generate a verification URL for the seal
     */
    fun generateVerificationUrl(seal: CryptographicSeal): String {
        // In a production system, this would link to a verification service
        // For offline operation, we use a data URI
        return "verum://verify/${seal.sha512Hash.take(16)}"
    }
}
