package com.veruomnis.forensic.engine

import android.content.Context
import com.veruomnis.forensic.brains.*
import com.veruomnis.forensic.models.*
import kotlinx.coroutines.*
import java.util.Date

/**
 * ForensicEngine - Main orchestrator for the Nine-Brain Architecture
 * 
 * Coordinates all forensic analysis brains and ensures constitutional compliance.
 */
class ForensicEngine(private val context: Context) {

    // Initialize all nine brains
    private val brains: List<ForensicBrain> = listOf(
        ContradictionBrainImpl(),           // Brain 1
        BehavioralBrainImpl(),              // Brain 2
        DocumentAuthenticityBrainImpl(),    // Brain 3
        TimelineGeolocationBrainImpl(),     // Brain 4
        VoiceForensicsBrainImpl(),          // Brain 5
        ImageValidationBrainImpl(),         // Brain 6
        LegalComplianceBrainImpl(),         // Brain 7
        PredictiveAnalyticsBrainImpl()      // Brain 8
    )

    // Synthesis brain (Brain 9) - for final aggregation
    private val synthesisBrain = SynthesisVerdictBrainImpl()

    // Cryptographic sealing system
    private val sealingSystem = CryptographicSealingSystem(context)

    // Analysis state
    private var analysisProgress: AnalysisProgress = AnalysisProgress()
    private var progressCallback: ((AnalysisProgress) -> Unit)? = null

    /**
     * Set progress callback for real-time updates
     */
    fun setProgressCallback(callback: (AnalysisProgress) -> Unit) {
        progressCallback = callback
    }

    /**
     * Run complete forensic analysis on evidence
     */
    suspend fun analyzeEvidence(
        evidence: List<Evidence>,
        gpsLocation: GpsLocation? = null
    ): ForensicReport = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // Reset progress
        analysisProgress = AnalysisProgress(
            totalBrains = brains.size + 1, // +1 for synthesis
            currentBrainIndex = 0,
            currentBrainName = "",
            overallProgress = 0.0,
            isComplete = false
        )
        updateProgress()

        // Verify input sealing (Gate 1)
        verifyInputSealing(evidence)

        // Run all brains in parallel where possible
        val brainResults = mutableListOf<BrainAnalysisResult>()

        // Run brains 1-8 in parallel for efficiency
        val deferredResults = brains.mapIndexed { index, brain ->
            async {
                analysisProgress = analysisProgress.copy(
                    currentBrainIndex = index + 1,
                    currentBrainName = brain.brainName,
                    overallProgress = (index.toDouble() / (brains.size + 1)) * 100
                )
                updateProgress()

                // Track transformation (Gate 2)
                sealingSystem.trackTransformation(
                    evidenceId = evidence.firstOrNull()?.id ?: "BATCH",
                    sourceBrain = brain.brainName,
                    transformationType = "ANALYSIS",
                    inputData = evidence.map { it.id }.joinToString(","),
                    outputData = "PENDING"
                )

                val result = brain.analyze(evidence)

                // Log completion
                sealingSystem.trackTransformation(
                    evidenceId = evidence.firstOrNull()?.id ?: "BATCH",
                    sourceBrain = brain.brainName,
                    transformationType = "COMPLETE",
                    inputData = evidence.map { it.id }.joinToString(","),
                    outputData = result.findings.size.toString()
                )

                result
            }
        }

        // Collect results
        deferredResults.forEach { deferred ->
            brainResults.add(deferred.await())
        }

        // Run synthesis brain (Brain 9)
        analysisProgress = analysisProgress.copy(
            currentBrainIndex = brains.size + 1,
            currentBrainName = synthesisBrain.brainName,
            overallProgress = 90.0
        )
        updateProgress()

        // Generate synthesis results
        val unifiedModel = synthesisBrain.generateUnifiedTruthModel(evidence, brainResults)
        val consensusScore = synthesisBrain.verifyCrossBrainConsensus(brainResults)
        val compiledReport = synthesisBrain.compileReport(evidence, brainResults)
        val constitutionalCompliance = synthesisBrain.checkConstitutionalCompliance(evidence, brainResults)

        // Build synthesis result
        val synthesisResult = SynthesisResult(
            truthStabilityIndex = unifiedModel["truth_stability_index"] as? Double ?: 75.0,
            overallVerdict = (compiledReport["verdict"] as? Map<*, *>)?.get("verdict") as? String ?: "INCONCLUSIVE",
            consensusLevel = consensusScore,
            keyFindings = compiledReport["key_findings"] as? List<String> ?: emptyList(),
            contradictions = extractContradictions(brainResults),
            recommendations = compiledReport["recommended_actions"] as? List<String> ?: emptyList()
        )

        // Build constitutional compliance
        val compliance = ConstitutionalCompliance(
            zeroLossEvidenceDoctrine = constitutionalCompliance["zero_loss_evidence_doctrine"] ?: false,
            tripleAiConsensus = constitutionalCompliance["triple_ai_consensus"] ?: false,
            guardianshipModelEnforced = constitutionalCompliance["guardianship_model_enforced"] ?: false,
            complianceNotes = buildComplianceNotes(constitutionalCompliance)
        )

        // Create report ID
        val reportId = "VO-${System.currentTimeMillis()}-${(1000..9999).random()}"

        // Build the forensic report (without seal first)
        val unsealedReport = ForensicReport(
            reportId = reportId,
            evidence = evidence,
            brainResults = brainResults,
            synthesisResult = synthesisResult,
            cryptographicSeal = CryptographicSeal(
                sha512Hash = "",
                timestamp = Date(),
                gpsLocation = gpsLocation,
                qrCodeData = "",
                digitalWatermark = ""
            ),
            generatedAt = Date(),
            constitutionalCompliance = compliance
        )

        // Apply output seal (Gate 3)
        val cryptographicSeal = sealingSystem.sealOutput(unsealedReport, gpsLocation)

        // Mark analysis complete
        analysisProgress = analysisProgress.copy(
            overallProgress = 100.0,
            isComplete = true,
            processingTimeMs = System.currentTimeMillis() - startTime
        )
        updateProgress()

        // Return sealed report
        unsealedReport.copy(cryptographicSeal = cryptographicSeal)
    }

    /**
     * Run a single brain analysis
     */
    suspend fun runSingleBrain(
        brainIndex: Int,
        evidence: List<Evidence>
    ): BrainAnalysisResult? {
        if (brainIndex < 0 || brainIndex >= brains.size) return null
        return brains[brainIndex].analyze(evidence)
    }

    /**
     * Get brain by index
     */
    fun getBrain(index: Int): ForensicBrain? {
        return if (index in brains.indices) brains[index] else null
    }

    /**
     * Get all brain names
     */
    fun getBrainNames(): List<String> {
        return brains.map { it.brainName } + synthesisBrain.brainName
    }

    /**
     * Verify evidence integrity
     */
    suspend fun verifyEvidenceIntegrity(evidence: Evidence): IntegrityVerification {
        return sealingSystem.verifyIntegrity(evidence)
    }

    /**
     * Get the cryptographic sealing system
     */
    fun getSealingSystem(): CryptographicSealingSystem = sealingSystem

    /**
     * Get current analysis progress
     */
    fun getProgress(): AnalysisProgress = analysisProgress

    // Private helper functions

    private fun verifyInputSealing(evidence: List<Evidence>) {
        evidence.forEach { ev ->
            if (ev.metadata["hash"] == null) {
                // Evidence was not sealed - log warning
                sealingSystem.trackTransformation(
                    evidenceId = ev.id,
                    sourceBrain = "ForensicEngine",
                    transformationType = "INPUT_WARNING",
                    inputData = ev.id,
                    outputData = "Evidence not properly sealed at input"
                )
            }
        }
    }

    private fun extractContradictions(brainResults: List<BrainAnalysisResult>): List<Contradiction> {
        return brainResults.flatMap { brain ->
            brain.findings
                .filter { it.category.contains("Contradiction") || it.category.contains("Conflict") }
                .map { finding ->
                    Contradiction(
                        type = finding.category,
                        description = finding.description,
                        evidenceIds = (finding.details["evidence_ids"] as? List<*>)
                            ?.mapNotNull { it as? String } ?: emptyList(),
                        severity = finding.severity,
                        resolutionSuggestion = "Review contradicting evidence for resolution"
                    )
                }
        }
    }

    private fun buildComplianceNotes(compliance: Map<String, Boolean>): List<String> {
        val notes = mutableListOf<String>()

        if (compliance["zero_loss_evidence_doctrine"] == true) {
            notes.add("Zero-Loss Evidence Doctrine: COMPLIANT")
        } else {
            notes.add("Zero-Loss Evidence Doctrine: NON-COMPLIANT - Review evidence preservation")
        }

        if (compliance["triple_ai_consensus"] == true) {
            notes.add("Triple-AI Consensus: ACHIEVED (≥75% agreement)")
        } else {
            notes.add("Triple-AI Consensus: NOT ACHIEVED - Brains have conflicting analyses")
        }

        if (compliance["guardianship_model_enforced"] == true) {
            notes.add("Guardianship Model: ENFORCED")
        } else {
            notes.add("Guardianship Model: VIOLATION DETECTED")
        }

        if (compliance["article_x_complete"] == true) {
            notes.add("Article X Seal Rule: ALL THREE GATES OPERATIONAL")
        } else {
            val gates = mutableListOf<String>()
            if (compliance["article_x_gate_1_input"] != true) gates.add("Gate 1 (Input)")
            if (compliance["article_x_gate_2_processing"] != true) gates.add("Gate 2 (Processing)")
            if (compliance["article_x_gate_3_output"] != true) gates.add("Gate 3 (Output)")
            notes.add("Article X Seal Rule: INCOMPLETE - Issues with ${gates.joinToString(", ")}")
        }

        return notes
    }

    private fun updateProgress() {
        progressCallback?.invoke(analysisProgress)
    }
}

/**
 * Analysis progress tracking
 */
data class AnalysisProgress(
    val totalBrains: Int = 9,
    val currentBrainIndex: Int = 0,
    val currentBrainName: String = "",
    val overallProgress: Double = 0.0,
    val isComplete: Boolean = false,
    val processingTimeMs: Long = 0
)
