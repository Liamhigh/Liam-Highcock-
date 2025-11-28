package com.veruomnis.forensic.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.veruomnis.forensic.R
import com.veruomnis.forensic.brains.*
import com.veruomnis.forensic.databinding.ActivityAnalysisBinding
import com.veruomnis.forensic.models.BrainAnalysisResult
import com.veruomnis.forensic.models.Evidence
import com.veruomnis.forensic.models.EvidenceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Analysis Activity - Runs the Nine-Brain forensic analysis
 * Shows real-time progress for each brain module
 */
class AnalysisActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_EVIDENCE_COUNT = "evidence_count"
    }
    
    private lateinit var binding: ActivityAnalysisBinding
    private val brainProgress = mutableListOf<BrainProgressItem>()
    private var brainAdapter: BrainProgressAdapter? = null
    private val analysisResults = mutableListOf<BrainAnalysisResult>()
    
    // Sample evidence for demonstration
    private val sampleEvidence = listOf(
        Evidence(
            id = UUID.randomUUID().toString(),
            type = EvidenceType.DOCUMENT,
            filePath = "/sample/document.pdf",
            fileName = "contract.pdf",
            mimeType = "application/pdf",
            fileSize = 245000,
            dateAdded = Date(),
            metadata = mapOf("hash" to "abc123", "source" to "demo")
        ),
        Evidence(
            id = UUID.randomUUID().toString(),
            type = EvidenceType.IMAGE,
            filePath = "/sample/photo.jpg",
            fileName = "evidence_photo.jpg",
            mimeType = "image/jpeg",
            fileSize = 1500000,
            dateAdded = Date(),
            metadata = mapOf("camera_make" to "Samsung", "latitude" to -30.5, "longitude" to 31.0)
        )
    )
    
    private val brains = listOf(
        Pair("1. Contradiction Detection", ContradictionBrainImpl()),
        Pair("2. Behavioral Diagnostics", BehavioralBrainImpl()),
        Pair("3. Document Authenticity", DocumentAuthenticityBrainImpl()),
        Pair("4. Timeline & Geolocation", TimelineGeolocationBrainImpl()),
        Pair("5. Voice Forensics", VoiceForensicsBrainImpl()),
        Pair("6. Image Validation", ImageValidationBrainImpl()),
        Pair("7. Legal & Compliance", LegalComplianceBrainImpl()),
        Pair("8. Predictive Analytics", PredictiveAnalyticsBrainImpl()),
        Pair("9. Synthesis & Verdict", SynthesisVerdictBrainImpl())
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        startAnalysis()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        // Initialize brain progress items
        brains.forEachIndexed { index, (name, _) ->
            brainProgress.add(BrainProgressItem(
                name = name,
                status = if (index == 0) "Analyzing..." else "Pending",
                isComplete = false,
                isActive = index == 0,
                colorIndex = index
            ))
        }
        
        brainAdapter = BrainProgressAdapter(brainProgress)
        binding.brainProgressRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AnalysisActivity)
            adapter = brainAdapter
        }
    }
    
    private fun startAnalysis() {
        lifecycleScope.launch {
            brains.forEachIndexed { index, (name, brain) ->
                // Update UI for current brain
                updateBrainStatus(index, "Analyzing...")
                binding.currentBrainText.text = getString(R.string.analyzing_brain, name)
                binding.progressText.text = getString(R.string.brain_progress, index + 1)
                binding.overallProgressBar.progress = ((index + 1) * 100) / brains.size
                
                try {
                    // Run brain analysis
                    val result = brain.analyze(sampleEvidence)
                    analysisResults.add(result)
                    
                    // Mark complete
                    updateBrainStatus(index, "Complete (${result.findings.size} findings)", isComplete = true)
                    
                    // Small delay for visual feedback
                    delay(300)
                    
                } catch (e: Exception) {
                    updateBrainStatus(index, "Error: ${e.message}", isComplete = true)
                }
                
                // Activate next brain
                if (index < brains.size - 1) {
                    brainProgress[index + 1] = brainProgress[index + 1].copy(
                        isActive = true,
                        status = "Starting..."
                    )
                    brainAdapter?.notifyItemChanged(index + 1)
                }
            }
            
            // Analysis complete
            onAnalysisComplete()
        }
    }
    
    private fun updateBrainStatus(index: Int, status: String, isComplete: Boolean = false) {
        brainProgress[index] = brainProgress[index].copy(
            status = status,
            isComplete = isComplete,
            isActive = !isComplete
        )
        brainAdapter?.notifyItemChanged(index)
    }
    
    private fun onAnalysisComplete() {
        binding.statusText.text = getString(R.string.analysis_complete)
        binding.currentBrainText.text = "All ${brains.size} brains completed analysis"
        binding.overallProgressBar.progress = 100
        
        binding.viewReportButton.visibility = View.VISIBLE
        binding.viewReportButton.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }
    }
}

data class BrainProgressItem(
    val name: String,
    val status: String,
    val isComplete: Boolean,
    val isActive: Boolean,
    val colorIndex: Int
)

class BrainProgressAdapter(
    private val items: List<BrainProgressItem>
) : androidx.recyclerview.widget.RecyclerView.Adapter<BrainProgressAdapter.ViewHolder>() {
    
    private val brainColors = intArrayOf(
        R.color.brain_1, R.color.brain_2, R.color.brain_3,
        R.color.brain_4, R.color.brain_5, R.color.brain_6,
        R.color.brain_7, R.color.brain_8, R.color.brain_9
    )
    
    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val colorIndicator: View = itemView.findViewById(R.id.brainColorIndicator)
        val nameText: android.widget.TextView = itemView.findViewById(R.id.brainNameText)
        val statusText: android.widget.TextView = itemView.findViewById(R.id.brainStatusText)
        val progressIndicator: com.google.android.material.progressindicator.CircularProgressIndicator = 
            itemView.findViewById(R.id.brainProgressIndicator)
        val completeIcon: android.widget.ImageView = itemView.findViewById(R.id.brainCompleteIcon)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_brain_progress, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        
        holder.colorIndicator.setBackgroundColor(
            androidx.core.content.ContextCompat.getColor(context, brainColors[item.colorIndex])
        )
        holder.nameText.text = item.name
        holder.statusText.text = item.status
        
        if (item.isComplete) {
            holder.progressIndicator.visibility = View.GONE
            holder.completeIcon.visibility = View.VISIBLE
        } else if (item.isActive) {
            holder.progressIndicator.visibility = View.VISIBLE
            holder.completeIcon.visibility = View.GONE
        } else {
            holder.progressIndicator.visibility = View.GONE
            holder.completeIcon.visibility = View.GONE
        }
    }
    
    override fun getItemCount() = items.size
}
