package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Brain 4: Timeline & Geolocation Analysis
 * 
 * Builds master chronology and validates GPS coordinates
 * to detect temporal impossibilities and location conflicts.
 */
class TimelineGeolocationBrainImpl : TimelineGeolocationBrain {
    override val brainName = "Timeline Geolocation Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Build master chronology
        val chronology = buildMasterChronology(evidence)
        if (chronology.isNotEmpty()) {
            findings.add(Finding(
                severity = Severity.INFO,
                category = "Master Chronology",
                description = "Constructed timeline with ${chronology.size} events",
                confidence = 0.90,
                details = mapOf("event_count" to chronology.size)
            ))
        }

        // Validate GPS coordinates
        val gpsValidation = validateGpsCoordinates(evidence)
        gpsValidation.forEach { validation ->
            val isValid = validation["valid"] as? Boolean ?: true
            findings.add(Finding(
                severity = if (isValid) Severity.INFO else Severity.HIGH,
                category = "GPS Validation",
                description = validation["description"] as? String ?: "GPS coordinate check",
                confidence = 0.85,
                details = validation
            ))
        }

        // Analyze timeline gaps
        val gaps = analyzeTimelineGaps(evidence)
        gaps.forEach { gap ->
            findings.add(Finding(
                severity = Severity.MEDIUM,
                category = "Timeline Gap",
                description = gap["description"] as? String ?: "Gap detected in timeline",
                confidence = 0.80,
                details = gap
            ))
        }

        // Detect impossible events
        val impossibleEvents = detectImpossibleEvents(evidence)
        impossibleEvents.forEach { event ->
            findings.add(Finding(
                severity = Severity.CRITICAL,
                category = "Impossible Event",
                description = event["description"] as? String ?: "Temporally impossible event detected",
                confidence = 0.95,
                details = event
            ))
        }

        val processingTime = System.currentTimeMillis() - startTime
        BrainAnalysisResult(
            brainName = brainName,
            confidence = if (findings.isEmpty()) 0.90 else findings.maxOfOrNull { it.confidence } ?: 0.0,
            findings = findings,
            timestamp = Date(),
            processingTimeMs = processingTime
        )
    }

    override suspend fun buildMasterChronology(evidence: List<Evidence>): List<Map<String, Any>> {
        return evidence.sortedBy { it.dateAdded }.mapIndexed { index, ev ->
            mapOf(
                "sequence" to index + 1,
                "evidence_id" to ev.id,
                "file_name" to ev.fileName,
                "timestamp" to ev.dateAdded.time,
                "date_string" to ev.dateAdded.toString(),
                "type" to ev.type.name
            )
        }
    }

    override suspend fun validateGpsCoordinates(evidence: List<Evidence>): List<Map<String, Any>> {
        val validations = mutableListOf<Map<String, Any>>()
        
        evidence.forEach { ev ->
            val lat = ev.metadata["latitude"] as? Double
            val lon = ev.metadata["longitude"] as? Double
            
            if (lat != null && lon != null) {
                // Validate coordinate ranges
                val validLat = lat in -90.0..90.0
                val validLon = lon in -180.0..180.0
                
                if (!validLat || !validLon) {
                    validations.add(mapOf(
                        "evidence_id" to ev.id,
                        "valid" to false,
                        "description" to "Invalid GPS coordinates for ${ev.fileName}: ($lat, $lon)",
                        "latitude" to lat,
                        "longitude" to lon
                    ))
                } else {
                    validations.add(mapOf(
                        "evidence_id" to ev.id,
                        "valid" to true,
                        "description" to "GPS coordinates valid for ${ev.fileName}",
                        "latitude" to lat,
                        "longitude" to lon
                    ))
                }
            }
        }
        
        return validations
    }

    override suspend fun analyzeTimelineGaps(evidence: List<Evidence>): List<Map<String, Any>> {
        val gaps = mutableListOf<Map<String, Any>>()
        
        if (evidence.size < 2) return gaps
        
        val sorted = evidence.sortedBy { it.dateAdded }
        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]
            val gapMs = next.dateAdded.time - current.dateAdded.time
            val gapHours = gapMs / (1000 * 60 * 60)
            
            // Flag gaps larger than 24 hours
            if (gapHours > 24) {
                gaps.add(mapOf(
                    "type" to "timeline_gap",
                    "description" to "Gap of ${gapHours} hours between ${current.fileName} and ${next.fileName}",
                    "gap_hours" to gapHours,
                    "from_evidence" to current.id,
                    "to_evidence" to next.id
                ))
            }
        }
        
        return gaps
    }

    override suspend fun detectImpossibleEvents(evidence: List<Evidence>): List<Map<String, Any>> {
        val impossibleEvents = mutableListOf<Map<String, Any>>()
        
        if (evidence.size < 2) return impossibleEvents
        
        val sorted = evidence.sortedBy { it.dateAdded }
        
        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]
            
            // Check if same person/device claims to be in two distant locations quickly
            val currentLat = current.metadata["latitude"] as? Double
            val currentLon = current.metadata["longitude"] as? Double
            val nextLat = next.metadata["latitude"] as? Double
            val nextLon = next.metadata["longitude"] as? Double
            
            if (currentLat != null && currentLon != null && nextLat != null && nextLon != null) {
                val distance = calculateDistance(currentLat, currentLon, nextLat, nextLon)
                val timeGapHours = (next.dateAdded.time - current.dateAdded.time) / (1000.0 * 60 * 60)
                
                // Speed in km/h (max reasonable travel speed ~900 km/h for commercial flight)
                if (timeGapHours > 0) {
                    val speed = distance / timeGapHours
                    if (speed > 1000) { // Faster than physically possible
                        impossibleEvents.add(mapOf(
                            "type" to "impossible_travel",
                            "description" to "Evidence suggests travel of ${distance.toInt()} km in ${timeGapHours.toInt()} hours (${speed.toInt()} km/h)",
                            "from_evidence" to current.id,
                            "to_evidence" to next.id,
                            "distance_km" to distance,
                            "time_hours" to timeGapHours,
                            "implied_speed_kmh" to speed
                        ))
                    }
                }
            }
        }
        
        return impossibleEvents
    }
    
    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
