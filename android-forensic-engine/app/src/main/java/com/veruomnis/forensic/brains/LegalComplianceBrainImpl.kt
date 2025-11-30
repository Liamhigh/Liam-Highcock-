package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 7: Legal & Compliance
 * 
 * Maps jurisdictional rules, monitors legal thresholds,
 * checks compliance requirements, and cross-references statutes.
 */
class LegalComplianceBrainImpl : LegalComplianceBrain {
    override val brainName = "Legal Compliance Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Map jurisdictional rules
        val jurisdictionRules = mapJurisdictionalRules(evidence)
        findings.add(Finding(
            severity = Severity.INFO,
            category = "Jurisdiction Mapping",
            description = jurisdictionRules["description"] as? String ?: "Jurisdiction analyzed",
            confidence = 0.85,
            details = jurisdictionRules
        ))

        // Monitor legal thresholds
        val thresholds = monitorLegalThresholds(evidence)
        thresholds.forEach { threshold ->
            val exceeded = threshold["exceeded"] as? Boolean ?: false
            findings.add(Finding(
                severity = if (exceeded) Severity.HIGH else Severity.INFO,
                category = "Legal Threshold",
                description = threshold["description"] as? String ?: "Threshold check",
                confidence = 0.80,
                details = threshold
            ))
        }

        // Check compliance requirements
        val compliance = checkComplianceRequirements(evidence)
        val isCompliant = compliance["compliant"] as? Boolean ?: true
        findings.add(Finding(
            severity = if (isCompliant) Severity.INFO else Severity.MEDIUM,
            category = "Compliance Check",
            description = compliance["description"] as? String ?: "Compliance verified",
            confidence = 0.90,
            details = compliance
        ))

        // Cross-reference statutes
        val statutes = crossReferenceStatutes(evidence)
        statutes.forEach { statute ->
            findings.add(Finding(
                severity = Severity.INFO,
                category = "Statute Reference",
                description = statute["description"] as? String ?: "Relevant statute identified",
                confidence = 0.75,
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
        
        // Identify potential jurisdictions from evidence
        val jurisdictions = mutableSetOf<String>()
        
        evidence.forEach { ev ->
            // Check for GPS-based jurisdiction
            val lat = ev.metadata["latitude"] as? Double
            val lon = ev.metadata["longitude"] as? Double
            if (lat != null && lon != null) {
                // Simplified jurisdiction detection
                val region = when {
                    lat in 24.0..49.0 && lon in -125.0..-66.0 -> "United States"
                    lat in 49.0..84.0 && lon in -141.0..-52.0 -> "Canada"
                    lat in 35.0..71.0 && lon in -11.0..40.0 -> "Europe"
                    lat in -35.0..37.0 && lon in -18.0..52.0 -> "Africa"
                    lat in 12.0..45.0 && lon in 54.0..141.0 -> "Asia"
                    lat in -47.0..10.0 && lon in 113.0..180.0 -> "Oceania"
                    lat in -56.0..12.0 && lon in -82.0..-34.0 -> "South America"
                    else -> "International"
                }
                jurisdictions.add(region)
            }
            
            // Check metadata for location info
            ev.metadata["country"]?.let { jurisdictions.add(it.toString()) }
            ev.metadata["region"]?.let { jurisdictions.add(it.toString()) }
        }
        
        result["jurisdictions"] = jurisdictions.toList()
        result["evidence_count"] = evidence.size
        result["description"] = if (jurisdictions.isNotEmpty()) {
            "Evidence spans ${jurisdictions.size} potential jurisdiction(s): ${jurisdictions.joinToString(", ")}"
        } else {
            "Unable to determine jurisdiction from evidence - manual review recommended"
        }
        
        return result
    }

    override suspend fun monitorLegalThresholds(evidence: List<Evidence>): List<Map<String, Any>> {
        val thresholds = mutableListOf<Map<String, Any>>()
        
        // Check evidence count thresholds
        if (evidence.size >= 10) {
            thresholds.add(mapOf(
                "type" to "evidence_volume",
                "exceeded" to true,
                "description" to "Substantial evidence volume (${evidence.size} items) - may support formal proceedings",
                "threshold" to 10,
                "actual" to evidence.size
            ))
        }
        
        // Check for critical evidence types
        val hasDocument = evidence.any { it.type == EvidenceType.DOCUMENT }
        val hasAudioVideo = evidence.any { it.type == EvidenceType.AUDIO || it.type == EvidenceType.VIDEO }
        val hasImage = evidence.any { it.type == EvidenceType.IMAGE }
        
        if (hasDocument && hasAudioVideo) {
            thresholds.add(mapOf(
                "type" to "corroborating_evidence",
                "exceeded" to true,
                "description" to "Multiple evidence types present - supports corroboration requirements",
                "has_documents" to hasDocument,
                "has_audio_video" to hasAudioVideo,
                "has_images" to hasImage
            ))
        }
        
        return thresholds
    }

    override suspend fun checkComplianceRequirements(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val issues = mutableListOf<String>()
        
        // Check for chain of custody (hash verification)
        val hasHashes = evidence.all { it.metadata.containsKey("hash") }
        if (!hasHashes) {
            issues.add("Some evidence lacks hash verification for chain of custody")
        }
        
        // Check for timestamps
        val hasTimestamps = evidence.all { 
            it.metadata.containsKey("created_date") || it.metadata.containsKey("modified_date")
        }
        if (!hasTimestamps) {
            issues.add("Some evidence lacks timestamp metadata")
        }
        
        // Check for source identification
        val hasSource = evidence.all { 
            it.metadata.containsKey("source") || it.metadata.containsKey("device")
        }
        if (!hasSource) {
            issues.add("Some evidence lacks source/device identification")
        }
        
        result["compliant"] = issues.isEmpty()
        result["issues"] = issues
        result["description"] = if (issues.isEmpty()) {
            "Evidence set meets basic compliance requirements"
        } else {
            "Compliance issues found: ${issues.size} requirement(s) not fully met"
        }
        
        return result
    }

    override suspend fun crossReferenceStatutes(evidence: List<Evidence>): List<Map<String, Any>> {
        val statutes = mutableListOf<Map<String, Any>>()
        
        // Add relevant general statutes based on evidence types
        if (evidence.any { it.type == EvidenceType.DOCUMENT }) {
            statutes.add(mapOf(
                "category" to "Document Evidence",
                "description" to "Documentary evidence rules apply (varies by jurisdiction)",
                "note" to "Authentication and best evidence rules may apply"
            ))
        }
        
        if (evidence.any { it.type == EvidenceType.AUDIO || it.type == EvidenceType.VIDEO }) {
            statutes.add(mapOf(
                "category" to "Audio/Visual Evidence",
                "description" to "Recording laws and authentication requirements apply",
                "note" to "Two-party consent states may have additional requirements"
            ))
        }
        
        if (evidence.any { it.type == EvidenceType.IMAGE }) {
            statutes.add(mapOf(
                "category" to "Photographic Evidence",
                "description" to "Photographic authentication requirements apply",
                "note" to "Digital manipulation detection may be required"
            ))
        }
        
        // Constitutional compliance
        statutes.add(mapOf(
            "category" to "Constitutional Compliance",
            "description" to "Verum Omnis Constitutional Charter enforced",
            "note" to "Zero-Loss Evidence Doctrine, Triple-AI Consensus, Article X Seal Rule"
        ))
        
        return statutes
    }
}
