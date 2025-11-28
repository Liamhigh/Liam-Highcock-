package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.*

/**
 * Brain 4: Timeline & Geolocation Analysis
 * Builds master chronology, validates GPS coordinates, and detects impossible events
 */
class TimelineGeolocationBrainImpl : TimelineGeolocationBrain {
    override val brainName = "Timeline & Geolocation Brain"

    override suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // Build master chronology
        val chronology = buildMasterChronology(evidence)
        findings.add(Finding(
            severity = Severity.INFO,
            category = "Master Chronology",
            description = "Built chronology with ${chronology.size} timeline events",
            confidence = 0.90,
            details = mapOf("events_count" to chronology.size, "chronology" to chronology)
        ))

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
                description = event["description"] as? String ?: "Physically impossible event detected",
                confidence = 0.95,
                details = event
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

    override suspend fun buildMasterChronology(evidence: List<Evidence>): List<Map<String, Any>> {
        val events = mutableListOf<Map<String, Any>>()
        
        evidence.sortedBy { it.dateAdded }.forEachIndexed { index, ev ->
            val event = mutableMapOf<String, Any>(
                "sequence" to index + 1,
                "evidence_id" to ev.id,
                "file_name" to ev.fileName,
                "timestamp" to ev.dateAdded.time,
                "date" to ev.dateAdded.toString(),
                "type" to ev.type.name
            )
            
            // Add GPS if available
            val lat = ev.metadata["latitude"] as? Double
            val lon = ev.metadata["longitude"] as? Double
            if (lat != null && lon != null) {
                event["location"] = mapOf("latitude" to lat, "longitude" to lon)
            }
            
            // Add creation date from metadata if available
            val createdDate = ev.metadata["created_date"] as? Long
            if (createdDate != null) {
                event["metadata_date"] = createdDate
            }
            
            events.add(event)
        }
        
        return events
    }

    override suspend fun validateGpsCoordinates(evidence: List<Evidence>): List<Map<String, Any>> {
        val validations = mutableListOf<Map<String, Any>>()
        
        evidence.forEach { ev ->
            val lat = ev.metadata["latitude"] as? Double
            val lon = ev.metadata["longitude"] as? Double
            
            if (lat != null && lon != null) {
                // Validate GPS coordinates are within valid ranges
                val isValidLat = lat >= -90.0 && lat <= 90.0
                val isValidLon = lon >= -180.0 && lon <= 180.0
                
                if (!isValidLat || !isValidLon) {
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
                        "description" to "Valid GPS coordinates for ${ev.fileName}",
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
            val gapHours = gapMs / (1000.0 * 60 * 60)
            val gapDays = gapHours / 24
            
            // Flag gaps larger than 7 days
            if (gapDays > 7) {
                gaps.add(mapOf(
                    "type" to "significant_gap",
                    "description" to "Gap of ${gapDays.toInt()} days between '${current.fileName}' and '${next.fileName}'",
                    "from_evidence" to current.id,
                    "to_evidence" to next.id,
                    "gap_hours" to gapHours,
                    "gap_days" to gapDays
                ))
            }
        }
        
        return gaps
    }

    override suspend fun detectImpossibleEvents(evidence: List<Evidence>): List<Map<String, Any>> {
        val impossibleEvents = mutableListOf<Map<String, Any>>()
        
        if (evidence.size < 2) return impossibleEvents
        
        val eventsWithLocation = evidence
            .filter { 
                it.metadata["latitude"] != null && it.metadata["longitude"] != null 
            }
            .sortedBy { it.dateAdded }
        
        if (eventsWithLocation.size < 2) return impossibleEvents
        
        for (i in 0 until eventsWithLocation.size - 1) {
            val current = eventsWithLocation[i]
            val next = eventsWithLocation[i + 1]
            
            val lat1 = current.metadata["latitude"] as Double
            val lon1 = current.metadata["longitude"] as Double
            val lat2 = next.metadata["latitude"] as Double
            val lon2 = next.metadata["longitude"] as Double
            
            val distance = calculateHaversineDistance(lat1, lon1, lat2, lon2)
            val timeMs = next.dateAdded.time - current.dateAdded.time
            val timeHours = timeMs / (1000.0 * 60 * 60)
            
            if (timeHours > 0) {
                val speedKmh = distance / timeHours
                
                // Flag if speed exceeds realistic maximum (e.g., > 1000 km/h is unrealistic for most travel)
                if (speedKmh > 1000) {
                    impossibleEvents.add(mapOf(
                        "type" to "impossible_travel",
                        "description" to "Impossible travel: ${distance.toInt()} km in ${timeHours.format(2)} hours (${speedKmh.toInt()} km/h)",
                        "from_evidence" to current.id,
                        "to_evidence" to next.id,
                        "distance_km" to distance,
                        "time_hours" to timeHours,
                        "speed_kmh" to speedKmh
                    ))
                }
            }
        }
        
        return impossibleEvents
    }
    
    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     */
    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
