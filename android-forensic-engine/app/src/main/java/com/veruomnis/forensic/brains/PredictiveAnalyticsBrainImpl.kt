package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 8: Predictive Analytics
 * 
 * Models risk probability, forecasts behavior escalation,
 * predicts evidence gaps, and scores outcome probability.
 */
class PredictiveAnalyticsBrainImpl : PredictiveAnalyticsBrain {
    override val brainName = "Predictive Analytics Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Model risk probability
        val riskScore = modelRiskProbability(evidence)
        findings.add(Finding(
            severity = when {
                riskScore > 0.8 -> Severity.CRITICAL
                riskScore > 0.6 -> Severity.HIGH
                riskScore > 0.4 -> Severity.MEDIUM
                riskScore > 0.2 -> Severity.LOW
                else -> Severity.INFO
            },
            category = "Risk Assessment",
            description = "Overall risk probability: ${(riskScore * 100).toInt()}%",
            confidence = 0.80,
            details = mapOf("risk_score" to riskScore)
        ))

        // Forecast behavior escalation
        val escalation = forecastBehaviorEscalation(evidence)
        val escalationRisk = escalation["escalation_probability"] as? Double ?: 0.0
        findings.add(Finding(
            severity = if (escalationRisk > 0.5) Severity.HIGH else Severity.MEDIUM,
            category = "Behavior Forecast",
            description = escalation["description"] as? String ?: "Escalation analysis complete",
            confidence = 0.75,
            details = escalation
        ))

        // Predict evidence gaps
        val gaps = predictEvidenceGaps(evidence)
        gaps.forEach { gap ->
            findings.add(Finding(
                severity = Severity.MEDIUM,
                category = "Evidence Gap Prediction",
                description = gap,
                confidence = 0.70
            ))
        }

        // Score outcome probability
        val outcomes = scoreOutcomeProbability(evidence)
        findings.add(Finding(
            severity = Severity.INFO,
            category = "Outcome Analysis",
            description = outcomes["description"] as? String ?: "Outcome probabilities calculated",
            confidence = 0.75,
            details = outcomes
        ))

        val processingTime = System.currentTimeMillis() - startTime
        BrainAnalysisResult(
            brainName = brainName,
            confidence = if (findings.isEmpty()) 0.75 else findings.maxOfOrNull { it.confidence } ?: 0.0,
            findings = findings,
            timestamp = Date(),
            processingTimeMs = processingTime
        )
    }

    override suspend fun modelRiskProbability(evidence: List<Evidence>): Double {
        var riskScore = 0.0
        
        if (evidence.isEmpty()) return 0.0
        
        // Factor 1: Evidence diversity (more types = stronger case)
        val typeCount = evidence.map { it.type }.distinct().size
        riskScore += typeCount * 0.1
        
        // Factor 2: Evidence volume
        riskScore += minOf(evidence.size * 0.05, 0.3)
        
        // Factor 3: Metadata completeness
        val avgMetadataCount = evidence.map { it.metadata.size }.average()
        riskScore += minOf(avgMetadataCount * 0.02, 0.2)
        
        // Factor 4: Timeline coverage
        if (evidence.size >= 2) {
            val sortedDates = evidence.map { it.dateAdded }.sorted()
            val timeSpan = sortedDates.last().time - sortedDates.first().time
            val daysCovered = timeSpan / (1000 * 60 * 60 * 24)
            if (daysCovered > 30) riskScore += 0.1
            if (daysCovered > 90) riskScore += 0.1
        }
        
        return riskScore.coerceIn(0.0, 1.0)
    }

    override suspend fun forecastBehaviorEscalation(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        // Analyze pattern progression in evidence
        var escalationProbability = 0.2 // Base probability
        val indicators = mutableListOf<String>()
        
        if (evidence.size >= 5) {
            // Check for increasing frequency
            val sortedEvidence = evidence.sortedBy { it.dateAdded }
            val timeDiffs = sortedEvidence.zipWithNext { a, b ->
                b.dateAdded.time - a.dateAdded.time
            }
            
            if (timeDiffs.isNotEmpty()) {
                val avgFirst = timeDiffs.take(timeDiffs.size / 2).average()
                val avgSecond = timeDiffs.drop(timeDiffs.size / 2).average()
                
                if (avgSecond < avgFirst * 0.5) {
                    escalationProbability += 0.3
                    indicators.add("Evidence frequency increasing over time")
                }
            }
        }
        
        // Check for evidence severity progression
        // (Would analyze content in full implementation)
        
        result["escalation_probability"] = escalationProbability
        result["indicators"] = indicators
        result["description"] = when {
            escalationProbability > 0.7 -> "HIGH risk of behavior escalation detected"
            escalationProbability > 0.4 -> "MODERATE indicators of escalation present"
            else -> "LOW escalation risk based on current evidence"
        }
        
        return result
    }

    override suspend fun predictEvidenceGaps(evidence: List<Evidence>): List<String> {
        val gaps = mutableListOf<String>()
        
        // Predict what evidence might be missing
        val types = evidence.map { it.type }.distinct()
        
        if (EvidenceType.DOCUMENT !in types) {
            gaps.add("No documentary evidence - written records may strengthen case")
        }
        
        if (EvidenceType.IMAGE !in types && EvidenceType.VIDEO !in types) {
            gaps.add("No visual evidence - photos/videos may provide corroboration")
        }
        
        if (EvidenceType.AUDIO !in types && EvidenceType.VIDEO !in types) {
            gaps.add("No audio evidence - recordings may establish context/intent")
        }
        
        // Check for temporal gaps
        if (evidence.size >= 2) {
            val sorted = evidence.sortedBy { it.dateAdded }
            for (i in 0 until sorted.size - 1) {
                val gap = sorted[i + 1].dateAdded.time - sorted[i].dateAdded.time
                val gapDays = gap / (1000 * 60 * 60 * 24)
                if (gapDays > 30) {
                    gaps.add("$gapDays day gap between evidence items - additional records for this period recommended")
                    break // Only report first major gap
                }
            }
        }
        
        // Check metadata gaps
        val missingLocation = evidence.none { 
            it.metadata.containsKey("latitude") || it.metadata.containsKey("location")
        }
        if (missingLocation) {
            gaps.add("No location data in evidence - GPS-enabled sources recommended")
        }
        
        return gaps
    }

    override suspend fun scoreOutcomeProbability(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        // Calculate probabilities based on evidence strength
        val strength = modelRiskProbability(evidence)
        
        result["favorable_outcome"] = (0.4 + strength * 0.4).coerceIn(0.0, 0.85)
        result["neutral_outcome"] = 0.15
        result["unfavorable_outcome"] = (0.45 - strength * 0.35).coerceIn(0.05, 0.45)
        
        result["strength_factors"] = mapOf(
            "evidence_count" to evidence.size,
            "evidence_types" to evidence.map { it.type }.distinct().size,
            "metadata_completeness" to evidence.map { it.metadata.size }.average()
        )
        
        result["description"] = "Outcome probabilities based on evidence strength analysis"
        
        return result
    }
}
