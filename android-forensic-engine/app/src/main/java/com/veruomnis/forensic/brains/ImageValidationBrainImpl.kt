package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * Brain 6: Image Validation and Authenticity
 * "Images are evidence only if they survive scrutiny."
 */
class ImageValidationBrainImpl : ImageValidationBrain {
    override val brainName = "Image Validation Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        val imageEvidence = evidence.filter { it.type == EvidenceType.IMAGE }

        if (imageEvidence.isEmpty()) {
            findings.add(Finding(
                severity = Severity.INFO,
                category = "Image Analysis",
                description = "No image evidence provided for validation",
                confidence = 1.0,
                details = mapOf("image_count" to 0)
            ))
        } else {
            imageEvidence.forEach { image ->
                // Analyze EXIF metadata
                val exifData = analyzeExifMetadata(image)
                val hasValidExif = exifData["has_valid_exif"] as? Boolean ?: false
                findings.add(Finding(
                    severity = if (hasValidExif) Severity.INFO else Severity.MEDIUM,
                    category = "EXIF Metadata",
                    description = if (hasValidExif) 
                        "Valid EXIF metadata found for ${image.fileName}"
                    else 
                        "Missing or incomplete EXIF metadata for ${image.fileName}",
                    confidence = 0.90,
                    details = exifData
                ))

                // Detect deepfake/AI generation
                val deepfakeAnalysis = detectDeepfake(image)
                val deepfakeProbability = deepfakeAnalysis["deepfake_probability"] as? Double ?: 0.0
                findings.add(Finding(
                    severity = when {
                        deepfakeProbability > 0.7 -> Severity.CRITICAL
                        deepfakeProbability > 0.4 -> Severity.HIGH
                        deepfakeProbability > 0.2 -> Severity.MEDIUM
                        else -> Severity.INFO
                    },
                    category = "Deepfake Detection",
                    description = "Deepfake probability: ${(deepfakeProbability * 100).toInt()}%",
                    confidence = 0.80,
                    details = deepfakeAnalysis
                ))

                // Analyze lighting consistency
                val lightingAnalysis = analyzeLightingConsistency(image)
                val lightingConsistent = lightingAnalysis["is_consistent"] as? Boolean ?: true
                findings.add(Finding(
                    severity = if (lightingConsistent) Severity.INFO else Severity.HIGH,
                    category = "Lighting Analysis",
                    description = if (lightingConsistent)
                        "Lighting appears consistent in ${image.fileName}"
                    else
                        "Lighting inconsistencies detected in ${image.fileName}",
                    confidence = 0.75,
                    details = lightingAnalysis
                ))

                // Detect pixel manipulation
                val manipulations = detectPixelManipulation(image)
                manipulations.forEach { manipulation ->
                    findings.add(Finding(
                        severity = Severity.HIGH,
                        category = "Pixel Manipulation",
                        description = manipulation["description"] as? String ?: "Manipulation detected",
                        confidence = 0.85,
                        details = manipulation
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

    override suspend fun analyzeExifMetadata(evidence: Evidence): Map<String, Any> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["evidence_name"] = evidence.fileName

        val file = File(evidence.filePath)
        if (!file.exists()) {
            result["has_valid_exif"] = false
            result["error"] = "Image file not found"
            return@withContext result
        }

        // Check for EXIF data in metadata
        val metadata = evidence.metadata
        var validFields = 0
        val totalExpectedFields = 5

        // Camera make/model
        val cameraMake = metadata["camera_make"] as? String
        val cameraModel = metadata["camera_model"] as? String
        if (cameraMake != null || cameraModel != null) {
            validFields++
            result["camera_make"] = cameraMake ?: "Unknown"
            result["camera_model"] = cameraModel ?: "Unknown"
        }

        // Date/time original
        val dateOriginal = metadata["date_original"] as? Long
        if (dateOriginal != null) {
            validFields++
            result["date_original"] = dateOriginal
        }

        // GPS coordinates
        val latitude = metadata["latitude"] as? Double
        val longitude = metadata["longitude"] as? Double
        if (latitude != null && longitude != null) {
            validFields++
            result["gps_latitude"] = latitude
            result["gps_longitude"] = longitude
        }

        // Image dimensions
        val width = metadata["width"] as? Int
        val height = metadata["height"] as? Int
        if (width != null && height != null) {
            validFields++
            result["width"] = width
            result["height"] = height
        }

        // Software/processing info
        val software = metadata["software"] as? String
        if (software != null) {
            validFields++
            result["software"] = software
            
            // Flag if editing software detected
            val editingSoftware = listOf("photoshop", "gimp", "lightroom", "snapseed", "facetune")
            if (editingSoftware.any { software.lowercase().contains(it) }) {
                result["editing_software_detected"] = true
            }
        }

        result["has_valid_exif"] = validFields >= 2
        result["exif_completeness"] = validFields.toDouble() / totalExpectedFields
        result
    }

    override suspend fun detectDeepfake(evidence: Evidence): Map<String, Any> = withContext(Dispatchers.Default) {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["evidence_name"] = evidence.fileName

        var deepfakeProbability = 0.0
        val indicators = mutableListOf<String>()

        val metadata = evidence.metadata

        // Check for AI generation markers in metadata
        val aiMarkers = listOf("DALL-E", "Midjourney", "Stable Diffusion", "AI Generated", "synthetic")
        val software = metadata["software"] as? String
        if (software != null && aiMarkers.any { software.contains(it, ignoreCase = true) }) {
            deepfakeProbability += 0.9
            indicators.add("AI generation marker found in metadata: $software")
        }

        // Check for unusual file characteristics
        val fileSize = metadata["file_size"] as? Long ?: evidence.fileSize
        val width = metadata["width"] as? Int ?: 0
        val height = metadata["height"] as? Int ?: 0
        val pixels = width.toLong() * height.toLong()

        if (pixels > 0) {
            val bytesPerPixel = fileSize.toDouble() / pixels
            
            // AI-generated images often have different compression ratios
            if (bytesPerPixel < 0.1 || bytesPerPixel > 10) {
                deepfakeProbability += 0.1
                indicators.add("Unusual file size to pixel ratio")
            }
        }

        // Check for perfect dimensions (often AI-generated)
        val perfectDimensions = listOf(512, 768, 1024, 1536, 2048)
        if (perfectDimensions.contains(width) && perfectDimensions.contains(height)) {
            deepfakeProbability += 0.15
            indicators.add("Dimensions match common AI generation sizes ($width x $height)")
        }

        // Check for missing camera data on photos that should have it
        val dateOriginal = metadata["date_original"] as? Long
        val cameraMake = metadata["camera_make"] as? String
        if (dateOriginal != null && cameraMake == null) {
            deepfakeProbability += 0.1
            indicators.add("Photo timestamp present but no camera information")
        }

        // Check for suspicious pattern in file name
        val aiFilePatterns = listOf("generated", "synthetic", "fake", "deepfake", "ai_")
        if (aiFilePatterns.any { evidence.fileName.lowercase().contains(it) }) {
            deepfakeProbability += 0.2
            indicators.add("Filename suggests AI generation")
        }

        result["deepfake_probability"] = deepfakeProbability.coerceIn(0.0, 1.0)
        result["indicators"] = indicators
        result["classification"] = when {
            deepfakeProbability > 0.7 -> "AI-Generated"
            deepfakeProbability > 0.4 -> "Likely Edited"
            deepfakeProbability > 0.2 -> "Uncertain"
            else -> "Authentic"
        }
        result
    }

    override suspend fun analyzeLightingConsistency(evidence: Evidence): Map<String, Any> = withContext(Dispatchers.Default) {
        val result = mutableMapOf<String, Any>()
        result["evidence_id"] = evidence.id
        result["evidence_name"] = evidence.fileName

        val metadata = evidence.metadata
        var isConsistent = true
        val inconsistencies = mutableListOf<String>()

        // Check lighting direction metadata
        val lightDirection = metadata["light_direction"] as? List<*>
        if (lightDirection != null && lightDirection.size > 1) {
            // Multiple distinct light directions could indicate compositing
            result["light_directions"] = lightDirection.size
            if (lightDirection.size > 2) {
                isConsistent = false
                inconsistencies.add("Multiple conflicting light sources detected")
            }
        }

        // Check shadow consistency
        val shadowConsistency = metadata["shadow_consistency"] as? Double
        if (shadowConsistency != null && shadowConsistency < 0.7) {
            isConsistent = false
            inconsistencies.add("Shadow direction inconsistency detected")
            result["shadow_consistency_score"] = shadowConsistency
        }

        // Check exposure consistency across image regions
        val exposureVariance = metadata["exposure_variance"] as? Double
        if (exposureVariance != null && exposureVariance > 0.5) {
            isConsistent = false
            inconsistencies.add("Significant exposure variation across image regions")
            result["exposure_variance"] = exposureVariance
        }

        // Check for color temperature consistency
        val colorTempVariance = metadata["color_temp_variance"] as? Double
        if (colorTempVariance != null && colorTempVariance > 0.4) {
            isConsistent = false
            inconsistencies.add("Color temperature inconsistency detected")
            result["color_temp_variance"] = colorTempVariance
        }

        result["is_consistent"] = isConsistent
        result["inconsistencies"] = inconsistencies
        result
    }

    override suspend fun detectPixelManipulation(evidence: Evidence): List<Map<String, Any>> = withContext(Dispatchers.Default) {
        val manipulations = mutableListOf<Map<String, Any>>()

        val metadata = evidence.metadata

        // Check for clone stamping artifacts
        val cloneRegions = metadata["clone_regions"] as? List<*>
        if (cloneRegions != null && cloneRegions.isNotEmpty()) {
            manipulations.add(mapOf(
                "manipulation_type" to "clone_stamping",
                "evidence_id" to evidence.id,
                "description" to "Detected ${cloneRegions.size} regions with cloned content",
                "regions_count" to cloneRegions.size,
                "confidence" to 0.85
            ))
        }

        // Check for Error Level Analysis (ELA) anomalies
        val elaAnomalies = metadata["ela_anomalies"] as? List<*>
        if (elaAnomalies != null && elaAnomalies.isNotEmpty()) {
            manipulations.add(mapOf(
                "manipulation_type" to "compression_anomaly",
                "evidence_id" to evidence.id,
                "description" to "Error Level Analysis detected ${elaAnomalies.size} suspicious regions with different compression levels",
                "anomaly_count" to elaAnomalies.size,
                "confidence" to 0.80
            ))
        }

        // Check for edge artifacts (common in compositing)
        val edgeArtifacts = metadata["edge_artifacts"] as? Boolean
        if (edgeArtifacts == true) {
            manipulations.add(mapOf(
                "manipulation_type" to "edge_artifacts",
                "evidence_id" to evidence.id,
                "description" to "Unnatural edge artifacts detected, suggesting object insertion or removal",
                "confidence" to 0.75
            ))
        }

        // Check for noise pattern inconsistencies
        val noisePatternScore = metadata["noise_pattern_consistency"] as? Double
        if (noisePatternScore != null && noisePatternScore < 0.6) {
            manipulations.add(mapOf(
                "manipulation_type" to "noise_inconsistency",
                "evidence_id" to evidence.id,
                "description" to "Image noise patterns are inconsistent across regions (score: ${(noisePatternScore * 100).toInt()}%)",
                "consistency_score" to noisePatternScore,
                "confidence" to 0.70
            ))
        }

        // Check for resampling artifacts
        val resamplingDetected = metadata["resampling_detected"] as? Boolean
        if (resamplingDetected == true) {
            manipulations.add(mapOf(
                "manipulation_type" to "resampling",
                "evidence_id" to evidence.id,
                "description" to "Image contains resampling artifacts indicating resize or rotation manipulation",
                "confidence" to 0.72
            ))
        }

        // Check for JPEG artifact inconsistencies
        val jpegQualityVariance = metadata["jpeg_quality_variance"] as? Double
        if (jpegQualityVariance != null && jpegQualityVariance > 0.3) {
            manipulations.add(mapOf(
                "manipulation_type" to "compression_variance",
                "evidence_id" to evidence.id,
                "description" to "Different JPEG compression qualities detected within image, suggesting editing",
                "quality_variance" to jpegQualityVariance,
                "confidence" to 0.78
            ))
        }

        manipulations
    }
}
