package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 2: Behavioral Diagnostics and Pattern Recognition
 */
class BehavioralBrainImpl : BehavioralBrain {
    override val brainName = "Behavioral Diagnostics Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        val bpm = analyzeBehavioralProbability(evidence)
        findings.add(Finding(
            severity = if (bpm > 0.7) Severity.HIGH else Severity.INFO,
            category = "Behavioral Probability",
            description = "Behavioral Probability Model score: ${(bpm * 100).toInt()}%",
            confidence = 0.80,
            details = mapOf("bpm_score" to bpm)
        ))

        val microEmotions = detectMicroEmotions(evidence)
        microEmotions.forEach { emotion ->
            findings.add(Finding(
                severity = Severity.MEDIUM,
                category = "Micro-Emotion",
                description = emotion["description"] as? String ?: "Emotion detected",
                confidence = 0.75,
                details = emotion
            ))
        }

        val intentPatterns = recognizeIntentPatterns(evidence)
        intentPatterns.forEach { pattern ->
            findings.add(Finding(
                severity = Severity.MEDIUM,
                category = "Intent Pattern",
                description = pattern["description"] as? String ?: "Intent pattern recognized",
                confidence = 0.78,
                details = pattern
            ))
        }

        val manipulationIndicators = detectManipulation(evidence)
        manipulationIndicators.forEach { indicator ->
            findings.add(Finding(
                severity = Severity.HIGH,
                category = "Manipulation Detected",
                description = indicator["description"] as? String ?: "Manipulation indicator found",
                confidence = 0.82,
                details = indicator
            ))
        }

        val processingTime = System.currentTimeMillis() - startTime
        BrainAnalysisResult(
            brainName = brainName,
            confidence = if (findings.isEmpty()) 0.75 else findings.maxOfOrNull { it.confidence } ?: 0.0,
            findings = findings,
            timestamp = Date(),
            processingTimeMs = processingTime
        )
    }

    override suspend fun analyzeBehavioralProbability(evidence: List<Evidence>): Double {
        var probability = 0.0
        
        // Analyze patterns in evidence submission
        val timeGaps = if (evidence.size > 1) {
            evidence.sortedBy { it.dateAdded }
                .zipWithNext { a, b -> b.dateAdded.time - a.dateAdded.time }
        } else emptyList()
        
        // Suspicious rapid-fire submissions
        val rapidSubmissions = timeGaps.count { it < 60000 } // Less than 1 minute
        if (rapidSubmissions > evidence.size / 3) {
            probability += 0.3
        }
        
        // Check for modification patterns
        val hasModifications = evidence.count { 
            it.metadata.containsKey("modified") 
        }
        if (hasModifications > evidence.size / 2) {
            probability += 0.2
        }
        
        return probability.coerceIn(0.0, 1.0)
    }

    override suspend fun detectMicroEmotions(evidence: List<Evidence>): List<Map<String, Any>> {
        val emotions = mutableListOf<Map<String, Any>>()
        
        evidence.filter { it.type == EvidenceType.AUDIO || it.type == EvidenceType.VIDEO }.forEach { ev ->
            // Placeholder for actual emotion detection
            emotions.add(mapOf(
                "evidence_id" to ev.id,
                "emotion_type" to "stress_detected",
                "description" to "Audio/Video evidence may contain emotional stress markers",
                "confidence" to 0.70
            ))
        }
        
        return emotions
    }

    override suspend fun recognizeIntentPatterns(evidence: List<Evidence>): List<Map<String, Any>> {
        val patterns = mutableListOf<Map<String, Any>>()
        
        // Check for evidence type clustering
        val typeGroups = evidence.groupBy { it.type }
        typeGroups.forEach { (type, items) ->
            if (items.size > evidence.size * 0.6) {
                patterns.add(mapOf(
                    "pattern_type" to "evidence_clustering",
                    "description" to "Predominance of ${type.name} evidence (${items.size}/${evidence.size})",
                    "dominant_type" to type.name,
                    "percentage" to (items.size.toDouble() / evidence.size * 100)
                ))
            }
        }
        
        return patterns
    }

    override suspend fun detectManipulation(evidence: List<Evidence>): List<Map<String, Any>> {
        val indicators = mutableListOf<Map<String, Any>>()
        
        evidence.forEach { ev ->
            // Check for metadata manipulation indicators
            val metadata = ev.metadata
            
            // Check for timestamp anomalies
            val created = metadata["created_date"] as? Long
            val modified = metadata["modified_date"] as? Long
            
            if (created != null && modified != null && modified < created) {
                indicators.add(mapOf(
                    "indicator_type" to "timestamp_anomaly",
                    "description" to "File ${ev.fileName} has modification date before creation date",
                    "evidence_id" to ev.id,
                    "severity" to "high"
                ))
            }
            
            // Check for suspicious file size changes
            if (metadata.containsKey("original_size")) {
                val originalSize = metadata["original_size"] as? Long
                if (originalSize != null && originalSize != ev.fileSize) {
                    indicators.add(mapOf(
                        "indicator_type" to "size_mismatch",
                        "description" to "File ${ev.fileName} size differs from original",
                        "evidence_id" to ev.id,
                        "original_size" to originalSize,
                        "current_size" to ev.fileSize
                    ))
                }
            }
        }
        
        return indicators
    }
}
