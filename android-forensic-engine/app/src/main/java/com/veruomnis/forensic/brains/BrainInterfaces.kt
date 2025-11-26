package com.veruomnis.forensic.brains

import com.veruomnis.forensic.models.BrainAnalysisResult
import com.veruomnis.forensic.models.Evidence

/**
 * Base interface for all forensic brain modules
 */
interface ForensicBrain {
    val brainName: String
    suspend fun analyze(evidence: List<Evidence>): BrainAnalysisResult
}

/**
 * Brain 1: Contradiction Detection and Truth Stability
 */
interface ContradictionBrain : ForensicBrain {
    suspend fun calculateTruthStabilityIndex(evidence: List<Evidence>): Double
    suspend fun detectContradictions(evidence: List<Evidence>): List<Map<String, Any>>
    suspend fun analyzeTimelineConflicts(evidence: List<Evidence>): List<Map<String, Any>>
    suspend fun detectOmissionPatterns(evidence: List<Evidence>): List<Map<String, Any>>
}

/**
 * Brain 2: Behavioral Diagnostics
 */
interface BehavioralBrain : ForensicBrain {
    suspend fun analyzeBehavioralProbability(evidence: List<Evidence>): Double
    suspend fun detectMicroEmotions(evidence: List<Evidence>): List<Map<String, Any>>
    suspend fun recognizeIntentPatterns(evidence: List<Evidence>): List<Map<String, Any>>
    suspend fun detectManipulation(evidence: List<Evidence>): List<Map<String, Any>>
}

/**
 * Brain 3: Document Authenticity
 */
interface DocumentAuthenticityBrain : ForensicBrain {
    suspend fun analyzeMetadata(evidence: Evidence): Map<String, Any>
    suspend fun verifyHashIntegrity(evidence: Evidence): Boolean
    suspend fun detectTampering(evidence: Evidence): List<Map<String, Any>>
    suspend fun validateFileLineage(evidence: Evidence): Map<String, Any>
}

/**
 * Brain 4: Timeline & Geolocation
 */
interface TimelineGeolocationBrain : ForensicBrain {
    suspend fun buildMasterChronology(evidence: List<Evidence>): List<Map<String, Any>>
    suspend fun validateGpsCoordinates(evidence: List<Evidence>): List<Map<String, Any>>
    suspend fun analyzeTimelineGaps(evidence: List<Evidence>): List<Map<String, Any>>
    suspend fun detectImpossibleEvents(evidence: List<Evidence>): List<Map<String, Any>>
}

/**
 * Brain 5: Voice Forensics
 */
interface VoiceForensicsBrain : ForensicBrain {
    suspend fun verifyAudioAuthenticity(evidence: Evidence): Map<String, Any>
    suspend fun detectEmotionalStress(evidence: Evidence): List<Map<String, Any>>
    suspend fun analyzeVoiceprintConsistency(evidence: Evidence): Map<String, Any>
    suspend fun detectEditArtifacts(evidence: Evidence): List<Map<String, Any>>
}

/**
 * Brain 6: Image Validation
 */
interface ImageValidationBrain : ForensicBrain {
    suspend fun analyzeExifMetadata(evidence: Evidence): Map<String, Any>
    suspend fun detectDeepfake(evidence: Evidence): Map<String, Any>
    suspend fun analyzeLightingConsistency(evidence: Evidence): Map<String, Any>
    suspend fun detectPixelManipulation(evidence: Evidence): List<Map<String, Any>>
}

/**
 * Brain 7: Legal & Compliance
 */
interface LegalComplianceBrain : ForensicBrain {
    suspend fun mapJurisdictionalRules(evidence: List<Evidence>): Map<String, Any>
    suspend fun monitorLegalThresholds(evidence: List<Evidence>): List<Map<String, Any>>
    suspend fun checkComplianceRequirements(evidence: List<Evidence>): Map<String, Any>
    suspend fun crossReferenceStatutes(evidence: List<Evidence>): List<Map<String, Any>>
}

/**
 * Brain 8: Predictive Analytics
 */
interface PredictiveAnalyticsBrain : ForensicBrain {
    suspend fun modelRiskProbability(evidence: List<Evidence>): Double
    suspend fun forecastBehaviorEscalation(evidence: List<Evidence>): Map<String, Any>
    suspend fun predictEvidenceGaps(evidence: List<Evidence>): List<String>
    suspend fun scoreOutcomeProbability(evidence: List<Evidence>): Map<String, Any>
}

/**
 * Brain 9: Synthesis & Verdict
 */
interface SynthesisVerdictBrain : ForensicBrain {
    suspend fun generateUnifiedTruthModel(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Any>
    
    suspend fun verifyCrossBrainConsensus(
        brainResults: List<BrainAnalysisResult>
    ): Double
    
    suspend fun compileReport(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Any>
    
    suspend fun checkConstitutionalCompliance(
        evidence: List<Evidence>,
        brainResults: List<BrainAnalysisResult>
    ): Map<String, Boolean>
}
