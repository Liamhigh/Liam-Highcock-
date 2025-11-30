package com.veruomnis.forensic.engine

import com.veruomnis.forensic.brains.*
import com.veruomnis.forensic.models.*
import com.veruomnis.forensic.crypto.CryptographicSealer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

/**
 * Forensic Engine Orchestrator
 * 
 * The central engine that orchestrates all Nine Brains to perform
 * comprehensive forensic analysis. Runs 100% offline.
 * 
 * Constitutional Compliance:
 * - All 9 brains are consulted (Articles I-IX)
 * - Zero-Loss Evidence Doctrine enforced
 * - Triple-AI Consensus verification
 * - Article X Seal Rule applied
 */
class ForensicEngine {
    
    // Nine Brain instances
    private val brain1: ContradictionBrain = ContradictionBrainImpl()
    private val brain2: BehavioralBrain = BehavioralBrainImpl()
    private val brain3: DocumentAuthenticityBrain = DocumentAuthenticityBrainImpl()
    private val brain4: TimelineGeolocationBrain = TimelineGeolocationBrainImpl()
    private val brain5: VoiceForensicsBrain = VoiceForensicsBrainImpl()
    private val brain6: ImageValidationBrain = ImageValidationBrainImpl()
    private val brain7: LegalComplianceBrain = LegalComplianceBrainImpl()
    private val brain8: PredictiveAnalyticsBrain = PredictiveAnalyticsBrainImpl()
    private val brain9: SynthesisVerdictBrain = SynthesisVerdictBrainImpl()
    
    // Cryptographic sealer
    private val sealer = CryptographicSealer()
    
    /**
     * Callback interface for analysis progress updates
     */
    interface ProgressCallback {
        fun onBrainStarted(brainNumber: Int, brainName: String)
        fun onBrainCompleted(brainNumber: Int, brainName: String, result: BrainAnalysisResult)
        fun onProgressUpdate(percentage: Int, message: String)
        fun onAnalysisComplete(report: ForensicReport)
        fun onError(error: String)
    }
    
    /**
     * Perform complete Nine-Brain forensic analysis
     * 
     * @param evidence List of evidence items to analyze
     * @param gpsLocation Optional GPS location for sealing
     * @param callback Optional progress callback
     * @return Complete forensic report with cryptographic seal
     */
    suspend fun analyzeEvidence(
        evidence: List<Evidence>,
        gpsLocation: GpsLocation? = null,
        callback: ProgressCallback? = null
    ): ForensicReport = withContext(Dispatchers.Default) {
        
        if (evidence.isEmpty()) {
            throw IllegalArgumentException("No evidence provided for analysis")
        }
        
        val brainResults = mutableListOf<BrainAnalysisResult>()
        val totalBrains = 9
        
        try {
            // Process brains 1-8 (can partially parallelize)
            callback?.onProgressUpdate(0, "Initializing Nine-Brain Analysis...")
            
            // Brain 1: Contradiction Detection
            callback?.onBrainStarted(1, brain1.brainName)
            val result1 = brain1.analyze(evidence)
            brainResults.add(result1)
            callback?.onBrainCompleted(1, brain1.brainName, result1)
            callback?.onProgressUpdate(11, "Contradiction analysis complete")
            
            // Brain 2: Behavioral Diagnostics
            callback?.onBrainStarted(2, brain2.brainName)
            val result2 = brain2.analyze(evidence)
            brainResults.add(result2)
            callback?.onBrainCompleted(2, brain2.brainName, result2)
            callback?.onProgressUpdate(22, "Behavioral analysis complete")
            
            // Brain 3: Document Authenticity
            callback?.onBrainStarted(3, brain3.brainName)
            val result3 = brain3.analyze(evidence)
            brainResults.add(result3)
            callback?.onBrainCompleted(3, brain3.brainName, result3)
            callback?.onProgressUpdate(33, "Document analysis complete")
            
            // Brain 4: Timeline & Geolocation
            callback?.onBrainStarted(4, brain4.brainName)
            val result4 = brain4.analyze(evidence)
            brainResults.add(result4)
            callback?.onBrainCompleted(4, brain4.brainName, result4)
            callback?.onProgressUpdate(44, "Timeline analysis complete")
            
            // Brain 5: Voice Forensics
            callback?.onBrainStarted(5, brain5.brainName)
            val result5 = brain5.analyze(evidence)
            brainResults.add(result5)
            callback?.onBrainCompleted(5, brain5.brainName, result5)
            callback?.onProgressUpdate(55, "Voice analysis complete")
            
            // Brain 6: Image Validation
            callback?.onBrainStarted(6, brain6.brainName)
            val result6 = brain6.analyze(evidence)
            brainResults.add(result6)
            callback?.onBrainCompleted(6, brain6.brainName, result6)
            callback?.onProgressUpdate(66, "Image analysis complete")
            
            // Brain 7: Legal Compliance
            callback?.onBrainStarted(7, brain7.brainName)
            val result7 = brain7.analyze(evidence)
            brainResults.add(result7)
            callback?.onBrainCompleted(7, brain7.brainName, result7)
            callback?.onProgressUpdate(77, "Legal analysis complete")
            
            // Brain 8: Predictive Analytics
            callback?.onBrainStarted(8, brain8.brainName)
            val result8 = brain8.analyze(evidence)
            brainResults.add(result8)
            callback?.onBrainCompleted(8, brain8.brainName, result8)
            callback?.onProgressUpdate(88, "Predictive analysis complete")
            
            // Brain 9: Synthesis & Verdict (uses results from all other brains)
            callback?.onBrainStarted(9, brain9.brainName)
            val result9 = brain9.analyze(evidence)
            brainResults.add(result9)
            callback?.onBrainCompleted(9, brain9.brainName, result9)
            callback?.onProgressUpdate(95, "Synthesis complete")
            
            // Generate synthesis result
            val synthesisData = brain9.compileReport(evidence, brainResults)
            val synthesisResult = createSynthesisResult(synthesisData, brainResults)
            
            // Generate cryptographic seal
            callback?.onProgressUpdate(98, "Applying cryptographic seal...")
            val seal = sealer.generateSeal(
                evidence = evidence,
                brainResults = brainResults,
                gpsLocation = gpsLocation
            )
            
            // Check constitutional compliance
            val complianceData = brain9.checkConstitutionalCompliance(evidence, brainResults)
            val compliance = ConstitutionalCompliance(
                zeroLossEvidenceDoctrine = complianceData["zero_loss_doctrine"] ?: false,
                tripleAiConsensus = complianceData["triple_ai_consensus"] ?: false,
                guardianshipModelEnforced = complianceData["guardianship_model"] ?: false,
                complianceNotes = listOf(
                    "All ${brainResults.size} brains consulted",
                    "Evidence count: ${evidence.size}",
                    "Seal generated with SHA-512"
                )
            )
            
            // Create final report
            val report = ForensicReport(
                reportId = UUID.randomUUID().toString(),
                evidence = evidence,
                brainResults = brainResults,
                synthesisResult = synthesisResult,
                cryptographicSeal = seal,
                generatedAt = Date(),
                constitutionalCompliance = compliance
            )
            
            callback?.onProgressUpdate(100, "Report generation complete")
            callback?.onAnalysisComplete(report)
            
            report
            
        } catch (e: Exception) {
            callback?.onError("Analysis failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Create a SynthesisResult from the compiled report data
     */
    private suspend fun createSynthesisResult(
        data: Map<String, Any>,
        brainResults: List<BrainAnalysisResult>
    ): SynthesisResult {
        val tsi = (data["truth_stability_index"] as? Double) ?: 0.0
        val verdict = (data["verdict"] as? String) ?: "Analysis complete"
        val consensus = (data["consensus_level"] as? Double) ?: 0.0
        val keyFindings = (data["key_findings"] as? List<String>) ?: emptyList()
        val recommendations = (data["recommendations"] as? List<String>) ?: emptyList()
        
        // Extract contradictions from Brain 1 results
        val contradictions = brainResults
            .find { it.brainName.contains("Contradiction") }
            ?.findings
            ?.filter { it.category.contains("Contradiction") }
            ?.map { finding ->
                Contradiction(
                    type = finding.category,
                    description = finding.description,
                    evidenceIds = (finding.details["evidence_ids"] as? List<String>) ?: emptyList(),
                    severity = finding.severity,
                    resolutionSuggestion = "Review evidence for inconsistencies"
                )
            } ?: emptyList()
        
        return SynthesisResult(
            truthStabilityIndex = tsi,
            overallVerdict = verdict,
            consensusLevel = consensus,
            keyFindings = keyFindings,
            contradictions = contradictions,
            recommendations = recommendations
        )
    }
    
    /**
     * Get the Truth Stability Index from existing brain results
     */
    suspend fun calculateTruthStabilityIndex(evidence: List<Evidence>): Double {
        return brain1.calculateTruthStabilityIndex(evidence)
    }
    
    /**
     * Quick analysis using only critical brains (1, 3, 9)
     */
    suspend fun quickAnalysis(evidence: List<Evidence>): BrainAnalysisResult {
        val results = mutableListOf<BrainAnalysisResult>()
        results.add(brain1.analyze(evidence))
        results.add(brain3.analyze(evidence))
        
        // Use synthesis brain to combine
        val reportData = brain9.compileReport(evidence, results)
        return brain9.analyze(evidence)
    }
}
