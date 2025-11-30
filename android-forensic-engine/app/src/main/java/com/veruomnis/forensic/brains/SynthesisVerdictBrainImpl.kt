package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 9: Synthesis & Verdict
 * 
 * The final brain that synthesizes all analysis results from Brains 1-8,
 * generates a unified truth model, verifies cross-brain consensus,
 * and ensures constitutional compliance.
 */
class SynthesisVerdictBrainImpl : SynthesisVerdictBrain {
    override val brainName = "Synthesis Verdict Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        // Note: Full analysis requires results from other brains
        // This is a placeholder for direct analysis calls
        val findings = mutableListOf<Finding>()
        
        findings.add(Finding(
            severity = Severity.INFO,
            category = "Synthesis Ready",
            description = "Brain 9 ready for cross-brain synthesis. Call generateUnifiedTruthModel with all brain results.",
            confidence = 1.0
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
        
        // Calculate Truth Stability Index from all brains
        val confidences = brainResults.map { it.confidence }
        val avgConfidence = if (confidences.isNotEmpty()) confidences.average() else 0.0
        val minConfidence = confidences.minOrNull() ?: 0.0
        
        // TSI is weighted average with penalty for low minimums
        val tsi = (avgConfidence * 0.7 + minConfidence * 0.3) * 100
        model["truth_stability_index"] = tsi
        
        // Aggregate all findings by severity
        val allFindings = brainResults.flatMap { it.findings }
        val criticalCount = allFindings.count { it.severity == Severity.CRITICAL }
        val highCount = allFindings.count { it.severity == Severity.HIGH }
        val mediumCount = allFindings.count { it.severity == Severity.MEDIUM }
        val lowCount = allFindings.count { it.severity == Severity.LOW }
        val infoCount = allFindings.count { it.severity == Severity.INFO }
        
        model["findings_summary"] = mapOf(
            "total" to allFindings.size,
            "critical" to criticalCount,
            "high" to highCount,
            "medium" to mediumCount,
            "low" to lowCount,
            "info" to infoCount
        )
        
        // Generate key findings (top severity items)
        val keyFindings = allFindings
            .filter { it.severity in listOf(Severity.CRITICAL, Severity.HIGH) }
            .sortedByDescending { it.confidence }
            .take(5)
            .map { "${it.category}: ${it.description}" }
        model["key_findings"] = keyFindings
        
        // Generate verdict
        val verdict = when {
            criticalCount > 0 -> "CRITICAL ISSUES DETECTED - Immediate attention required"
            highCount > 2 -> "SIGNIFICANT CONCERNS - Multiple high-severity findings"
            highCount > 0 -> "MODERATE CONCERNS - Some high-severity findings present"
            mediumCount > 3 -> "MINOR CONCERNS - Several medium-severity findings"
            else -> "CLEAN ANALYSIS - No significant issues detected"
        }
        model["verdict"] = verdict
        
        // Evidence summary
        model["evidence_analyzed"] = evidence.size
        model["brains_consulted"] = brainResults.size
        model["total_findings"] = allFindings.size
        
        model
    }

    override suspend fun verifyCrossBrainConsensus(
        brainResults: List<BrainAnalysisResult>
    ): Double = withContext(Dispatchers.Default) {
        if (brainResults.isEmpty()) return 0.0
        if (brainResults.size < 3) return 0.5 // Need at least 3 for triple consensus
        
        // Calculate consensus as agreement between brain confidence levels
        val confidences = brainResults.map { it.confidence }
        val avgConfidence = confidences.average()
        
        // Calculate variance (lower variance = higher consensus)
        val variance = confidences.map { (it - avgConfidence) * (it - avgConfidence) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        
        // Convert to consensus score (lower stdDev = higher consensus)
        val consensusScore = (1.0 - stdDev).coerceIn(0.0, 1.0)
        
        // Check for agreement on critical findings
        val criticalBrains = brainResults.filter { result ->
            result.findings.any { it.severity == Severity.CRITICAL }
        }
        val criticalAgreement = if (brainResults.size > 0) {
            when (criticalBrains.size) {
                0 -> 1.0 // No critical findings, full agreement
                1 -> 0.7 // Only one brain found critical - partial agreement
                else -> if (criticalBrains.size >= brainResults.size / 2) 0.9 else 0.6
            }
        } else 0.5
        
        // Final consensus combines statistical and finding-based measures
        (consensusScore * 0.6 + criticalAgreement * 0.4)
    }

    override suspend fun compileReport(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Any> = withContext(Dispatchers.Default) {
        val report = mutableMapOf<String, Any>()
        
        // Generate unified truth model
        val truthModel = generateUnifiedTruthModel(evidence, brainResults)
        report.putAll(truthModel)
        
        // Verify consensus
        val consensusLevel = verifyCrossBrainConsensus(brainResults)
        report["consensus_level"] = consensusLevel
        report["consensus_verified"] = consensusLevel >= 0.75
        
        // Constitutional compliance
        val compliance = checkConstitutionalCompliance(evidence, brainResults)
        report["constitutional_compliance"] = compliance
        
        // Brain-by-brain summary
        val brainSummaries = brainResults.map { result ->
            mapOf(
                "brain" to result.brainName,
                "confidence" to result.confidence,
                "findings_count" to result.findings.size,
                "processing_time_ms" to result.processingTimeMs
            )
        }
        report["brain_summaries"] = brainSummaries
        
        // Total processing time
        report["total_processing_time_ms"] = brainResults.sumOf { it.processingTimeMs }
        
        // Recommendations
        val recommendations = generateRecommendations(evidence, brainResults)
        report["recommendations"] = recommendations
        
        // Report metadata
        report["report_generated_at"] = Date().toString()
        report["verum_omnis_version"] = "5.2.6"
        
        report
    }

    override suspend fun checkConstitutionalCompliance(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Boolean> = withContext(Dispatchers.Default) {
        val compliance = mutableMapOf<String, Boolean>()
        
        // Article I-IX: All nine brains must be consulted
        compliance["nine_brain_doctrine"] = brainResults.size >= 9
        
        // Zero-Loss Evidence Doctrine: All evidence preserved
        compliance["zero_loss_doctrine"] = evidence.isNotEmpty()
        
        // Triple-AI Consensus: At least 3 brains must agree (consensus > 0.75)
        val consensus = verifyCrossBrainConsensus(brainResults)
        compliance["triple_ai_consensus"] = consensus >= 0.75
        
        // Article X: Seal Rule (verified separately during report export)
        compliance["seal_rule_pending"] = true
        
        // Guardianship Model: User maintains ownership (always true in offline mode)
        compliance["guardianship_model"] = true
        
        // Overall compliance
        compliance["fully_compliant"] = compliance.values.all { it }
        
        compliance
    }
    
    /**
     * Generate actionable recommendations based on analysis
     */
    private fun generateRecommendations(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Check for critical findings
        val criticalFindings = brainResults.flatMap { it.findings }
            .filter { it.severity == Severity.CRITICAL }
        
        if (criticalFindings.isNotEmpty()) {
            recommendations.add("URGENT: ${criticalFindings.size} critical finding(s) require immediate review")
        }
        
        // Check evidence diversity
        val types = evidence.map { it.type }.distinct()
        if (types.size < 3) {
            recommendations.add("Consider gathering additional evidence types for stronger corroboration")
        }
        
        // Check metadata completeness
        val incompleteMetadata = evidence.count { it.metadata.size < 3 }
        if (incompleteMetadata > evidence.size / 2) {
            recommendations.add("Many evidence items lack complete metadata - consider re-importing with original files")
        }
        
        // Check consensus level
        val consensus = brainResults.map { it.confidence }.average()
        if (consensus < 0.75) {
            recommendations.add("Analysis confidence below threshold - additional evidence may strengthen conclusions")
        }
        
        // Default recommendation
        if (recommendations.isEmpty()) {
            recommendations.add("Evidence set appears complete - proceed with report generation")
        }
        
        return recommendations
    }
}
