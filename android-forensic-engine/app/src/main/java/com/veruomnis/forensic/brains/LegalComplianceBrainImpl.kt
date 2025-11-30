package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 7: Legal & Compliance Analysis
 * "Interpretation must follow jurisdiction, not opinion."
 */
class LegalComplianceBrainImpl : LegalComplianceBrain {
    override val brainName = "Legal & Compliance Brain"

    // Common legal jurisdictions
    private val jurisdictions = mapOf(
        "UAE" to listOf("Federal Decree-Law No. 34 of 2021", "Cybercrime Law", "Evidence Law"),
        "ZA" to listOf("POPIA", "Electronic Communications Act", "Criminal Procedure Act"),
        "US" to listOf("Federal Rules of Evidence", "CFAA", "Digital Millennium Copyright Act"),
        "UK" to listOf("Data Protection Act 2018", "Computer Misuse Act", "Civil Evidence Act"),
        "EU" to listOf("GDPR", "eIDAS Regulation", "Digital Services Act")
    )

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Map jurisdictional rules
        val jurisdictionalRules = mapJurisdictionalRules(evidence)
        findings.add(Finding(
            severity = Severity.INFO,
            category = "Jurisdictional Rules",
            description = "Applicable jurisdictions identified",
            confidence = 0.85,
            details = jurisdictionalRules
        ))

        // Monitor legal thresholds
        val thresholds = monitorLegalThresholds(evidence)
        thresholds.forEach { threshold ->
            val isExceeded = threshold["is_exceeded"] as? Boolean ?: false
            findings.add(Finding(
                severity = if (isExceeded) Severity.HIGH else Severity.INFO,
                category = "Legal Threshold",
                description = threshold["description"] as? String ?: "Legal threshold analyzed",
                confidence = 0.80,
                details = threshold
            ))
        }

        // Check compliance requirements
        val compliance = checkComplianceRequirements(evidence)
        val isCompliant = compliance["overall_compliant"] as? Boolean ?: true
        findings.add(Finding(
            severity = if (isCompliant) Severity.INFO else Severity.HIGH,
            category = "Compliance Status",
            description = if (isCompliant) 
                "Evidence handling meets compliance requirements"
            else 
                "Compliance issues detected in evidence handling",
            confidence = 0.88,
            details = compliance
        ))

        // Cross-reference statutes
        val statutes = crossReferenceStatutes(evidence)
        statutes.forEach { statute ->
            findings.add(Finding(
                severity = Severity.INFO,
                category = "Statute Reference",
                description = statute["description"] as? String ?: "Relevant statute identified",
                confidence = 0.82,
                details = statute
            ))
        }

        val processingTime = System.currentTimeMillis() - startTime
        BrainAnalysisResult(
            brainName = brainName,
            confidence = if (findings.isEmpty()) 0.85 else findings.maxOfOrNull { it.confidence } ?: 0.0,
            findings = findings,
            timestamp = Date(),
            processingTimeMs = processingTime
        )
    }

    override suspend fun mapJurisdictionalRules(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val detectedJurisdictions = mutableSetOf<String>()
        val applicableStatutes = mutableListOf<String>()

        evidence.forEach { ev ->
            // Check GPS location for jurisdiction
            val latitude = ev.metadata["latitude"] as? Double
            val longitude = ev.metadata["longitude"] as? Double
            
            if (latitude != null && longitude != null) {
                val jurisdiction = inferJurisdictionFromGps(latitude, longitude)
                if (jurisdiction != null) {
                    detectedJurisdictions.add(jurisdiction)
                }
            }

            // Check timezone/locale hints
            val timezone = ev.metadata["timezone"] as? String
            if (timezone != null) {
                val jurisdiction = inferJurisdictionFromTimezone(timezone)
                if (jurisdiction != null) {
                    detectedJurisdictions.add(jurisdiction)
                }
            }

            // Check file metadata for country indicators
            val country = ev.metadata["country"] as? String
            if (country != null) {
                detectedJurisdictions.add(country)
            }
        }

        // Map statutes for detected jurisdictions
        detectedJurisdictions.forEach { jurisdiction ->
            jurisdictions[jurisdiction]?.let { statutes ->
                applicableStatutes.addAll(statutes)
            }
        }

        result["detected_jurisdictions"] = detectedJurisdictions.toList()
        result["applicable_statutes"] = applicableStatutes.distinct()
        result["jurisdiction_count"] = detectedJurisdictions.size
        result["is_cross_border"] = detectedJurisdictions.size > 1

        if (detectedJurisdictions.size > 1) {
            result["cross_border_note"] = "Evidence spans multiple jurisdictions. Consider conflict of laws analysis."
        }

        result
    }

    override suspend fun monitorLegalThresholds(evidence: List<Evidence>): List<Map<String, Any>> {
        val thresholds = mutableListOf<Map<String, Any>>()

        // Check evidence volume thresholds
        val totalSize = evidence.sumOf { it.fileSize }
        val sizeMB = totalSize / (1024 * 1024)
        thresholds.add(mapOf(
            "threshold_type" to "evidence_volume",
            "value" to sizeMB,
            "unit" to "MB",
            "limit" to 100,
            "is_exceeded" to (sizeMB > 100),
            "description" to if (sizeMB > 100) 
                "Evidence volume ($sizeMB MB) exceeds typical digital forensic submission threshold"
            else 
                "Evidence volume ($sizeMB MB) within normal parameters"
        ))

        // Check evidence count threshold
        thresholds.add(mapOf(
            "threshold_type" to "evidence_count",
            "value" to evidence.size,
            "limit" to 1000,
            "is_exceeded" to (evidence.size > 1000),
            "description" to if (evidence.size > 1000)
                "Evidence count (${evidence.size}) may require specialized handling"
            else
                "Evidence count (${evidence.size}) within manageable limits"
        ))

        // Check for time-sensitive evidence (recent files)
        val recentEvidence = evidence.filter { 
            System.currentTimeMillis() - it.dateAdded.time < 24 * 60 * 60 * 1000 
        }
        if (recentEvidence.isNotEmpty()) {
            thresholds.add(mapOf(
                "threshold_type" to "time_sensitivity",
                "value" to recentEvidence.size,
                "description" to "${recentEvidence.size} pieces of evidence added within last 24 hours - may be time-sensitive",
                "is_exceeded" to false,
                "recommendation" to "Consider expedited processing for recent evidence"
            ))
        }

        // Check for high-risk evidence types
        val sensitiveTypes = evidence.filter { 
            it.type == EvidenceType.AUDIO || it.type == EvidenceType.VIDEO 
        }
        if (sensitiveTypes.isNotEmpty()) {
            thresholds.add(mapOf(
                "threshold_type" to "sensitive_media",
                "value" to sensitiveTypes.size,
                "description" to "${sensitiveTypes.size} audio/video evidence items may contain personal data requiring special handling",
                "is_exceeded" to false,
                "legal_note" to "Audio/video evidence may be subject to privacy regulations"
            ))
        }

        thresholds
    }

    override suspend fun checkComplianceRequirements(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val complianceIssues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        // Check chain of custody
        var hasChainOfCustody = true
        evidence.forEach { ev ->
            if (!ev.metadata.containsKey("date_added") && !ev.metadata.containsKey("source")) {
                hasChainOfCustody = false
            }
        }
        result["chain_of_custody"] = hasChainOfCustody
        if (!hasChainOfCustody) {
            complianceIssues.add("Some evidence lacks proper chain of custody documentation")
            recommendations.add("Document source and acquisition date for all evidence")
        }

        // Check data protection compliance
        var hasDataProtection = true
        evidence.forEach { ev ->
            // Check if personal data is properly handled
            val hasPersonalData = ev.metadata["contains_personal_data"] as? Boolean ?: false
            val isEncrypted = ev.metadata["is_encrypted"] as? Boolean ?: false
            
            if (hasPersonalData && !isEncrypted) {
                hasDataProtection = false
            }
        }
        result["data_protection_compliant"] = hasDataProtection
        if (!hasDataProtection) {
            complianceIssues.add("Personal data may not be adequately protected")
            recommendations.add("Ensure personal data is encrypted at rest")
        }

        // Check evidence integrity
        var hasIntegrity = true
        evidence.forEach { ev ->
            if (!ev.metadata.containsKey("hash")) {
                hasIntegrity = false
            }
        }
        result["integrity_verified"] = hasIntegrity
        if (!hasIntegrity) {
            complianceIssues.add("Some evidence lacks cryptographic integrity verification")
            recommendations.add("Compute and store hash values for all evidence")
        }

        // Check access logging
        val hasAccessLog = evidence.all { ev ->
            ev.metadata.containsKey("access_log") || ev.metadata.containsKey("created_by")
        }
        result["access_logging"] = hasAccessLog
        if (!hasAccessLog) {
            recommendations.add("Implement access logging for audit trail compliance")
        }

        result["compliance_issues"] = complianceIssues
        result["recommendations"] = recommendations
        result["overall_compliant"] = complianceIssues.isEmpty()
        result["compliance_score"] = calculateComplianceScore(hasChainOfCustody, hasDataProtection, hasIntegrity, hasAccessLog)

        result
    }

    override suspend fun crossReferenceStatutes(evidence: List<Evidence>): List<Map<String, Any>> {
        val statutes = mutableListOf<Map<String, Any>>()

        // Analyze evidence types for applicable statutes
        val hasDigitalEvidence = evidence.any { it.type != EvidenceType.UNKNOWN }
        val hasAudioVideo = evidence.any { it.type == EvidenceType.AUDIO || it.type == EvidenceType.VIDEO }
        val hasDocuments = evidence.any { it.type == EvidenceType.DOCUMENT }
        val hasImages = evidence.any { it.type == EvidenceType.IMAGE }

        if (hasDigitalEvidence) {
            statutes.add(mapOf(
                "statute_category" to "Digital Evidence",
                "description" to "Digital evidence present - electronic discovery rules may apply",
                "applicable_rules" to listOf(
                    "Federal Rules of Evidence 901(b)(9)",
                    "Best Evidence Rule",
                    "Authentication requirements for digital files"
                )
            ))
        }

        if (hasAudioVideo) {
            statutes.add(mapOf(
                "statute_category" to "Audio/Video Evidence",
                "description" to "Audio/video evidence subject to authentication and privacy laws",
                "applicable_rules" to listOf(
                    "Wiretapping and surveillance laws",
                    "Privacy regulations",
                    "Chain of custody requirements for recordings"
                )
            ))
        }

        if (hasDocuments) {
            statutes.add(mapOf(
                "statute_category" to "Documentary Evidence",
                "description" to "Documents subject to hearsay rules and authentication",
                "applicable_rules" to listOf(
                    "Business records exception",
                    "Self-authentication rules",
                    "Original document rule"
                )
            ))
        }

        if (hasImages) {
            statutes.add(mapOf(
                "statute_category" to "Photographic Evidence",
                "description" to "Images require authentication and may be subject to manipulation analysis",
                "applicable_rules" to listOf(
                    "Accurate representation requirement",
                    "Foundation for admission",
                    "Digital manipulation disclosure"
                )
            ))
        }

        // Add cross-border considerations if detected
        val hasGpsData = evidence.any { 
            it.metadata.containsKey("latitude") && it.metadata.containsKey("longitude") 
        }
        if (hasGpsData) {
            statutes.add(mapOf(
                "statute_category" to "Location Data",
                "description" to "GPS data present - location privacy laws may apply",
                "applicable_rules" to listOf(
                    "Location data privacy regulations",
                    "GPS evidence authentication",
                    "Cross-border data transfer rules"
                )
            ))
        }

        statutes
    }

    private fun inferJurisdictionFromGps(latitude: Double, longitude: Double): String? {
        // Simplified GPS to jurisdiction mapping
        return when {
            latitude in 22.0..26.0 && longitude in 51.0..56.0 -> "UAE"
            latitude in -35.0..-22.0 && longitude in 16.0..33.0 -> "ZA"
            latitude in 49.0..61.0 && longitude in -8.0..2.0 -> "UK"
            latitude in 24.0..49.0 && longitude in -125.0..-66.0 -> "US"
            latitude in 35.0..72.0 && longitude in -10.0..40.0 -> "EU"
            else -> null
        }
    }

    private fun inferJurisdictionFromTimezone(timezone: String): String? {
        val tz = timezone.lowercase()
        return when {
            tz.contains("gulf") || tz.contains("dubai") -> "UAE"
            tz.contains("africa/johannesburg") || tz.contains("sast") -> "ZA"
            tz.contains("london") || tz.contains("gmt") -> "UK"
            tz.contains("america") || tz.contains("pacific") || tz.contains("eastern") -> "US"
            tz.contains("europe") || tz.contains("cet") -> "EU"
            else -> null
        }
    }

    private fun calculateComplianceScore(
        chainOfCustody: Boolean,
        dataProtection: Boolean,
        integrity: Boolean,
        accessLog: Boolean
    ): Double {
        var score = 0.0
        if (chainOfCustody) score += 0.30
        if (dataProtection) score += 0.25
        if (integrity) score += 0.30
        if (accessLog) score += 0.15
        return score
    }
}
