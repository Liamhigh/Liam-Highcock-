package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.ln
import kotlin.math.pow

/**
 * Brain 8: Predictive Analytics
 * Models risk probability, forecasts behavior, and predicts evidence gaps
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
                riskProbability > 0.7 -> Severity.HIGH
                riskProbability > 0.4 -> Severity.MEDIUM
                else -> Severity.LOW
            },
            category = "Risk Assessment",
            description = "Overall risk probability: ${(riskProbability * 100).toInt()}%",
            confidence = 0.80,
            details = mapOf("risk_probability" to riskProbability)
        ))

        // Forecast behavior escalation
        val behaviorForecast = forecastBehaviorEscalation(evidence)
        val escalationRisk = behaviorForecast["escalation_risk"] as? Double ?: 0.0
        findings.add(Finding(
            severity = when {
                escalationRisk > 0.6 -> Severity.HIGH
                escalationRisk > 0.3 -> Severity.MEDIUM
                else -> Severity.INFO
            },
            category = "Behavior Forecast",
            description = behaviorForecast["description"] as? String ?: "Behavior analysis complete",
            confidence = behaviorForecast["confidence"] as? Double ?: 0.75,
            details = behaviorForecast
        ))

        // Predict evidence gaps
        val gaps = predictEvidenceGaps(evidence)
        if (gaps.isNotEmpty()) {
            findings.add(Finding(
                severity = Severity.MEDIUM,
                category = "Evidence Gaps",
                description = "Predicted ${gaps.size} evidence gaps that should be addressed",
                confidence = 0.70,
                details = mapOf("predicted_gaps" to gaps)
            ))
        }

        // Score outcome probability
        val outcomeProbability = scoreOutcomeProbability(evidence)
        findings.add(Finding(
            severity = Severity.INFO,
            category = "Outcome Probability",
            description = outcomeProbability["description"] as? String ?: "Outcome scored",
            confidence = outcomeProbability["confidence"] as? Double ?: 0.70,
            details = outcomeProbability
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
        if (evidence.isEmpty()) return 0.0
        
        var riskScore = 0.0
        var factors = 0
        
        // Factor 1: Evidence quantity (more evidence = higher confidence but potentially higher risk)
        val quantityFactor = when {
            evidence.size > 20 -> 0.3
            evidence.size > 10 -> 0.2
            evidence.size > 5 -> 0.1
            else -> 0.05
        }
        riskScore += quantityFactor
        factors++
        
        // Factor 2: Evidence type diversity
        val typesDiversity = evidence.map { it.type }.distinct().size
        val diversityFactor = (typesDiversity.toDouble() / EvidenceType.values().size) * 0.2
        riskScore += diversityFactor
        factors++
        
        // Factor 3: Temporal spread
        if (evidence.size > 1) {
            val sorted = evidence.sortedBy { it.dateAdded }
            val timeSpanDays = (sorted.last().dateAdded.time - sorted.first().dateAdded.time) / (1000.0 * 60 * 60 * 24)
            val temporalFactor = when {
                timeSpanDays > 365 -> 0.3 // Long-running case
                timeSpanDays > 90 -> 0.2
                timeSpanDays > 30 -> 0.15
                else -> 0.1
            }
            riskScore += temporalFactor
            factors++
        }
        
        // Factor 4: Metadata completeness (incomplete metadata = higher risk)
        val metadataCompleteness = evidence.count { it.metadata.isNotEmpty() }.toDouble() / evidence.size
        val metadataFactor = (1 - metadataCompleteness) * 0.2
        riskScore += metadataFactor
        factors++
        
        // Factor 5: Financial indicators
        val hasFinancialData = evidence.any { ev ->
            ev.metadata.keys.any { it in listOf("amount", "transaction", "payment") }
        }
        if (hasFinancialData) {
            riskScore += 0.15
            factors++
        }
        
        return (riskScore / factors * factors).coerceIn(0.0, 1.0)
    }

    override suspend fun forecastBehaviorEscalation(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        if (evidence.size < 2) {
            result["escalation_risk"] = 0.0
            result["description"] = "Insufficient evidence for escalation forecasting"
            result["confidence"] = 0.5
            return result
        }
        
        val sorted = evidence.sortedBy { it.dateAdded }
        
        // Analyze frequency of evidence over time
        val intervals = mutableListOf<Long>()
        for (i in 1 until sorted.size) {
            val interval = sorted[i].dateAdded.time - sorted[i-1].dateAdded.time
            intervals.add(interval)
        }
        
        // Check for accelerating pattern (shorter intervals over time)
        var accelerating = false
        if (intervals.size >= 3) {
            val firstHalf = intervals.take(intervals.size / 2).average()
            val secondHalf = intervals.drop(intervals.size / 2).average()
            accelerating = secondHalf < firstHalf * 0.7 // 30% faster
        }
        
        // Check for increasing severity indicators
        val severityProgression = evidence.mapIndexedNotNull { index, ev ->
            val amount = ev.metadata["amount"] as? Double
            if (amount != null) Pair(index, amount) else null
        }
        
        val increasingSeverity = if (severityProgression.size >= 2) {
            val firstAmount = severityProgression.first().second
            val lastAmount = severityProgression.last().second
            lastAmount > firstAmount * 1.5
        } else false
        
        // Calculate escalation risk
        var escalationRisk = 0.0
        val escalationIndicators = mutableListOf<String>()
        
        if (accelerating) {
            escalationRisk += 0.35
            escalationIndicators.add("Accelerating pattern of incidents detected")
        }
        
        if (increasingSeverity) {
            escalationRisk += 0.30
            escalationIndicators.add("Increasing severity of incidents over time")
        }
        
        // Check evidence recency
        val daysSinceLastEvidence = (Date().time - sorted.last().dateAdded.time) / (1000.0 * 60 * 60 * 24)
        if (daysSinceLastEvidence < 30) {
            escalationRisk += 0.15
            escalationIndicators.add("Recent activity within last 30 days")
        }
        
        result["escalation_risk"] = escalationRisk.coerceIn(0.0, 1.0)
        result["accelerating_pattern"] = accelerating
        result["increasing_severity"] = increasingSeverity
        result["days_since_last_incident"] = daysSinceLastEvidence.toInt()
        result["indicators"] = escalationIndicators
        result["description"] = when {
            escalationRisk > 0.6 -> "HIGH escalation risk - immediate attention recommended"
            escalationRisk > 0.3 -> "MODERATE escalation risk - continued monitoring advised"
            else -> "LOW escalation risk based on current evidence patterns"
        }
        result["confidence"] = 0.75
        
        result
    }

    override suspend fun predictEvidenceGaps(evidence: List<Evidence>): List<String> {
        val gaps = mutableListOf<String>()
        
        val evidenceTypes = evidence.map { it.type }.toSet()
        
        // Suggest missing evidence types that could strengthen the case
        if (EvidenceType.DOCUMENT !in evidenceTypes) {
            gaps.add("Documentary evidence (contracts, emails, statements)")
        }
        
        if (EvidenceType.IMAGE !in evidenceTypes && evidence.any { 
            it.metadata.containsKey("location") || it.metadata.containsKey("physical_item")
        }) {
            gaps.add("Photographic evidence of locations or items mentioned")
        }
        
        if (EvidenceType.AUDIO !in evidenceTypes && evidence.any {
            it.metadata.containsKey("conversation") || it.metadata.containsKey("verbal_agreement")
        }) {
            gaps.add("Audio recordings of conversations mentioned in evidence")
        }
        
        // Check for timeline gaps
        if (evidence.size >= 2) {
            val sorted = evidence.sortedBy { it.dateAdded }
            for (i in 0 until sorted.size - 1) {
                val gap = sorted[i + 1].dateAdded.time - sorted[i].dateAdded.time
                val gapDays = gap / (1000.0 * 60 * 60 * 24)
                if (gapDays > 30) {
                    gaps.add("Evidence for period between ${sorted[i].dateAdded} and ${sorted[i + 1].dateAdded}")
                    break // Only report first major gap
                }
            }
        }
        
        // Check for missing corroborating evidence
        if (evidence.size < 3) {
            gaps.add("Additional corroborating evidence (minimum 3 pieces recommended)")
        }
        
        // Check for missing witness or third-party evidence
        val hasThirdParty = evidence.any { 
            it.metadata.containsKey("third_party") || it.metadata.containsKey("witness")
        }
        if (!hasThirdParty && evidence.size > 2) {
            gaps.add("Third-party or witness corroboration")
        }
        
        return gaps
    }

    override suspend fun scoreOutcomeProbability(evidence: List<Evidence>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        // Calculate base probability based on evidence strength
        var probabilityScore = 0.0
        val strengthFactors = mutableListOf<String>()
        val weaknessFactors = mutableListOf<String>()
        
        // Factor 1: Evidence quantity
        when {
            evidence.size >= 10 -> {
                probabilityScore += 0.25
                strengthFactors.add("Strong evidence quantity (${evidence.size} pieces)")
            }
            evidence.size >= 5 -> {
                probabilityScore += 0.15
                strengthFactors.add("Adequate evidence quantity")
            }
            evidence.size >= 3 -> {
                probabilityScore += 0.10
                weaknessFactors.add("Minimal evidence quantity")
            }
            else -> {
                weaknessFactors.add("Insufficient evidence quantity")
            }
        }
        
        // Factor 2: Evidence diversity
        val typeCount = evidence.map { it.type }.distinct().size
        when {
            typeCount >= 4 -> {
                probabilityScore += 0.20
                strengthFactors.add("Diverse evidence types ($typeCount categories)")
            }
            typeCount >= 2 -> {
                probabilityScore += 0.10
                strengthFactors.add("Multiple evidence types")
            }
            else -> {
                weaknessFactors.add("Single evidence type only")
            }
        }
        
        // Factor 3: Metadata completeness
        val completeMetadata = evidence.count { it.metadata.size >= 3 }
        val metadataRatio = completeMetadata.toDouble() / evidence.size
        when {
            metadataRatio >= 0.8 -> {
                probabilityScore += 0.20
                strengthFactors.add("Excellent metadata documentation")
            }
            metadataRatio >= 0.5 -> {
                probabilityScore += 0.10
                strengthFactors.add("Adequate metadata")
            }
            else -> {
                weaknessFactors.add("Poor metadata documentation")
            }
        }
        
        // Factor 4: Verification indicators
        val hasHashes = evidence.count { it.metadata.containsKey("hash") }
        if (hasHashes > evidence.size / 2) {
            probabilityScore += 0.15
            strengthFactors.add("Cryptographic verification available")
        }
        
        // Factor 5: Recency
        val newestEvidence = evidence.maxByOrNull { it.dateAdded }
        if (newestEvidence != null) {
            val daysSinceNewest = (Date().time - newestEvidence.dateAdded.time) / (1000.0 * 60 * 60 * 24)
            if (daysSinceNewest < 365) {
                probabilityScore += 0.10
                strengthFactors.add("Evidence is recent")
            }
        }
        
        val favorableOutcome = probabilityScore.coerceIn(0.0, 1.0)
        
        result["favorable_outcome_probability"] = favorableOutcome
        result["strength_factors"] = strengthFactors
        result["weakness_factors"] = weaknessFactors
        result["description"] = when {
            favorableOutcome >= 0.7 -> "STRONG case - ${(favorableOutcome * 100).toInt()}% probability of favorable outcome"
            favorableOutcome >= 0.4 -> "MODERATE case - ${(favorableOutcome * 100).toInt()}% probability, strengthen with additional evidence"
            else -> "WEAK case - ${(favorableOutcome * 100).toInt()}% probability, significant additional evidence needed"
        }
        result["confidence"] = 0.70
        
        result
    }
}
