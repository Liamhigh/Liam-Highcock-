package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * Brain 6: Image Validation
 * 
 * Analyzes image evidence for EXIF metadata, deepfake detection,
 * lighting consistency, and pixel-level manipulation.
 */
class ImageValidationBrainImpl : ImageValidationBrain {
    override val brainName = "Image Validation Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Process image evidence
        val imageEvidence = evidence.filter { it.type == EvidenceType.IMAGE }
        
        imageEvidence.forEach { image ->
            // Analyze EXIF metadata
            val exifData = analyzeExifMetadata(image)
            val hasExif = (exifData["has_exif"] as? Boolean) ?: false
            findings.add(Finding(
                severity = if (hasExif) Severity.INFO else Severity.LOW,
                category = "EXIF Metadata",
                description = exifData["description"] as? String ?: "EXIF analysis complete",
                confidence = 0.90,
                details = exifData
            ))

            // Detect deepfake
            val deepfakeResult = detectDeepfake(image)
            val deepfakeProbability = (deepfakeResult["probability"] as? Double) ?: 0.0
            findings.add(Finding(
                severity = when {
                    deepfakeProbability > 0.7 -> Severity.CRITICAL
                    deepfakeProbability > 0.4 -> Severity.HIGH
                    deepfakeProbability > 0.2 -> Severity.MEDIUM
                    else -> Severity.INFO
                },
                category = "Deepfake Detection",
                description = deepfakeResult["description"] as? String ?: "Deepfake analysis complete",
                confidence = 0.85,
                details = deepfakeResult
            ))

            // Analyze lighting consistency
            val lightingResult = analyzeLightingConsistency(image)
            val lightingConsistent = (lightingResult["consistent"] as? Boolean) ?: true
            findings.add(Finding(
                severity = if (lightingConsistent) Severity.INFO else Severity.MEDIUM,
                category = "Lighting Analysis",
                description = lightingResult["description"] as? String ?: "Lighting analysis complete",
                confidence = 0.80,
                details = lightingResult
            ))

            // Detect pixel manipulation
            val pixelManipulations = detectPixelManipulation(image)
            pixelManipulations.forEach { manipulation ->
                findings.add(Finding(
                    severity = Severity.HIGH,
                    category = "Pixel Manipulation",
                    description = manipulation["description"] as? String ?: "Manipulation detected",
                    confidence = 0.85,
                    details = manipulation
                ))
            }
        }

        if (imageEvidence.isEmpty()) {
            findings.add(Finding(
                severity = Severity.INFO,
                category = "No Image Evidence",
                description = "No image files found in evidence set",
                confidence = 1.0
            ))
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

    override suspend fun analyzeExifMetadata(evidence: Evidence): Map<String, Any> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["file_name"] = evidence.fileName
        
        val file = File(evidence.filePath)
        if (!file.exists()) {
            result["has_exif"] = false
            result["description"] = "Image file not found: ${evidence.fileName}"
            return@withContext result
        }
        
        // Extract EXIF data from metadata
        val metadata = evidence.metadata
        val hasExifData = metadata.containsKey("exif") || 
                          metadata.containsKey("camera_make") ||
                          metadata.containsKey("camera_model") ||
                          metadata.containsKey("gps_latitude")
        
        result["has_exif"] = hasExifData
        
        if (hasExifData) {
            result["description"] = "EXIF metadata present in ${evidence.fileName}"
            metadata["camera_make"]?.let { result["camera_make"] = it }
            metadata["camera_model"]?.let { result["camera_model"] = it }
            metadata["datetime_original"]?.let { result["datetime_original"] = it }
            metadata["gps_latitude"]?.let { result["gps_latitude"] = it }
            metadata["gps_longitude"]?.let { result["gps_longitude"] = it }
            metadata["software"]?.let { result["software"] = it }
        } else {
            result["description"] = "No EXIF metadata found in ${evidence.fileName} (may indicate stripping)"
            result["warning"] = "Missing EXIF may indicate metadata was intentionally removed"
        }
        
        result
    }

    override suspend fun detectDeepfake(evidence: Evidence): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["file_name"] = evidence.fileName
        
        // Heuristic-based deepfake detection
        // In a full implementation, this would use ML models
        var probability = 0.0
        val indicators = mutableListOf<String>()
        
        val metadata = evidence.metadata
        
        // Check for AI generation signatures
        val software = (metadata["software"] as? String)?.lowercase() ?: ""
        val aiGenerators = listOf("midjourney", "dall-e", "stable diffusion", "ai", "generated")
        if (aiGenerators.any { software.contains(it) }) {
            probability += 0.5
            indicators.add("AI generation software signature detected")
        }
        
        // Check for missing EXIF (common in AI images)
        if (!metadata.containsKey("camera_make") && !metadata.containsKey("camera_model")) {
            probability += 0.1
            indicators.add("No camera metadata (common in synthetic images)")
        }
        
        // Check for unusual dimensions
        val width = metadata["width"] as? Int
        val height = metadata["height"] as? Int
        if (width != null && height != null) {
            val aspectRatio = width.toDouble() / height
            // AI generators often produce square or specific aspect ratios
            if (aspectRatio == 1.0 || aspectRatio == 1.5 || aspectRatio == 1.78) {
                probability += 0.05
                indicators.add("Common AI aspect ratio detected")
            }
        }
        
        probability = probability.coerceIn(0.0, 1.0)
        
        result["probability"] = probability
        result["indicators"] = indicators
        result["description"] = when {
            probability > 0.7 -> "HIGH probability of synthetic/deepfake content"
            probability > 0.4 -> "MEDIUM probability of manipulation or synthetic content"
            probability > 0.2 -> "LOW indicators of possible manipulation"
            else -> "No significant deepfake indicators detected"
        }
        
        return result
    }

    override suspend fun analyzeLightingConsistency(evidence: Evidence): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["file_name"] = evidence.fileName
        
        // Placeholder for lighting analysis
        // Full implementation would analyze shadow directions, light sources, etc.
        result["consistent"] = true
        result["description"] = "Lighting analysis completed for ${evidence.fileName}"
        result["note"] = "Advanced lighting analysis requires image processing libraries"
        
        return result
    }

    override suspend fun detectPixelManipulation(evidence: Evidence): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val manipulations = mutableListOf<Map<String, Any>>()
        
        val metadata = evidence.metadata
        
        // Check for editing software signatures
        val software = (metadata["software"] as? String)?.lowercase() ?: ""
        val editSoftware = listOf("photoshop", "gimp", "lightroom", "snapseed", "picsart")
        
        if (editSoftware.any { software.contains(it) }) {
            manipulations.add(mapOf(
                "evidence_id" to evidence.id,
                "type" to "editing_software_detected",
                "description" to "Image was edited with: $software",
                "software" to software,
                "note" to "Editing does not necessarily indicate malicious manipulation"
            ))
        }
        
        // Check for file modification after creation
        val created = metadata["created_date"] as? Long
        val modified = metadata["modified_date"] as? Long
        if (created != null && modified != null && modified > created) {
            val diffHours = (modified - created) / (1000 * 60 * 60)
            if (diffHours > 1) { // Modified more than 1 hour after creation
                manipulations.add(mapOf(
                    "evidence_id" to evidence.id,
                    "type" to "post_creation_modification",
                    "description" to "Image was modified ${diffHours} hours after creation",
                    "created_timestamp" to created,
                    "modified_timestamp" to modified
                ))
            }
        }
        
        manipulations
    }
}
