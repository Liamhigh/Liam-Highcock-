package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 1: Contradiction Detection and Truth Stability Index
 */
class ContradictionBrainImpl : ContradictionBrain {
    override val brainName = "Contradiction Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Calculate Truth Stability Index
        val tsi = calculateTruthStabilityIndex(evidence)
        findings.add(Finding(
            severity = if (tsi > 80) Severity.INFO else if (tsi > 60) Severity.LOW else Severity.HIGH,
            category = "Truth Stability",
            description = "Truth Stability Index: $tsi/100",
            confidence = 0.95,
            details = mapOf("tsi" to tsi)
        ))

        // Detect contradictions
        val contradictions = detectContradictions(evidence)
        contradictions.forEach { contradiction ->
            findings.add(Finding(
                severity = Severity.HIGH,
                category = "Contradiction Detected",
                description = contradiction["description"] as? String ?: "Contradiction found",
                confidence = 0.85,
                details = contradiction
            ))
        }

        // Timeline conflicts
        val timelineConflicts = analyzeTimelineConflicts(evidence)
        timelineConflicts.forEach { conflict ->
            findings.add(Finding(
                severity = Severity.MEDIUM,
                category = "Timeline Conflict",
                description = conflict["description"] as? String ?: "Timeline inconsistency",
                confidence = 0.80,
                details = conflict
            ))
        }

        // Omission patterns
        val omissions = detectOmissionPatterns(evidence)
        omissions.forEach { omission ->
            findings.add(Finding(
                severity = Severity.MEDIUM,
                category = "Omission Pattern",
                description = omission["description"] as? String ?: "Potential omission detected",
                confidence = 0.75,
                details = omission
            ))
        }

        val processingTime = System.currentTimeMillis() - startTime
        BrainAnalysisResult(
            brainName = brainName,
            confidence = if (findings.isEmpty()) 0.95 else findings.maxOfOrNull { it.confidence } ?: 0.0,
            findings = findings,
            timestamp = Date(),
            processingTimeMs = processingTime
        )
    }

    override suspend fun calculateTruthStabilityIndex(evidence: List<Evidence>): Double {
        if (evidence.isEmpty()) return 100.0
        
        // Calculate TSI based on evidence consistency, metadata completeness, and cross-validation
        var score = 100.0
        
        // Penalize for missing metadata
        val metadataCompleteness = evidence.count { it.metadata.isNotEmpty() }.toDouble() / evidence.size
        score -= (1.0 - metadataCompleteness) * 15
        
        // Check for file integrity indicators
        val hasHashData = evidence.count { it.metadata.containsKey("hash") }.toDouble() / evidence.size
        score -= (1.0 - hasHashData) * 10
        
        // Temporal consistency check
        if (evidence.size > 1) {
            val sortedByDate = evidence.sortedBy { it.dateAdded }
            val timeGaps = sortedByDate.zipWithNext { a, b ->
                (b.dateAdded.time - a.dateAdded.time) / (1000 * 60 * 60) // hours
            }
            val unusualGaps = timeGaps.count { it > 168 } // More than a week
            score -= unusualGaps * 5.0
        }
        
        return score.coerceIn(0.0, 100.0)
    }

    override suspend fun detectContradictions(evidence: List<Evidence>): List<Map<String, Any>> {
        val contradictions = mutableListOf<Map<String, Any>>()
        
        // Cross-check metadata for contradictions
        if (evidence.size >= 2) {
            for (i in evidence.indices) {
                for (j in i + 1 until evidence.size) {
                    val e1 = evidence[i]
                    val e2 = evidence[j]
                    
                    // Check for same file with different metadata
                    if (e1.fileName == e2.fileName && e1.metadata != e2.metadata) {
                        contradictions.add(mapOf(
                            "type" to "metadata_mismatch",
                            "description" to "Same file '${e1.fileName}' has conflicting metadata",
                            "evidence_ids" to listOf(e1.id, e2.id),
                            "severity" to "high"
                        ))
                    }
                }
            }
        }
        
        return contradictions
    }

    override suspend fun analyzeTimelineConflicts(evidence: List<Evidence>): List<Map<String, Any>> {
        val conflicts = mutableListOf<Map<String, Any>>()
        
        // Check for temporal impossibilities
        val sortedEvidence = evidence.sortedBy { it.dateAdded }
        for (i in 0 until sortedEvidence.size - 1) {
            val current = sortedEvidence[i]
            val next = sortedEvidence[i + 1]
            
            // Check metadata dates vs file dates
            val metadataDate = current.metadata["created_date"] as? Long
            if (metadataDate != null && metadataDate > next.dateAdded.time) {
                conflicts.add(mapOf(
                    "type" to "temporal_impossibility",
                    "description" to "Evidence ${current.fileName} has creation date after subsequent evidence",
                    "evidence_id" to current.id
                ))
            }
        }
        
        return conflicts
    }

    override suspend fun detectOmissionPatterns(evidence: List<Evidence>): List<Map<String, Any>> {
        val patterns = mutableListOf<Map<String, Any>>()
        
        // Check for sequential gaps in numbered files
        val numberedFiles = evidence.filter { it.fileName.contains(Regex("\\d+")) }
            .sortedBy { it.fileName }
        
        if (numberedFiles.size > 2) {
            val numbers = numberedFiles.mapNotNull { 
                Regex("\\d+").find(it.fileName)?.value?.toIntOrNull() 
            }
            
            if (numbers.isNotEmpty()) {
                val min = numbers.minOrNull() ?: 0
                val max = numbers.maxOrNull() ?: 0
                val expectedCount = max - min + 1
                
                if (numbers.size < expectedCount) {
                    patterns.add(mapOf(
                        "type" to "sequential_gap",
                        "description" to "Missing ${expectedCount - numbers.size} files in sequence",
                        "expected_count" to expectedCount,
                        "actual_count" to numbers.size
                    ))
                }
            }
        }
        
        return patterns
    }
}
