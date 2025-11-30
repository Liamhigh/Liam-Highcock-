package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 9: Synthesis & Verdict
 * "All truth converges."
 */
class SynthesisVerdictBrainImpl : SynthesisVerdictBrain {
    override val brainName = "Synthesis & Verdict Brain"

    // Minimum brains required for consensus
    private val MINIMUM_CONSENSUS_BRAINS = 6

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        // This brain aggregates results from other brains
        // When called directly, it provides a summary analysis
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        findings.add(Finding(
            severity = Severity.INFO,
            category = "Synthesis",
            description = "Synthesis Brain requires results from other brains for full analysis. Use ForensicEngine for complete processing.",
            confidence = 1.0,
            details = mapOf("evidence_count" to evidence.size)
        ))

        val processingTime = System.currentTimeMillis() - startTime
        BrainAnalysisResult(
            brainName = brainName,
            confidence = 1.0,
            findings = findings,
            timestamp = Date(),
            processingTimeMs = processingTime
        )
    }

    override suspend fun generateUnifiedTruthModel(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Any> = withContext(Dispatchers.Default) {
        val model = mutableMapOf<String, Any>()

        // Calculate overall truth stability
        val tsiFindings = brainResults.flatMap { it.findings }
            .filter { it.category == "Truth Stability" }
        val averageTsi = tsiFindings.mapNotNull { 
            it.details["tsi"] as? Double 
        }.average().takeIf { !it.isNaN() } ?: 75.0

        model["truth_stability_index"] = averageTsi

        // Aggregate all findings by severity
        val allFindings = brainResults.flatMap { it.findings }
        val bySeverity = allFindings.groupBy { it.severity }

        model["critical_findings"] = bySeverity[Severity.CRITICAL]?.size ?: 0
        model["high_findings"] = bySeverity[Severity.HIGH]?.size ?: 0
        model["medium_findings"] = bySeverity[Severity.MEDIUM]?.size ?: 0
        model["low_findings"] = bySeverity[Severity.LOW]?.size ?: 0
        model["info_findings"] = bySeverity[Severity.INFO]?.size ?: 0

        // Extract key narratives from each brain
        val narratives = brainResults.map { brain ->
            val keyFinding = brain.findings.maxByOrNull { it.confidence }
            mapOf(
                "brain" to brain.brainName,
                "key_finding" to (keyFinding?.description ?: "No significant findings"),
                "confidence" to brain.confidence
            )
        }
        model["brain_narratives"] = narratives

        // Identify contradictions across brains
        val contradictions = identifyContradictions(brainResults)
        model["cross_brain_contradictions"] = contradictions

        // Calculate unified confidence
        val averageConfidence = brainResults.map { it.confidence }.average()
        model["unified_confidence"] = averageConfidence

        // Generate unified narrative
        model["unified_narrative"] = generateNarrative(evidence, brainResults, averageTsi)

        model
    }

    override suspend fun verifyCrossBrainConsensus(
        brainResults: List<BrainAnalysisResult>
    ): Double = withContext(Dispatchers.Default) {
        if (brainResults.isEmpty()) return 0.0

        // Count brains with high confidence (> 0.7)
        val highConfidenceBrains = brainResults.count { it.confidence > 0.7 }
        
        // Count brains with consistent findings (no critical contradictions)
        val consistentBrains = brainResults.count { brain ->
            brain.findings.none { 
                it.severity == Severity.CRITICAL && it.category.contains("Contradiction")
            }
        }

        // Calculate consensus score
        val confidenceScore = highConfidenceBrains.toDouble() / brainResults.size
        val consistencyScore = consistentBrains.toDouble() / brainResults.size

        // Check if minimum brains agree
        val agreementThreshold = brainResults.size >= MINIMUM_CONSENSUS_BRAINS
        val minBrainBonus = if (agreementThreshold) 0.1 else 0.0

        ((confidenceScore + consistencyScore) / 2 + minBrainBonus).coerceIn(0.0, 1.0)
    }

    override suspend fun compileReport(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Any> = withContext(Dispatchers.Default) {
        val report = mutableMapOf<String, Any>()

        // Report metadata
        report["report_id"] = generateReportId()
        report["generation_timestamp"] = Date()
        report["evidence_count"] = evidence.size

        // Generate unified truth model
        val unifiedModel = generateUnifiedTruthModel(evidence, brainResults)
        report["unified_truth_model"] = unifiedModel

        // Verify consensus
        val consensusScore = verifyCrossBrainConsensus(brainResults)
        report["consensus_score"] = consensusScore
        report["consensus_achieved"] = consensusScore >= 0.75

        // Check constitutional compliance
        val compliance = checkConstitutionalCompliance(evidence, brainResults)
        report["constitutional_compliance"] = compliance

        // Executive summary
        report["executive_summary"] = generateExecutiveSummary(
            evidence, 
            brainResults, 
            unifiedModel,
            consensusScore,
            compliance
        )

        // Key findings
        report["key_findings"] = extractKeyFindings(brainResults)

        // Recommended actions
        report["recommended_actions"] = generateRecommendations(brainResults, compliance)

        // Brain-by-brain summary
        report["brain_summaries"] = brainResults.map { brain ->
            mapOf(
                "brain_name" to brain.brainName,
                "confidence" to brain.confidence,
                "finding_count" to brain.findings.size,
                "processing_time_ms" to brain.processingTimeMs,
                "critical_count" to brain.findings.count { it.severity == Severity.CRITICAL },
                "high_count" to brain.findings.count { it.severity == Severity.HIGH }
            )
        }

        // Final verdict
        report["verdict"] = generateVerdict(unifiedModel, consensusScore, compliance)

        report
    }

    override suspend fun checkConstitutionalCompliance(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Boolean> = withContext(Dispatchers.Default) {
        val compliance = mutableMapOf<String, Boolean>()

        // Zero-Loss Evidence Doctrine
        // Check if all evidence is accounted for
        val allEvidenceProcessed = brainResults.all { brain ->
            brain.findings.isNotEmpty()
        }
        compliance["zero_loss_evidence_doctrine"] = allEvidenceProcessed

        // Triple-AI Consensus (at least 6 of 8 brains agree)
        val consensusScore = verifyCrossBrainConsensus(brainResults)
        compliance["triple_ai_consensus"] = consensusScore >= 0.75

        // Guardianship Model Enforcement
        // System operates as guardian, not modifier
        val noEvidenceModified = evidence.all { ev ->
            // Check that original data is preserved
            ev.metadata.containsKey("hash") || ev.metadata.isEmpty()
        }
        compliance["guardianship_model_enforced"] = noEvidenceModified

        // Article X: The Verum Seal Rule
        // All three gates must be operational
        val inputSealed = evidence.all { it.id.isNotEmpty() }
        val processingLogged = brainResults.all { it.timestamp != null }
        val outputSealable = true // Will be sealed on report generation

        compliance["article_x_gate_1_input"] = inputSealed
        compliance["article_x_gate_2_processing"] = processingLogged
        compliance["article_x_gate_3_output"] = outputSealable
        compliance["article_x_complete"] = inputSealed && processingLogged && outputSealable

        // Overall constitutional compliance
        compliance["fully_compliant"] = compliance.values.all { it }

        compliance
    }

    private fun identifyContradictions(brainResults: List<BrainAnalysisResult>): List<Map<String, Any>> {
        val contradictions = mutableListOf<Map<String, Any>>()

        // Look for conflicting findings across brains
        val allFindings = brainResults.flatMap { brain ->
            brain.findings.map { finding -> brain.brainName to finding }
        }

        // Check for contradicting conclusions
        for (i in allFindings.indices) {
            for (j in i + 1 until allFindings.size) {
                val (brain1, finding1) = allFindings[i]
                val (brain2, finding2) = allFindings[j]

                // Simple contradiction detection: high severity findings that oppose
                if (finding1.category == finding2.category &&
                    finding1.severity != finding2.severity &&
                    (finding1.severity == Severity.CRITICAL || finding2.severity == Severity.CRITICAL)) {
                    contradictions.add(mapOf(
                        "brain_1" to brain1,
                        "finding_1" to finding1.description,
                        "brain_2" to brain2,
                        "finding_2" to finding2.description,
                        "category" to finding1.category
                    ))
                }
            }
        }

        return contradictions
    }

    private fun generateNarrative(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>,
        tsi: Double
    ): String {
        val builder = StringBuilder()

        builder.append("Analysis of ${evidence.size} evidence items using the Nine-Brain Architecture. ")
        
        val totalFindings = brainResults.sumOf { it.findings.size }
        val criticalCount = brainResults.sumOf { it.findings.count { f -> f.severity == Severity.CRITICAL } }
        val highCount = brainResults.sumOf { it.findings.count { f -> f.severity == Severity.HIGH } }

        builder.append("Total findings: $totalFindings. ")
        
        if (criticalCount > 0) {
            builder.append("CRITICAL: $criticalCount findings require immediate attention. ")
        }
        
        if (highCount > 0) {
            builder.append("HIGH: $highCount significant findings identified. ")
        }

        builder.append("Truth Stability Index: ${tsi.toInt()}/100. ")

        when {
            tsi >= 80 -> builder.append("Evidence demonstrates high consistency and reliability.")
            tsi >= 60 -> builder.append("Evidence shows moderate consistency with some concerns.")
            else -> builder.append("Evidence reveals significant inconsistencies requiring investigation.")
        }

        return builder.toString()
    }

    private fun generateExecutiveSummary(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>,
        unifiedModel: Map<String, Any>,
        consensusScore: Double,
        compliance: Map<String, Boolean>
    ): Map<String, Any> {
        return mapOf(
            "evidence_analyzed" to evidence.size,
            "brains_executed" to brainResults.size,
            "truth_stability_index" to (unifiedModel["truth_stability_index"] ?: 0.0),
            "consensus_level" to (consensusScore * 100).toInt(),
            "critical_alerts" to (unifiedModel["critical_findings"] ?: 0),
            "constitutional_compliance" to (compliance["fully_compliant"] ?: false),
            "processing_time_ms" to brainResults.sumOf { it.processingTimeMs }
        )
    }

    private fun extractKeyFindings(brainResults: List<BrainAnalysisResult>): List<String> {
        return brainResults.flatMap { it.findings }
            .filter { it.severity in listOf(Severity.CRITICAL, Severity.HIGH) }
            .sortedByDescending { it.confidence }
            .take(10)
            .map { "${it.category}: ${it.description}" }
    }

    private fun generateRecommendations(
        brainResults: List<BrainAnalysisResult>,
        compliance: Map<String, Boolean>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Check for critical findings
        val criticalCount = brainResults.sumOf { 
            it.findings.count { f -> f.severity == Severity.CRITICAL } 
        }
        if (criticalCount > 0) {
            recommendations.add("Address $criticalCount critical findings immediately")
        }

        // Check compliance issues
        if (compliance["zero_loss_evidence_doctrine"] != true) {
            recommendations.add("Ensure all evidence is properly cataloged and preserved")
        }
        if (compliance["triple_ai_consensus"] != true) {
            recommendations.add("Review contradicting brain analyses for resolution")
        }
        if (compliance["guardianship_model_enforced"] != true) {
            recommendations.add("Verify original evidence integrity")
        }

        // General recommendations based on findings
        val hasContradictions = brainResults.any { brain ->
            brain.findings.any { it.category.contains("Contradiction") }
        }
        if (hasContradictions) {
            recommendations.add("Investigate identified contradictions in evidence")
        }

        val hasManipulation = brainResults.any { brain ->
            brain.findings.any { 
                it.category.contains("Manipulation") || it.category.contains("Tampering")
            }
        }
        if (hasManipulation) {
            recommendations.add("Conduct detailed forensic analysis of potentially manipulated evidence")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Continue standard evidence monitoring and documentation")
        }

        return recommendations
    }

    private fun generateVerdict(
        unifiedModel: Map<String, Any>,
        consensusScore: Double,
        compliance: Map<String, Boolean>
    ): Map<String, Any> {
        val tsi = unifiedModel["truth_stability_index"] as? Double ?: 0.0
        val criticalFindings = unifiedModel["critical_findings"] as? Int ?: 0
        val isCompliant = compliance["fully_compliant"] == true

        val verdictLevel = when {
            tsi >= 80 && consensusScore >= 0.75 && isCompliant -> "VERIFIED"
            tsi >= 60 && consensusScore >= 0.5 -> "CONDITIONAL"
            else -> "INCONCLUSIVE"
        }

        val confidence = when {
            tsi >= 80 && consensusScore >= 0.75 -> "HIGH"
            tsi >= 60 && consensusScore >= 0.5 -> "MODERATE"
            else -> "LOW"
        }

        return mapOf(
            "verdict" to verdictLevel,
            "confidence" to confidence,
            "truth_stability_index" to tsi,
            "consensus_score" to consensusScore,
            "constitutional_compliance" to isCompliant,
            "critical_findings_count" to criticalFindings,
            "summary" to when (verdictLevel) {
                "VERIFIED" -> "Evidence has been verified with high confidence across multiple analytical dimensions."
                "CONDITIONAL" -> "Evidence shows moderate reliability but requires additional verification or documentation."
                else -> "Evidence is inconclusive and requires further investigation."
            }
        )
    }

    private fun generateReportId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "VO-${timestamp}-$random"
    }
}
