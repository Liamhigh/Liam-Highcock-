package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Brain 4: Timeline & Geolocation Analysis
 * "If an event cannot exist in time, it cannot exist in truth."
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
            description = "Built timeline with ${chronology.size} events",
            confidence = 0.90,
            details = mapOf("event_count" to chronology.size, "events" to chronology)
        ))

        // Validate GPS coordinates
        val gpsValidation = validateGpsCoordinates(evidence)
        gpsValidation.forEach { validation ->
            val isValid = validation["is_valid"] as? Boolean ?: true
            findings.add(Finding(
                severity = if (isValid) Severity.INFO else Severity.HIGH,
                category = "GPS Validation",
                description = validation["description"] as? String ?: "GPS coordinates analyzed",
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
                description = gap["description"] as? String ?: "Timeline gap detected",
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        evidence.forEach { ev ->
            // Add evidence creation event
            events.add(mapOf(
                "event_type" to "evidence_added",
                "evidence_id" to ev.id,
                "evidence_name" to ev.fileName,
                "timestamp" to ev.dateAdded.time,
                "formatted_date" to dateFormat.format(ev.dateAdded)
            ))

            // Add metadata-based events
            val createdDate = ev.metadata["created_date"] as? Long
            if (createdDate != null) {
                events.add(mapOf(
                    "event_type" to "file_created",
                    "evidence_id" to ev.id,
                    "evidence_name" to ev.fileName,
                    "timestamp" to createdDate,
                    "formatted_date" to dateFormat.format(Date(createdDate))
                ))
            }

            val modifiedDate = ev.metadata["modified_date"] as? Long
            if (modifiedDate != null) {
                events.add(mapOf(
                    "event_type" to "file_modified",
                    "evidence_id" to ev.id,
                    "evidence_name" to ev.fileName,
                    "timestamp" to modifiedDate,
                    "formatted_date" to dateFormat.format(Date(modifiedDate))
                ))
            }

            // Add GPS-based events if location data exists
            val latitude = ev.metadata["latitude"] as? Double
            val longitude = ev.metadata["longitude"] as? Double
            if (latitude != null && longitude != null) {
                events.add(mapOf(
                    "event_type" to "gps_location",
                    "evidence_id" to ev.id,
                    "evidence_name" to ev.fileName,
                    "timestamp" to ev.dateAdded.time,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "formatted_date" to dateFormat.format(ev.dateAdded)
                ))
            }
        }

        return events.sortedBy { it["timestamp"] as Long }
    }

    override suspend fun validateGpsCoordinates(evidence: List<Evidence>): List<Map<String, Any>> {
        val validations = mutableListOf<Map<String, Any>>()

        evidence.forEach { ev ->
            val latitude = ev.metadata["latitude"] as? Double
            val longitude = ev.metadata["longitude"] as? Double

            if (latitude != null && longitude != null) {
                val isValid = isValidGpsCoordinate(latitude, longitude)
                validations.add(mapOf(
                    "evidence_id" to ev.id,
                    "evidence_name" to ev.fileName,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "is_valid" to isValid,
                    "description" to if (isValid) 
                        "GPS coordinates ($latitude, $longitude) are valid"
                    else 
                        "GPS coordinates ($latitude, $longitude) are invalid"
                ))
            }
        }

        return validations
    }

    override suspend fun analyzeTimelineGaps(evidence: List<Evidence>): List<Map<String, Any>> {
        val gaps = mutableListOf<Map<String, Any>>()

        if (evidence.size < 2) return gaps

        val sortedEvidence = evidence.sortedBy { it.dateAdded }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (i in 0 until sortedEvidence.size - 1) {
            val current = sortedEvidence[i]
            val next = sortedEvidence[i + 1]
            val gapMs = next.dateAdded.time - current.dateAdded.time
            val gapHours = gapMs / (1000 * 60 * 60)

            // Report significant gaps (more than 24 hours)
            if (gapHours > 24) {
                gaps.add(mapOf(
                    "gap_type" to "significant_time_gap",
                    "from_evidence" to current.id,
                    "to_evidence" to next.id,
                    "from_date" to dateFormat.format(current.dateAdded),
                    "to_date" to dateFormat.format(next.dateAdded),
                    "gap_hours" to gapHours,
                    "gap_days" to gapHours / 24,
                    "description" to "Gap of ${gapHours / 24} days between ${current.fileName} and ${next.fileName}"
                ))
            }
        }

        return gaps
    }

    override suspend fun detectImpossibleEvents(evidence: List<Evidence>): List<Map<String, Any>> {
        val impossibleEvents = mutableListOf<Map<String, Any>>()

        // Check for geographically impossible movements
        val gpsEvidence = evidence.filter { 
            it.metadata.containsKey("latitude") && it.metadata.containsKey("longitude") 
        }.sortedBy { it.dateAdded }

        if (gpsEvidence.size >= 2) {
            for (i in 0 until gpsEvidence.size - 1) {
                val current = gpsEvidence[i]
                val next = gpsEvidence[i + 1]

                val lat1 = current.metadata["latitude"] as? Double ?: continue
                val lon1 = current.metadata["longitude"] as? Double ?: continue
                val lat2 = next.metadata["latitude"] as? Double ?: continue
                val lon2 = next.metadata["longitude"] as? Double ?: continue

                val distanceKm = calculateHaversineDistance(lat1, lon1, lat2, lon2)
                val timeHours = (next.dateAdded.time - current.dateAdded.time) / (1000.0 * 60 * 60)

                if (timeHours > 0) {
                    val speedKmh = distanceKm / timeHours

                    // Check for impossible travel speed (> 1000 km/h without flight)
                    if (speedKmh > 1000) {
                        impossibleEvents.add(mapOf(
                            "event_type" to "impossible_travel",
                            "from_evidence" to current.id,
                            "to_evidence" to next.id,
                            "distance_km" to distanceKm,
                            "time_hours" to timeHours,
                            "implied_speed_kmh" to speedKmh,
                            "description" to "Evidence suggests travel at ${speedKmh.toInt()} km/h between ${current.fileName} and ${next.fileName}, which may indicate time/location falsification"
                        ))
                    }
                }
            }
        }

        // Check for temporal impossibilities
        evidence.forEach { ev ->
            val createdDate = ev.metadata["created_date"] as? Long
            val modifiedDate = ev.metadata["modified_date"] as? Long

            if (createdDate != null && modifiedDate != null && modifiedDate < createdDate) {
                impossibleEvents.add(mapOf(
                    "event_type" to "impossible_timeline",
                    "evidence_id" to ev.id,
                    "evidence_name" to ev.fileName,
                    "created_date" to createdDate,
                    "modified_date" to modifiedDate,
                    "description" to "File ${ev.fileName} shows modification date before creation date"
                ))
            }
        }

        return impossibleEvents
    }

    private fun isValidGpsCoordinate(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }
}
