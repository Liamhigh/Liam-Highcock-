package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 8: Predictive Analytics
 * "All outcomes leave statistical footprints."
 */
class PredictiveAnalyticsBrainImpl : PredictiveAnalyticsBrain {
    override val brainName = "Predictive Analytics Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Model risk probability
        val riskProbability = modelRiskProbability(evidence)
        findings.add(Finding(
            severity = when {
                riskProbability > 0.7 -> Severity.CRITICAL
                riskProbability > 0.5 -> Severity.HIGH
                riskProbability > 0.3 -> Severity.MEDIUM
                else -> Severity.LOW
            },
            category = "Risk Assessment",
            description = "Overall risk probability: ${(riskProbability * 100).toInt()}%",
            confidence = 0.82,
            details = mapOf(
                "risk_probability" to riskProbability,
                "risk_level" to when {
                    riskProbability > 0.7 -> "Critical"
                    riskProbability > 0.5 -> "High"
                    riskProbability > 0.3 -> "Medium"
                    else -> "Low"
                }
            )
        ))

        // Forecast behavior escalation
        val escalationForecast = forecastBehaviorEscalation(evidence)
        val escalationLikelihood = escalationForecast["escalation_likelihood"] as? Double ?: 0.0
        findings.add(Finding(
            severity = if (escalationLikelihood > 0.5) Severity.HIGH else Severity.MEDIUM,
            category = "Escalation Forecast",
            description = "Behavior escalation likelihood: ${(escalationLikelihood * 100).toInt()}%",
            confidence = 0.75,
            details = escalationForecast
        ))

        // Predict evidence gaps
        val predictedGaps = predictEvidenceGaps(evidence)
        if (predictedGaps.isNotEmpty()) {
            findings.add(Finding(
                severity = Severity.MEDIUM,
                category = "Evidence Gaps",
                description = "Predicted ${predictedGaps.size} potential evidence gaps",
                confidence = 0.70,
                details = mapOf(
                    "predicted_gaps" to predictedGaps,
                    "gap_count" to predictedGaps.size
                )
            ))
        }

        // Score outcome probability
        val outcomeProbabilities = scoreOutcomeProbability(evidence)
        findings.add(Finding(
            severity = Severity.INFO,
            category = "Outcome Prediction",
            description = "Case outcome probabilities calculated",
            confidence = 0.78,
            details = outcomeProbabilities
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
        
        // Factor 1: Evidence volume and complexity
        val volumeRisk = when {
            evidence.isEmpty() -> 0.1
            evidence.size < 5 -> 0.2
            evidence.size < 20 -> 0.3
            evidence.size < 50 -> 0.4
            else -> 0.5
        }
        riskScore += volumeRisk * 0.15

        // Factor 2: Evidence type diversity
        val typeCount = evidence.map { it.type }.distinct().size
        val diversityRisk = (typeCount.toDouble() / EvidenceType.values().size) * 0.5
        riskScore += diversityRisk * 0.15

        // Factor 3: Temporal patterns
        if (evidence.size > 1) {
            val sortedEvidence = evidence.sortedBy { it.dateAdded }
            val timeSpanDays = (sortedEvidence.last().dateAdded.time - 
                sortedEvidence.first().dateAdded.time) / (1000 * 60 * 60 * 24)
            
            // Short time spans with lots of evidence = higher risk
            val temporalRisk = if (timeSpanDays > 0) {
                (evidence.size.toDouble() / timeSpanDays).coerceIn(0.0, 1.0)
            } else 0.5
            riskScore += temporalRisk * 0.2
        }

        // Factor 4: Metadata completeness (incomplete = higher risk)
        val incompleteEvidence = evidence.count { it.metadata.isEmpty() || it.metadata.size < 3 }
        val completenessRisk = incompleteEvidence.toDouble() / evidence.size.coerceAtLeast(1)
        riskScore += completenessRisk * 0.2

        // Factor 5: Hash verification status
        val unverifiedCount = evidence.count { !it.metadata.containsKey("hash") }
        val verificationRisk = unverifiedCount.toDouble() / evidence.size.coerceAtLeast(1)
        riskScore += verificationRisk * 0.15

        // Factor 6: Sensitive content indicators
        val sensitiveEvidence = evidence.count { ev ->
            ev.metadata["contains_personal_data"] as? Boolean ?: false ||
            ev.type == EvidenceType.AUDIO || ev.type == EvidenceType.VIDEO
        }
        val sensitivityRisk = sensitiveEvidence.toDouble() / evidence.size.coerceAtLeast(1)
        riskScore += sensitivityRisk * 0.15

        return riskScore.coerceIn(0.0, 1.0)
    }

    override suspend fun forecastBehaviorEscalation(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        var escalationScore = 0.0
        val escalationIndicators = mutableListOf<String>()

        // Analyze frequency of evidence submission
        if (evidence.size > 1) {
            val sortedEvidence = evidence.sortedBy { it.dateAdded }
            val intervals = sortedEvidence.zipWithNext { a, b ->
                (b.dateAdded.time - a.dateAdded.time) / (1000 * 60 * 60) // hours
            }

            // Decreasing intervals suggest escalating behavior
            if (intervals.size >= 2) {
                val firstHalf = intervals.take(intervals.size / 2).average()
                val secondHalf = intervals.takeLast(intervals.size / 2).average()
                
                if (secondHalf < firstHalf * 0.5) {
                    escalationScore += 0.3
                    escalationIndicators.add("Evidence submission frequency is increasing")
                }
            }

            // Very rapid submissions
            val rapidCount = intervals.count { it < 1 } // Less than 1 hour
            if (rapidCount > intervals.size * 0.5) {
                escalationScore += 0.2
                escalationIndicators.add("Multiple evidence items submitted in rapid succession")
            }
        }

        // Analyze evidence type escalation
        val audioVideoCount = evidence.count { 
            it.type == EvidenceType.AUDIO || it.type == EvidenceType.VIDEO 
        }
        if (audioVideoCount > evidence.size * 0.5) {
            escalationScore += 0.15
            escalationIndicators.add("High proportion of audio/video evidence suggests documentation escalation")
        }

        // Check for urgency indicators in metadata
        val urgentEvidence = evidence.count { ev ->
            val notes = ev.metadata["notes"] as? String ?: ""
            listOf("urgent", "immediate", "emergency", "now", "asap").any { 
                notes.lowercase().contains(it) 
            }
        }
        if (urgentEvidence > 0) {
            escalationScore += urgentEvidence * 0.1
            escalationIndicators.add("$urgentEvidence evidence items marked with urgency indicators")
        }

        result["escalation_likelihood"] = escalationScore.coerceIn(0.0, 1.0)
        result["escalation_indicators"] = escalationIndicators
        result["escalation_trend"] = when {
            escalationScore > 0.6 -> "Rapidly Escalating"
            escalationScore > 0.4 -> "Moderately Escalating"
            escalationScore > 0.2 -> "Slightly Escalating"
            else -> "Stable"
        }
        result["recommendation"] = when {
            escalationScore > 0.6 -> "Immediate attention recommended"
            escalationScore > 0.4 -> "Monitor closely"
            escalationScore > 0.2 -> "Standard monitoring"
            else -> "No escalation concerns"
        }

        result
    }

    override suspend fun predictEvidenceGaps(evidence: List<Evidence>): List<String> {
        val gaps = mutableListOf<String>()

        // Analyze evidence types present
        val presentTypes = evidence.map { it.type }.toSet()

        // Suggest missing corroborating evidence types
        if (EvidenceType.DOCUMENT in presentTypes && EvidenceType.IMAGE !in presentTypes) {
            gaps.add("No image evidence to corroborate document claims")
        }

        if (EvidenceType.AUDIO in presentTypes && EvidenceType.DOCUMENT !in presentTypes) {
            gaps.add("No transcripts or written records to accompany audio evidence")
        }

        if (EvidenceType.VIDEO in presentTypes && EvidenceType.AUDIO !in presentTypes) {
            gaps.add("Consider extracting audio track from video for separate analysis")
        }

        // Check for temporal gaps
        if (evidence.size > 1) {
            val sortedEvidence = evidence.sortedBy { it.dateAdded }
            
            for (i in 0 until sortedEvidence.size - 1) {
                val gapHours = (sortedEvidence[i + 1].dateAdded.time - 
                    sortedEvidence[i].dateAdded.time) / (1000 * 60 * 60)
                
                if (gapHours > 168) { // More than a week
                    gaps.add("Week-long gap between ${sortedEvidence[i].fileName} and ${sortedEvidence[i + 1].fileName}")
                }
            }
        }

        // Check for metadata gaps
        val withoutLocation = evidence.count { 
            !it.metadata.containsKey("latitude") && !it.metadata.containsKey("longitude") 
        }
        if (withoutLocation > evidence.size * 0.7 && withoutLocation > 0) {
            gaps.add("$withoutLocation evidence items lack location data for geographic correlation")
        }

        val withoutTimestamp = evidence.count { 
            !it.metadata.containsKey("created_date") && !it.metadata.containsKey("modified_date")
        }
        if (withoutTimestamp > evidence.size * 0.5 && withoutTimestamp > 0) {
            gaps.add("$withoutTimestamp evidence items lack precise timestamp data")
        }

        // Check for authentication gaps
        val withoutHash = evidence.count { !it.metadata.containsKey("hash") }
        if (withoutHash > 0) {
            gaps.add("$withoutHash evidence items lack cryptographic hash verification")
        }

        gaps
    }

    override suspend fun scoreOutcomeProbability(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        // Base probabilities
        var authenticEvidence = 0.5
        var sufficientEvidence = 0.5
        var compliantEvidence = 0.5

        // Adjust based on evidence characteristics
        
        // Authenticity indicators
        val hashVerified = evidence.count { it.metadata.containsKey("hash") }
        authenticEvidence += (hashVerified.toDouble() / evidence.size.coerceAtLeast(1)) * 0.3

        val withMetadata = evidence.count { it.metadata.isNotEmpty() }
        authenticEvidence += (withMetadata.toDouble() / evidence.size.coerceAtLeast(1)) * 0.2

        // Sufficiency indicators
        val typeCount = evidence.map { it.type }.distinct().size
        sufficientEvidence += (typeCount.toDouble() / 4) * 0.25 // 4 main types

        sufficientEvidence += if (evidence.size >= 5) 0.15 else (evidence.size.toDouble() / 5) * 0.15

        // Check for corroborating evidence
        val hasCorroboration = evidence.groupBy { it.type }.values.any { it.size > 1 }
        if (hasCorroboration) {
            sufficientEvidence += 0.1
        }

        // Compliance indicators
        val withChainOfCustody = evidence.count { 
            it.metadata.containsKey("source") || it.metadata.containsKey("created_by") 
        }
        compliantEvidence += (withChainOfCustody.toDouble() / evidence.size.coerceAtLeast(1)) * 0.25

        val withDateAdded = evidence.count { it.dateAdded.time > 0 }
        compliantEvidence += (withDateAdded.toDouble() / evidence.size.coerceAtLeast(1)) * 0.25

        // Calculate overall outcome probabilities
        val strengthScore = (authenticEvidence + sufficientEvidence + compliantEvidence) / 3

        result["evidence_authenticity"] = authenticEvidence.coerceIn(0.0, 1.0)
        result["evidence_sufficiency"] = sufficientEvidence.coerceIn(0.0, 1.0)
        result["evidence_compliance"] = compliantEvidence.coerceIn(0.0, 1.0)
        result["overall_strength"] = strengthScore.coerceIn(0.0, 1.0)

        result["outcome_scenarios"] = mapOf(
            "strong_case" to if (strengthScore > 0.7) "Likely" else "Unlikely",
            "moderate_case" to if (strengthScore in 0.4..0.7) "Likely" else "Unlikely",
            "weak_case" to if (strengthScore < 0.4) "Likely" else "Unlikely"
        )

        result["recommendation"] = when {
            strengthScore > 0.75 -> "Evidence appears strong and well-documented"
            strengthScore > 0.5 -> "Evidence has moderate strength, consider additional documentation"
            strengthScore > 0.3 -> "Evidence may require significant strengthening"
            else -> "Evidence appears insufficient for most legal purposes"
        }

        result
    }
}
