package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 7: Legal & Compliance Analysis
 * Maps jurisdictional rules, monitors legal thresholds, and checks compliance
 */
class LegalComplianceBrainImpl : LegalComplianceBrain {
    override val brainName = "Legal & Compliance Brain"

    // Common legal thresholds by category
    private val legalThresholds = mapOf(
        "fraud" to mapOf(
            "minor" to 1000.0,
            "major" to 10000.0,
            "grand" to 100000.0
        ),
        "statute_of_limitations_years" to mapOf(
            "civil_fraud" to 6,
            "criminal_fraud" to 7,
            "contract" to 6,
            "personal_injury" to 3
        )
    )

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Map jurisdictional rules
        val jurisdictionalRules = mapJurisdictionalRules(evidence)
        findings.add(Finding(
            severity = Severity.INFO,
            category = "Jurisdictional Analysis",
            description = jurisdictionalRules["description"] as? String ?: "Jurisdiction mapped",
            confidence = 0.85,
            details = jurisdictionalRules
        ))

        // Monitor legal thresholds
        val thresholds = monitorLegalThresholds(evidence)
        thresholds.forEach { threshold ->
            val exceeded = threshold["exceeded"] as? Boolean ?: false
            findings.add(Finding(
                severity = if (exceeded) Severity.HIGH else Severity.INFO,
                category = "Legal Threshold",
                description = threshold["description"] as? String ?: "Threshold analysis",
                confidence = threshold["confidence"] as? Double ?: 0.80,
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
            confidence = compliance["confidence"] as? Double ?: 0.85,
            details = compliance
        ))

        // Cross-reference statutes
        val statutes = crossReferenceStatutes(evidence)
        statutes.forEach { statute ->
            findings.add(Finding(
                severity = Severity.INFO,
                category = "Statute Reference",
                description = statute["description"] as? String ?: "Relevant statute identified",
                confidence = statute["confidence"] as? Double ?: 0.75,
                details = statute
            ))
        }

        val processingTime = System.currentTimeMillis() - startTime
        BrainAnalysisResult(
            brainName = brainName,
            confidence = if (findings.isEmpty()) 0.80 else findings.maxOfOrNull { it.confidence } ?: 0.0,
            findings = findings,
            timestamp = Date(),
            processingTimeMs = processingTime
        )
    }

    override suspend fun mapJurisdictionalRules(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        // Analyze location data from evidence to determine jurisdiction
        val locations = evidence.mapNotNull { ev ->
            val lat = ev.metadata["latitude"] as? Double
            val lon = ev.metadata["longitude"] as? Double
            if (lat != null && lon != null) Pair(lat, lon) else null
        }
        
        val jurisdictions = mutableSetOf<String>()
        
        // Map coordinates to approximate jurisdictions (simplified)
        locations.forEach { (lat, lon) ->
            val jurisdiction = determineJurisdiction(lat, lon)
            if (jurisdiction != null) {
                jurisdictions.add(jurisdiction)
            }
        }
        
        // Also check for explicit jurisdiction in metadata
        evidence.forEach { ev ->
            val country = ev.metadata["country"] as? String
            val state = ev.metadata["state"] as? String
            if (country != null) jurisdictions.add(country)
            if (state != null) jurisdictions.add(state)
        }
        
        result["jurisdictions"] = jurisdictions.toList()
        result["multi_jurisdictional"] = jurisdictions.size > 1
        result["description"] = when {
            jurisdictions.isEmpty() -> "No jurisdiction identified from evidence"
            jurisdictions.size == 1 -> "Single jurisdiction: ${jurisdictions.first()}"
            else -> "Multi-jurisdictional case spanning: ${jurisdictions.joinToString(", ")}"
        }
        result["confidence"] = 0.85
        
        if (jurisdictions.size > 1) {
            result["cross_border_considerations"] = listOf(
                "Evidence may need to be admissible in multiple jurisdictions",
                "Consider conflict of laws rules",
                "May require international cooperation protocols"
            )
        }
        
        result
    }

    override suspend fun monitorLegalThresholds(evidence: List<Evidence>): List<Map<String, Any>> {
        val thresholds = mutableListOf<Map<String, Any>>()
        
        // Check evidence quantity thresholds
        val totalEvidence = evidence.size
        thresholds.add(mapOf(
            "threshold_type" to "evidence_quantity",
            "value" to totalEvidence,
            "exceeded" to (totalEvidence >= 3),
            "description" to "Evidence count: $totalEvidence (minimum 3 recommended for strong case)",
            "confidence" to 0.90
        ))
        
        // Check for financial amounts in metadata
        val financialAmounts = evidence.mapNotNull { 
            it.metadata["amount"] as? Double 
        }
        val totalAmount = financialAmounts.sum()
        
        if (totalAmount > 0) {
            val fraudLevel = when {
                totalAmount >= 100000 -> "grand_fraud"
                totalAmount >= 10000 -> "major_fraud"
                totalAmount >= 1000 -> "minor_fraud"
                else -> "petty"
            }
            
            thresholds.add(mapOf(
                "threshold_type" to "financial_amount",
                "value" to totalAmount,
                "fraud_level" to fraudLevel,
                "exceeded" to (totalAmount >= 1000),
                "description" to "Total financial amount: $${String.format("%.2f", totalAmount)} (Level: $fraudLevel)",
                "confidence" to 0.85
            ))
        }
        
        // Check evidence age for statute of limitations
        val oldestEvidence = evidence.minByOrNull { it.dateAdded }
        if (oldestEvidence != null) {
            val ageYears = (Date().time - oldestEvidence.dateAdded.time) / (1000L * 60 * 60 * 24 * 365)
            val withinStatute = ageYears <= 6
            
            thresholds.add(mapOf(
                "threshold_type" to "statute_of_limitations",
                "age_years" to ageYears,
                "exceeded" to !withinStatute,
                "description" to "Oldest evidence is $ageYears years old (6-year civil statute typical)",
                "confidence" to 0.80
            ))
        }
        
        return thresholds
    }

    override suspend fun checkComplianceRequirements(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val complianceIssues = mutableListOf<String>()
        
        // Check data handling compliance
        val hasPersonalData = evidence.any { ev ->
            ev.metadata.keys.any { key ->
                key in listOf("name", "email", "phone", "ssn", "id_number", "address")
            }
        }
        
        if (hasPersonalData) {
            complianceIssues.add("Evidence contains personal data - ensure GDPR/privacy compliance")
        }
        
        // Check evidence integrity requirements
        val hasHashVerification = evidence.all { it.metadata.containsKey("hash") }
        if (!hasHashVerification) {
            complianceIssues.add("Not all evidence has cryptographic hash verification")
        }
        
        // Check chain of custody documentation
        val hasChainOfCustody = evidence.all { 
            it.metadata.containsKey("collected_by") || it.metadata.containsKey("source")
        }
        if (!hasChainOfCustody) {
            complianceIssues.add("Chain of custody documentation incomplete")
        }
        
        // Check timestamp integrity
        val hasTimestamps = evidence.all { it.dateAdded != null }
        if (!hasTimestamps) {
            complianceIssues.add("Missing timestamp information on some evidence")
        }
        
        result["compliant"] = complianceIssues.isEmpty()
        result["issues"] = complianceIssues
        result["issues_count"] = complianceIssues.size
        result["description"] = if (complianceIssues.isEmpty()) 
            "All compliance requirements met" 
        else 
            "Compliance issues found: ${complianceIssues.size}"
        result["confidence"] = 0.85
        
        result
    }

    override suspend fun crossReferenceStatutes(evidence: List<Evidence>): List<Map<String, Any>> {
        val statutes = mutableListOf<Map<String, Any>>()
        
        // Analyze evidence types to suggest relevant statutes
        val evidenceTypes = evidence.groupBy { it.type }
        
        // Document-based evidence
        if (evidenceTypes.containsKey(EvidenceType.DOCUMENT)) {
            statutes.add(mapOf(
                "statute_category" to "documentary_evidence",
                "description" to "Documentary evidence rules apply (authentication required)",
                "relevant_rules" to listOf(
                    "Best Evidence Rule",
                    "Authentication requirements",
                    "Hearsay exceptions"
                ),
                "confidence" to 0.80
            ))
        }
        
        // Digital evidence
        if (evidence.any { it.mimeType.startsWith("image/") || it.mimeType.startsWith("audio/") || it.mimeType.startsWith("video/") }) {
            statutes.add(mapOf(
                "statute_category" to "digital_evidence",
                "description" to "Digital evidence standards apply",
                "relevant_rules" to listOf(
                    "Digital evidence authentication",
                    "Metadata preservation",
                    "Chain of custody for electronic records"
                ),
                "confidence" to 0.85
            ))
        }
        
        // Financial evidence
        val hasFinancialData = evidence.any { ev ->
            ev.metadata.keys.any { it in listOf("amount", "transaction", "payment", "invoice") }
        }
        if (hasFinancialData) {
            statutes.add(mapOf(
                "statute_category" to "financial_fraud",
                "description" to "Financial fraud statutes may apply",
                "relevant_rules" to listOf(
                    "Fraud Act provisions",
                    "Financial Services regulations",
                    "Money laundering considerations"
                ),
                "confidence" to 0.75
            ))
        }
        
        return statutes
    }
    
    /**
     * Simplified jurisdiction determination from GPS coordinates
     */
    private fun determineJurisdiction(lat: Double, lon: Double): String? {
        return when {
            // South Africa
            lat in -35.0..-22.0 && lon in 16.0..33.0 -> "South Africa"
            // UAE
            lat in 22.0..26.5 && lon in 51.0..56.5 -> "United Arab Emirates"
            // UK
            lat in 49.0..61.0 && lon in -8.0..2.0 -> "United Kingdom"
            // USA (continental)
            lat in 25.0..49.0 && lon in -125.0..-67.0 -> "United States"
            // Australia
            lat in -44.0..-10.0 && lon in 113.0..154.0 -> "Australia"
            else -> null
        }
    }
}
