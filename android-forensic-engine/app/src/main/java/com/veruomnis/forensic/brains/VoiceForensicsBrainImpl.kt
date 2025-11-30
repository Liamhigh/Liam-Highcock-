package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * Brain 5: Voice Forensics Analysis
 * "Speech reveals identity, intent, and deception."
 */
class VoiceForensicsBrainImpl : VoiceForensicsBrain {
    override val brainName = "Voice Forensics Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        val audioEvidence = evidence.filter { it.type == EvidenceType.AUDIO }

        if (audioEvidence.isEmpty()) {
            findings.add(Finding(
                severity = Severity.INFO,
                category = "Audio Analysis",
                description = "No audio evidence provided for voice forensics analysis",
                confidence = 1.0,
                details = mapOf("audio_count" to 0)
            ))
        } else {
            audioEvidence.forEach { audio ->
                // Verify audio authenticity
                val authenticity = verifyAudioAuthenticity(audio)
                val authenticityScore = authenticity["authenticity_score"] as? Double ?: 0.0
                findings.add(Finding(
                    severity = if (authenticityScore > 0.8) Severity.INFO else Severity.MEDIUM,
                    category = "Audio Authenticity",
                    description = "Audio file ${audio.fileName} authenticity score: ${(authenticityScore * 100).toInt()}%",
                    confidence = 0.85,
                    details = authenticity
                ))

                // Detect emotional stress markers
                val stressMarkers = detectEmotionalStress(audio)
                stressMarkers.forEach { marker ->
                    findings.add(Finding(
                        severity = Severity.MEDIUM,
                        category = "Emotional Stress",
                        description = marker["description"] as? String ?: "Stress marker detected",
                        confidence = 0.75,
                        details = marker
                    ))
                }

                // Analyze voiceprint consistency
                val voiceprintAnalysis = analyzeVoiceprintConsistency(audio)
                val consistency = voiceprintAnalysis["consistency_score"] as? Double ?: 1.0
                findings.add(Finding(
                    severity = if (consistency > 0.7) Severity.INFO else Severity.HIGH,
                    category = "Voiceprint Consistency",
                    description = "Voiceprint consistency: ${(consistency * 100).toInt()}%",
                    confidence = 0.80,
                    details = voiceprintAnalysis
                ))

                // Detect edit artifacts
                val editArtifacts = detectEditArtifacts(audio)
                editArtifacts.forEach { artifact ->
                    findings.add(Finding(
                        severity = Severity.HIGH,
                        category = "Audio Edit Detected",
                        description = artifact["description"] as? String ?: "Edit artifact detected",
                        confidence = 0.88,
                        details = artifact
                    ))
                }
            }
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

    override suspend fun verifyAudioAuthenticity(evidence: Evidence): Map<String, Any> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["evidence_name"] = evidence.fileName

        val file = File(evidence.filePath)
        if (!file.exists()) {
            result["authenticity_score"] = 0.0
            result["error"] = "Audio file not found"
            return@withContext result
        }

        // Analyze audio properties
        result["file_size"] = file.length()
        result["mime_type"] = evidence.mimeType

        // Check for expected audio metadata
        var score = 1.0

        // Check for duration metadata
        val duration = evidence.metadata["duration"] as? Long
        if (duration == null) {
            score -= 0.1
            result["missing_duration"] = true
        } else {
            result["duration_ms"] = duration
        }

        // Check for bitrate metadata
        val bitrate = evidence.metadata["bitrate"] as? Int
        if (bitrate == null) {
            score -= 0.05
        } else {
            result["bitrate"] = bitrate
            // Check for unusual bitrate
            if (bitrate < 32000 || bitrate > 320000) {
                score -= 0.1
                result["unusual_bitrate"] = true
            }
        }

        // Check for sample rate
        val sampleRate = evidence.metadata["sample_rate"] as? Int
        if (sampleRate != null) {
            result["sample_rate"] = sampleRate
            // Standard sample rates
            if (sampleRate !in listOf(8000, 11025, 22050, 44100, 48000, 96000)) {
                score -= 0.1
                result["non_standard_sample_rate"] = true
            }
        }

        // Check file size vs duration ratio for potential compression artifacts
        if (duration != null && duration > 0) {
            val expectedMinSize = duration * 1000 // Very rough minimum
            if (file.length() < expectedMinSize / 100) {
                score -= 0.2
                result["suspicious_compression"] = true
            }
        }

        result["authenticity_score"] = score.coerceIn(0.0, 1.0)
        result
    }

    override suspend fun detectEmotionalStress(evidence: Evidence): List<Map<String, Any>> {
        val stressMarkers = mutableListOf<Map<String, Any>>()

        // Analyze audio for stress indicators
        // In a production system, this would use audio analysis libraries
        
        val metadata = evidence.metadata

        // Check for voice pitch variations (if available in metadata)
        val avgPitch = metadata["average_pitch"] as? Double
        val pitchVariance = metadata["pitch_variance"] as? Double

        if (avgPitch != null && pitchVariance != null) {
            // High pitch variance can indicate stress
            if (pitchVariance > 50.0) {
                stressMarkers.add(mapOf(
                    "marker_type" to "pitch_variance",
                    "evidence_id" to evidence.id,
                    "description" to "High pitch variance detected (${pitchVariance.toInt()} Hz), may indicate emotional stress",
                    "pitch_variance" to pitchVariance,
                    "confidence" to 0.70
                ))
            }
        }

        // Check for unusual speaking rate
        val wordsPerMinute = metadata["words_per_minute"] as? Int
        if (wordsPerMinute != null) {
            when {
                wordsPerMinute > 180 -> stressMarkers.add(mapOf(
                    "marker_type" to "rapid_speech",
                    "evidence_id" to evidence.id,
                    "description" to "Rapid speech rate ($wordsPerMinute WPM) may indicate anxiety or stress",
                    "words_per_minute" to wordsPerMinute,
                    "confidence" to 0.65
                ))
                wordsPerMinute < 100 -> stressMarkers.add(mapOf(
                    "marker_type" to "slow_speech",
                    "evidence_id" to evidence.id,
                    "description" to "Slow speech rate ($wordsPerMinute WPM) may indicate hesitation or deception",
                    "words_per_minute" to wordsPerMinute,
                    "confidence" to 0.60
                ))
            }
        }

        // Check for silence patterns
        val silencePercentage = metadata["silence_percentage"] as? Double
        if (silencePercentage != null && silencePercentage > 30.0) {
            stressMarkers.add(mapOf(
                "marker_type" to "excessive_pauses",
                "evidence_id" to evidence.id,
                "description" to "Excessive pauses detected (${silencePercentage.toInt()}% silence), may indicate careful speech or stress",
                "silence_percentage" to silencePercentage,
                "confidence" to 0.55
            ))
        }

        return stressMarkers
    }

    override suspend fun analyzeVoiceprintConsistency(evidence: Evidence): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["evidence_name"] = evidence.fileName

        // In production, this would use voice fingerprinting algorithms
        val metadata = evidence.metadata

        var consistencyScore = 1.0

        // Check for multiple speakers
        val speakerCount = metadata["speaker_count"] as? Int
        if (speakerCount != null) {
            result["speaker_count"] = speakerCount
            if (speakerCount > 1) {
                result["multiple_speakers"] = true
                // Not necessarily inconsistent, but note it
            }
        }

        // Check for voice frequency stability
        val freqStability = metadata["frequency_stability"] as? Double
        if (freqStability != null) {
            result["frequency_stability"] = freqStability
            if (freqStability < 0.7) {
                consistencyScore -= 0.2
                result["unstable_frequency"] = true
            }
        }

        // Check for voice pattern breaks
        val patternBreaks = metadata["pattern_breaks"] as? Int
        if (patternBreaks != null && patternBreaks > 0) {
            result["pattern_breaks"] = patternBreaks
            consistencyScore -= patternBreaks * 0.05
        }

        result["consistency_score"] = consistencyScore.coerceIn(0.0, 1.0)
        result
    }

    override suspend fun detectEditArtifacts(evidence: Evidence): List<Map<String, Any>> {
        val artifacts = mutableListOf<Map<String, Any>>()

        val file = File(evidence.filePath)
        if (!file.exists()) return artifacts

        val metadata = evidence.metadata

        // Check for discontinuities in audio stream
        val discontinuities = metadata["audio_discontinuities"] as? Int
        if (discontinuities != null && discontinuities > 0) {
            artifacts.add(mapOf(
                "artifact_type" to "audio_discontinuity",
                "evidence_id" to evidence.id,
                "description" to "Detected $discontinuities audio discontinuities, possible splice points",
                "count" to discontinuities,
                "confidence" to 0.80
            ))
        }

        // Check for encoding mismatches
        val encodingMismatch = metadata["encoding_mismatch"] as? Boolean
        if (encodingMismatch == true) {
            artifacts.add(mapOf(
                "artifact_type" to "encoding_mismatch",
                "evidence_id" to evidence.id,
                "description" to "Multiple encoding formats detected within file, indicates possible editing",
                "confidence" to 0.85
            ))
        }

        // Check for timestamp gaps in audio frames
        val frameGaps = metadata["frame_gaps"] as? Int
        if (frameGaps != null && frameGaps > 0) {
            artifacts.add(mapOf(
                "artifact_type" to "frame_gaps",
                "evidence_id" to evidence.id,
                "description" to "Detected $frameGaps gaps in audio frames, possible content removal",
                "count" to frameGaps,
                "confidence" to 0.75
            ))
        }

        // Check for noise floor inconsistencies
        val noiseFloorVariance = metadata["noise_floor_variance"] as? Double
        if (noiseFloorVariance != null && noiseFloorVariance > 0.3) {
            artifacts.add(mapOf(
                "artifact_type" to "noise_inconsistency",
                "evidence_id" to evidence.id,
                "description" to "Background noise level varies significantly, indicating possible editing",
                "variance" to noiseFloorVariance,
                "confidence" to 0.70
            ))
        }

        return artifacts
    }
}
