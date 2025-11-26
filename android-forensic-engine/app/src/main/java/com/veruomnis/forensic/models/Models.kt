package com.veruomnis.forensic.models

import java.util.Date

/**
 * Represents evidence data to be analyzed by the forensic engine
 */
data class Evidence(
    val id: String,
    val type: EvidenceType,
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val dateAdded: Date,
    val metadata: Map<String, Any> = emptyMap()
)

enum class EvidenceType {
    DOCUMENT,
    IMAGE,
    AUDIO,
    VIDEO,
    UNKNOWN
}

/**
 * Analysis result from a single brain
 */
data class BrainAnalysisResult(
    val brainName: String,
    val confidence: Double, // 0.0 to 1.0
    val findings: List<Finding>,
    val timestamp: Date,
    val processingTimeMs: Long
)

/**
 * Individual finding from analysis
 */
data class Finding(
    val severity: Severity,
    val category: String,
    val description: String,
    val confidence: Double,
    val details: Map<String, Any> = emptyMap()
)

enum class Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

/**
 * Comprehensive forensic report
 */
data class ForensicReport(
    val reportId: String,
    val evidence: List<Evidence>,
    val brainResults: List<BrainAnalysisResult>,
    val synthesisResult: SynthesisResult,
    val cryptographicSeal: CryptographicSeal,
    val generatedAt: Date,
    val constitutionalCompliance: ConstitutionalCompliance
)

/**
 * Final synthesis from Brain 9
 */
data class SynthesisResult(
    val truthStabilityIndex: Double, // 0.0 to 100.0
    val overallVerdict: String,
    val consensusLevel: Double, // 0.0 to 1.0
    val keyFindings: List<String>,
    val contradictions: List<Contradiction>,
    val recommendations: List<String>
)

/**
 * Detected contradiction
 */
data class Contradiction(
    val type: String,
    val description: String,
    val evidenceIds: List<String>,
    val severity: Severity,
    val resolutionSuggestion: String
)

/**
 * Cryptographic seal for report integrity
 */
data class CryptographicSeal(
    val sha512Hash: String,
    val timestamp: Date,
    val gpsLocation: GpsLocation?,
    val qrCodeData: String,
    val digitalWatermark: String
)

/**
 * GPS location data
 */
data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Date
)

/**
 * Constitutional compliance verification
 */
data class ConstitutionalCompliance(
    val zeroLossEvidenceDoctrine: Boolean,
    val tripleAiConsensus: Boolean,
    val guardianshipModelEnforced: Boolean,
    val complianceNotes: List<String>
)
