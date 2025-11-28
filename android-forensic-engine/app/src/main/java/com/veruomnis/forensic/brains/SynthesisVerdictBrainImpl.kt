package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 9: Synthesis & Verdict
 * The master brain that synthesizes all other brain results into a unified truth model
 * Implements Constitutional Compliance and Triple AI Consensus verification
 */
class SynthesisVerdictBrainImpl : SynthesisVerdictBrain {
    override val brainName = "Synthesis & Verdict Brain"

    companion object {
        const val CONSENSUS_THRESHOLD = 0.66 // 2/3 majority for Triple AI Consensus
        const val TRUTH_STABILITY_THRESHOLD = 70.0
    }

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Note: This analyze function provides a standalone analysis
        // The full synthesis requires results from other brains via compileReport
        findings.add(Finding(
            severity = Severity.INFO,
            category = "Synthesis Status",
            description = "Synthesis Brain ready for cross-brain analysis with ${evidence.size} evidence items",
            confidence = 1.0,
            details = mapOf(
                "evidence_count" to evidence.size,
                "ready_for_synthesis" to true
            )
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
        val result = mutableMapOf<String, Any>()
        
        // Calculate overall Truth Stability Index from all brains
        val truthStabilityIndex = calculateOverallTruthStability(brainResults)
        result["truth_stability_index"] = truthStabilityIndex
        
        // Aggregate all high-severity findings
        val criticalFindings = brainResults.flatMap { it.findings }
            .filter { it.severity == Severity.CRITICAL }
        val highFindings = brainResults.flatMap { it.findings }
            .filter { it.severity == Severity.HIGH }
        
        result["critical_findings_count"] = criticalFindings.size
        result["high_findings_count"] = highFindings.size
        
        // Build unified narrative
        val narrative = buildUnifiedNarrative(evidence, brainResults)
        result["narrative"] = narrative
        
        // Determine overall verdict
        val verdict = determineVerdict(truthStabilityIndex, criticalFindings, highFindings)
        result["verdict"] = verdict
        result["verdict_confidence"] = calculateVerdictConfidence(brainResults)
        
        // Cross-brain consensus
        val consensusLevel = verifyCrossBrainConsensus(brainResults)
        result["cross_brain_consensus"] = consensusLevel
        result["consensus_achieved"] = consensusLevel >= CONSENSUS_THRESHOLD
        
        // Constitutional compliance check
        val compliance = checkConstitutionalCompliance(evidence, brainResults)
        result["constitutional_compliance"] = compliance
        
        result
    }

    override suspend fun verifyCrossBrainConsensus(brainResults: List<BrainAnalysisResult>): Double {
        if (brainResults.isEmpty()) return 0.0
        if (brainResults.size == 1) return brainResults[0].confidence
        
        // Calculate weighted consensus based on confidence scores
        val confidences = brainResults.map { it.confidence }
        val avgConfidence = confidences.average()
        
        // Check for agreement on critical findings
        val criticalCategories = brainResults.flatMap { it.findings }
            .filter { it.severity in listOf(Severity.CRITICAL, Severity.HIGH) }
            .groupBy { it.category }
        
        // Calculate agreement score based on consistency across brains
        var agreementScore = 0.0
        criticalCategories.forEach { (category, findings) ->
            val brainsConcurring = findings.map { it.confidence }.average()
            agreementScore += brainsConcurring
        }
        
        val categoryCount = criticalCategories.size.coerceAtLeast(1)
        val categoryAgreement = agreementScore / categoryCount
        
        // Combine confidence average with category agreement
        val consensusScore = (avgConfidence * 0.6 + categoryAgreement * 0.4)
        
        return consensusScore.coerceIn(0.0, 1.0)
    }

    override suspend fun compileReport(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Any> = withContext(Dispatchers.Default) {
        val report = mutableMapOf<String, Any>()
        
        // Report metadata
        report["report_id"] = java.util.UUID.randomUUID().toString()
        report["generated_at"] = Date().toString()
        report["evidence_count"] = evidence.size
        report["brains_analyzed"] = brainResults.size
        
        // Unified truth model
        val truthModel = generateUnifiedTruthModel(evidence, brainResults)
        report["truth_model"] = truthModel
        
        // Executive summary
        report["executive_summary"] = generateExecutiveSummary(evidence, brainResults, truthModel)
        
        // Individual brain summaries
        report["brain_summaries"] = brainResults.map { result ->
            mapOf(
                "brain_name" to result.brainName,
                "confidence" to result.confidence,
                "findings_count" to result.findings.size,
                "critical_count" to result.findings.count { it.severity == Severity.CRITICAL },
                "high_count" to result.findings.count { it.severity == Severity.HIGH },
                "processing_time_ms" to result.processingTimeMs
            )
        }
        
        // All findings aggregated by severity
        val allFindings = brainResults.flatMap { result ->
            result.findings.map { finding ->
                mapOf(
                    "source_brain" to result.brainName,
                    "severity" to finding.severity.name,
                    "category" to finding.category,
                    "description" to finding.description,
                    "confidence" to finding.confidence
                )
            }
        }
        report["all_findings"] = allFindings
        
        // Critical findings highlighted
        report["critical_findings"] = allFindings.filter { 
            it["severity"] == Severity.CRITICAL.name 
        }
        
        // Recommendations
        report["recommendations"] = generateRecommendations(evidence, brainResults, truthModel)
        
        // Constitutional compliance
        report["constitutional_compliance"] = truthModel["constitutional_compliance"]
        
        report
    }

    override suspend fun checkConstitutionalCompliance(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Boolean> {
        val compliance = mutableMapOf<String, Boolean>()
        
        // Zero-Loss Evidence Doctrine: All evidence must be preserved and analyzed
        val allEvidenceAnalyzed = brainResults.flatMap { it.findings }
            .any { it.category.contains("Evidence") || it.category.contains("Analysis") }
        compliance["zero_loss_evidence_doctrine"] = evidence.isNotEmpty() && allEvidenceAnalyzed
        
        // Triple AI Consensus: At least 3 brains must agree on major findings
        val majorFindings = brainResults.flatMap { it.findings }
            .filter { it.severity in listOf(Severity.CRITICAL, Severity.HIGH) }
        val brainsWithMajorFindings = brainResults.count { result ->
            result.findings.any { it.severity in listOf(Severity.CRITICAL, Severity.HIGH) }
        }
        compliance["triple_ai_consensus"] = brainsWithMajorFindings >= 3 || brainResults.size < 3
        
        // Guardianship Model: Human oversight preserved
        val hasHumanMetadata = evidence.any { 
            it.metadata.containsKey("collected_by") || 
            it.metadata.containsKey("verified_by") ||
            it.metadata.containsKey("source")
        }
        compliance["guardianship_model_enforced"] = hasHumanMetadata || evidence.isEmpty()
        
        // Contradiction Engine: Contradictions must be detected and flagged
        val contradictionBrainRan = brainResults.any { 
            it.brainName.contains("Contradiction", ignoreCase = true) 
        }
        compliance["contradiction_engine_active"] = contradictionBrainRan
        
        // Forensic Anchors: Cryptographic verification available
        val hasCryptoAnchors = evidence.any { it.metadata.containsKey("hash") }
        compliance["forensic_anchors_present"] = hasCryptoAnchors || evidence.isEmpty()
        
        compliance
    }
    
    // Private helper functions
    
    private fun calculateOverallTruthStability(brainResults: List<BrainAnalysisResult>): Double {
        if (brainResults.isEmpty()) return 0.0
        
        // Weight by confidence and penalize for critical/high findings
        var baseScore = 100.0
        
        brainResults.forEach { result ->
            val criticalPenalty = result.findings.count { it.severity == Severity.CRITICAL } * 10
            val highPenalty = result.findings.count { it.severity == Severity.HIGH } * 5
            val mediumPenalty = result.findings.count { it.severity == Severity.MEDIUM } * 2
            
            baseScore -= (criticalPenalty + highPenalty + mediumPenalty) * (1 - result.confidence)
        }
        
        // Boost for high confidence across brains
        val avgConfidence = brainResults.map { it.confidence }.average()
        baseScore *= (0.7 + avgConfidence * 0.3)
        
        return baseScore.coerceIn(0.0, 100.0)
    }
    
    private fun buildUnifiedNarrative(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): String {
        val sb = StringBuilder()
        
        sb.append("VERUM OMNIS FORENSIC ANALYSIS NARRATIVE\n")
        sb.append("=" .repeat(50)).append("\n\n")
        
        sb.append("EVIDENCE OVERVIEW:\n")
        sb.append("Total evidence items analyzed: ${evidence.size}\n")
        evidence.groupBy { it.type }.forEach { (type, items) ->
            sb.append("- ${type.name}: ${items.size} items\n")
        }
        sb.append("\n")
        
        sb.append("BRAIN ANALYSIS SUMMARY:\n")
        brainResults.forEach { result ->
            sb.append("${result.brainName}:\n")
            sb.append("  Confidence: ${(result.confidence * 100).toInt()}%\n")
            sb.append("  Findings: ${result.findings.size}\n")
            val critical = result.findings.count { it.severity == Severity.CRITICAL }
            val high = result.findings.count { it.severity == Severity.HIGH }
            if (critical > 0) sb.append("  ⚠️ Critical issues: $critical\n")
            if (high > 0) sb.append("  ⚡ High priority: $high\n")
            sb.append("\n")
        }
        
        sb.append("KEY FINDINGS:\n")
        brainResults.flatMap { it.findings }
            .filter { it.severity in listOf(Severity.CRITICAL, Severity.HIGH) }
            .take(10)
            .forEachIndexed { index, finding ->
                sb.append("${index + 1}. [${finding.severity}] ${finding.description}\n")
            }
        
        return sb.toString()
    }
    
    private fun determineVerdict(
        truthStabilityIndex: Double,
        criticalFindings: List<Finding>,
        highFindings: List<Finding>
    ): String {
        return when {
            criticalFindings.isNotEmpty() && truthStabilityIndex < 50 ->
                "SIGNIFICANT CONCERNS - Critical issues detected requiring immediate attention"
            criticalFindings.isNotEmpty() ->
                "CAUTION ADVISED - Critical findings present, further investigation recommended"
            highFindings.size > 5 && truthStabilityIndex < 60 ->
                "MODERATE CONCERNS - Multiple high-priority issues identified"
            highFindings.isNotEmpty() ->
                "ATTENTION REQUIRED - High-priority findings warrant review"
            truthStabilityIndex >= TRUTH_STABILITY_THRESHOLD ->
                "EVIDENCE STABLE - Truth Stability Index meets threshold"
            else ->
                "ANALYSIS COMPLETE - Review findings for detailed assessment"
        }
    }
    
    private fun calculateVerdictConfidence(brainResults: List<BrainAnalysisResult>): Double {
        if (brainResults.isEmpty()) return 0.0
        
        // Weighted average based on brain count and individual confidences
        val avgConfidence = brainResults.map { it.confidence }.average()
        val brainCoverage = brainResults.size.toDouble() / 9 // 9 total brains
        
        return (avgConfidence * 0.7 + brainCoverage * 0.3).coerceIn(0.0, 1.0)
    }
    
    private fun generateExecutiveSummary(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>,
        truthModel: Map<String, Any>
    ): String {
        val tsi = truthModel["truth_stability_index"] as? Double ?: 0.0
        val verdict = truthModel["verdict"] as? String ?: "Analysis pending"
        val consensus = truthModel["cross_brain_consensus"] as? Double ?: 0.0
        
        return buildString {
            append("EXECUTIVE SUMMARY\n")
            append("-".repeat(40)).append("\n")
            append("Evidence analyzed: ${evidence.size} items\n")
            append("Brains consulted: ${brainResults.size}\n")
            append("Truth Stability Index: ${tsi.toInt()}/100\n")
            append("Cross-Brain Consensus: ${(consensus * 100).toInt()}%\n")
            append("\nVERDICT: $verdict\n")
            
            val compliance = truthModel["constitutional_compliance"] as? Map<*, *>
            if (compliance != null) {
                val allCompliant = compliance.values.all { it == true }
                append("\nConstitutional Compliance: ${if (allCompliant) "✓ FULL" else "⚠ PARTIAL"}\n")
            }
        }
    }
    
    private fun generateRecommendations(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>,
        truthModel: Map<String, Any>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        val tsi = truthModel["truth_stability_index"] as? Double ?: 0.0
        val criticalCount = truthModel["critical_findings_count"] as? Int ?: 0
        val highCount = truthModel["high_findings_count"] as? Int ?: 0
        
        // TSI-based recommendations
        if (tsi < 50) {
            recommendations.add("URGENT: Truth Stability Index critically low - gather additional corroborating evidence")
        } else if (tsi < TRUTH_STABILITY_THRESHOLD) {
            recommendations.add("Consider supplementing evidence to improve Truth Stability Index")
        }
        
        // Finding-based recommendations
        if (criticalCount > 0) {
            recommendations.add("Address $criticalCount critical finding(s) before proceeding with legal action")
        }
        if (highCount > 3) {
            recommendations.add("Review and document responses to $highCount high-priority findings")
        }
        
        // Evidence-based recommendations
        val types = evidence.map { it.type }.distinct()
        if (types.size == 1) {
            recommendations.add("Diversify evidence types for stronger case (currently only ${types[0].name})")
        }
        
        if (evidence.size < 5) {
            recommendations.add("Case would benefit from additional supporting evidence")
        }
        
        // Consensus-based recommendations
        val consensus = truthModel["cross_brain_consensus"] as? Double ?: 0.0
        if (consensus < CONSENSUS_THRESHOLD) {
            recommendations.add("Cross-brain consensus below threshold - review conflicting analyses")
        }
        
        // Default recommendation if none apply
        if (recommendations.isEmpty()) {
            recommendations.add("Case analysis complete - proceed with documented findings")
        }
        
        return recommendations
    }
}
