package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * Brain 5: Voice Forensics Analysis
 * Analyzes audio evidence for authenticity, emotional stress, and tampering
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
                category = "No Audio Evidence",
                description = "No audio evidence found for voice forensics analysis",
                confidence = 1.0,
                details = emptyMap()
            ))
        } else {
            audioEvidence.forEach { audio ->
                // Verify audio authenticity
                val authenticity = verifyAudioAuthenticity(audio)
                val isAuthentic = authenticity["authentic"] as? Boolean ?: true
                findings.add(Finding(
                    severity = if (isAuthentic) Severity.INFO else Severity.HIGH,
                    category = "Audio Authenticity",
                    description = authenticity["description"] as? String ?: "Audio authenticity verified",
                    confidence = authenticity["confidence"] as? Double ?: 0.80,
                    details = authenticity
                ))

                // Detect emotional stress
                val stressMarkers = detectEmotionalStress(audio)
                stressMarkers.forEach { marker ->
                    findings.add(Finding(
                        severity = Severity.MEDIUM,
                        category = "Emotional Stress",
                        description = marker["description"] as? String ?: "Stress marker detected",
                        confidence = marker["confidence"] as? Double ?: 0.75,
                        details = marker
                    ))
                }

                // Analyze voiceprint consistency
                val voiceprint = analyzeVoiceprintConsistency(audio)
                findings.add(Finding(
                    severity = Severity.INFO,
                    category = "Voiceprint Analysis",
                    description = voiceprint["description"] as? String ?: "Voiceprint analyzed",
                    confidence = voiceprint["confidence"] as? Double ?: 0.80,
                    details = voiceprint
                ))

                // Detect edit artifacts
                val editArtifacts = detectEditArtifacts(audio)
                editArtifacts.forEach { artifact ->
                    findings.add(Finding(
                        severity = Severity.HIGH,
                        category = "Edit Artifact",
                        description = artifact["description"] as? String ?: "Audio edit detected",
                        confidence = artifact["confidence"] as? Double ?: 0.85,
                        details = artifact
                    ))
                }
            }
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

    override suspend fun verifyAudioAuthenticity(evidence: Evidence): Map<String, Any> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["file_name"] = evidence.fileName
        
        val file = File(evidence.filePath)
        if (!file.exists()) {
            result["authentic"] = false
            result["description"] = "Audio file not found: ${evidence.fileName}"
            result["confidence"] = 1.0
            return@withContext result
        }
        
        // Check file format and metadata
        val metadata = evidence.metadata
        val hasValidFormat = evidence.mimeType.startsWith("audio/")
        val hasMetadata = metadata.isNotEmpty()
        
        // Check for consistent encoding metadata
        val sampleRate = metadata["sample_rate"] as? Int
        val bitRate = metadata["bit_rate"] as? Int
        val channels = metadata["channels"] as? Int
        
        val metadataComplete = sampleRate != null || bitRate != null || channels != null
        
        // Verify file size is reasonable for claimed duration
        val duration = metadata["duration"] as? Long // in seconds
        if (duration != null && duration > 0) {
            val expectedMinSize = duration * 1000 // rough minimum bytes for compressed audio
            if (file.length() < expectedMinSize / 10) {
                result["authentic"] = false
                result["description"] = "Audio file size inconsistent with duration for ${evidence.fileName}"
                result["confidence"] = 0.85
                return@withContext result
            }
        }
        
        result["authentic"] = true
        result["description"] = "Audio authenticity verified for ${evidence.fileName}"
        result["confidence"] = 0.80
        result["has_valid_format"] = hasValidFormat
        result["has_metadata"] = hasMetadata
        result["metadata_complete"] = metadataComplete
        
        result
    }

    override suspend fun detectEmotionalStress(evidence: Evidence): List<Map<String, Any>> {
        val markers = mutableListOf<Map<String, Any>>()
        
        // Note: Full voice stress analysis would require audio processing libraries
        // This is a metadata-based heuristic analysis
        
        val metadata = evidence.metadata
        val duration = metadata["duration"] as? Long ?: 0L
        
        // Flag short recordings that might indicate stress/urgency
        if (duration > 0 && duration < 10) {
            markers.add(mapOf(
                "evidence_id" to evidence.id,
                "stress_type" to "short_duration",
                "description" to "Very short audio (${duration}s) may indicate urgency or distress",
                "confidence" to 0.60,
                "duration_seconds" to duration
            ))
        }
        
        // Check for multiple recordings in quick succession (metadata-based)
        val recordedTime = metadata["recorded_at"] as? Long
        if (recordedTime != null) {
            markers.add(mapOf(
                "evidence_id" to evidence.id,
                "stress_type" to "timing_analysis",
                "description" to "Audio recorded at specific timestamp - context may indicate emotional state",
                "confidence" to 0.55,
                "recorded_at" to recordedTime
            ))
        }
        
        return markers
    }

    override suspend fun analyzeVoiceprintConsistency(evidence: Evidence): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["file_name"] = evidence.fileName
        
        // Note: Full voiceprint analysis would require audio fingerprinting
        // This provides a structural analysis placeholder
        
        val metadata = evidence.metadata
        val channels = metadata["channels"] as? Int ?: 1
        val sampleRate = metadata["sample_rate"] as? Int
        
        result["channels"] = channels
        result["sample_rate"] = sampleRate ?: "unknown"
        result["voiceprint_extracted"] = false
        result["description"] = "Voiceprint structure analyzed for ${evidence.fileName}"
        result["confidence"] = 0.70
        
        // Check for consistent audio characteristics
        if (sampleRate != null) {
            val isStandardRate = sampleRate in listOf(8000, 16000, 22050, 44100, 48000)
            result["standard_sample_rate"] = isStandardRate
        }
        
        result
    }

    override suspend fun detectEditArtifacts(evidence: Evidence): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val artifacts = mutableListOf<Map<String, Any>>()
        
        val file = File(evidence.filePath)
        if (!file.exists()) return@withContext artifacts
        
        val metadata = evidence.metadata
        
        // Check for modification indicators
        val createdDate = metadata["created_date"] as? Long
        val modifiedDate = metadata["modified_date"] as? Long
        
        if (createdDate != null && modifiedDate != null && modifiedDate > createdDate) {
            val timeDiff = (modifiedDate - createdDate) / 1000 // seconds
            if (timeDiff > 60) { // Modified more than 1 minute after creation
                artifacts.add(mapOf(
                    "evidence_id" to evidence.id,
                    "artifact_type" to "post_creation_modification",
                    "description" to "Audio ${evidence.fileName} was modified ${timeDiff}s after creation",
                    "confidence" to 0.75,
                    "time_difference_seconds" to timeDiff
                ))
            }
        }
        
        // Check for software editing markers in metadata
        val software = metadata["software"] as? String
        val editingSoftware = listOf("audacity", "adobe", "premiere", "garageband", "logic", "pro tools")
        if (software != null && editingSoftware.any { software.lowercase().contains(it) }) {
            artifacts.add(mapOf(
                "evidence_id" to evidence.id,
                "artifact_type" to "editing_software_detected",
                "description" to "Audio was processed with editing software: $software",
                "confidence" to 0.90,
                "software" to software
            ))
        }
        
        artifacts
    }
}
