package com.veruomnis.forensic.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.veruomnis.forensic.R
import com.veruomnis.forensic.databinding.ActivityReportBinding
import com.veruomnis.forensic.models.Severity

/**
 * Report Activity - Displays the forensic analysis report
 * Shows Truth Stability Index, verdict, findings, and recommendations
 */
class ReportActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReportBinding
    
    // Sample report data (in real app, this would come from analysis)
    private val sampleFindings = listOf(
        FindingItem(Severity.INFO, "Truth Stability", "Truth Stability Index: 85/100", 0.95, "Contradiction Brain"),
        FindingItem(Severity.INFO, "Document Authenticity", "Document integrity verified", 0.90, "Document Authenticity Brain"),
        FindingItem(Severity.MEDIUM, "Timeline Gap", "Gap of 14 days detected between evidence items", 0.80, "Timeline Brain"),
        FindingItem(Severity.INFO, "GPS Validation", "Valid GPS coordinates verified", 0.85, "Timeline Brain"),
        FindingItem(Severity.LOW, "Metadata", "Image EXIF data extracted successfully", 0.90, "Image Validation Brain"),
        FindingItem(Severity.INFO, "Compliance", "All compliance requirements met", 0.85, "Legal Compliance Brain"),
        FindingItem(Severity.INFO, "Outcome", "MODERATE case - 65% probability of favorable outcome", 0.70, "Predictive Analytics Brain")
    )
    
    private val sampleRecommendations = listOf(
        "Case analysis complete - proceed with documented findings",
        "Consider supplementing evidence to improve Truth Stability Index",
        "Evidence for period gaps should be collected if available"
    )
    
    private val sampleCompliance = mapOf(
        "Zero-Loss Evidence Doctrine" to true,
        "Triple AI Consensus" to true,
        "Guardianship Model" to true,
        "Contradiction Engine Active" to true,
        "Forensic Anchors Present" to true
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        displayReport()
        setupButtons()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun displayReport() {
        // Truth Stability Index
        val tsi = 85
        binding.tsiValueText.text = tsi.toString()
        binding.tsiValueText.setTextColor(
            ContextCompat.getColor(this, when {
                tsi >= 80 -> R.color.success
                tsi >= 60 -> R.color.warning
                else -> R.color.error
            })
        )
        binding.tsiProgressBar.progress = tsi
        
        // Verdict
        binding.verdictText.text = "EVIDENCE STABLE - Truth Stability Index meets threshold"
        
        // Consensus
        binding.consensusText.text = "92%"
        
        // Evidence count
        binding.evidenceCountText.text = "2"
        
        // Compliance
        displayCompliance()
        
        // Findings
        displayFindings()
        
        // Recommendations
        displayRecommendations()
    }
    
    private fun displayCompliance() {
        binding.complianceContainer.removeAllViews()
        
        sampleCompliance.forEach { (name, compliant) ->
            val itemView = layoutInflater.inflate(android.R.layout.simple_list_item_1, binding.complianceContainer, false) as TextView
            itemView.text = if (compliant) "✓ $name" else "✗ $name"
            itemView.setTextColor(ContextCompat.getColor(this, 
                if (compliant) R.color.success else R.color.error
            ))
            itemView.textSize = 14f
            itemView.setPadding(0, 8, 0, 8)
            binding.complianceContainer.addView(itemView)
        }
    }
    
    private fun displayFindings() {
        val adapter = FindingsAdapter(sampleFindings)
        binding.findingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReportActivity)
            this.adapter = adapter
        }
    }
    
    private fun displayRecommendations() {
        binding.recommendationsContainer.removeAllViews()
        
        sampleRecommendations.forEachIndexed { index, recommendation ->
            val itemView = layoutInflater.inflate(android.R.layout.simple_list_item_1, binding.recommendationsContainer, false) as TextView
            itemView.text = "${index + 1}. $recommendation"
            itemView.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            itemView.textSize = 14f
            itemView.setPadding(0, 8, 0, 8)
            binding.recommendationsContainer.addView(itemView)
        }
    }
    
    private fun setupButtons() {
        binding.exportPdfButton.setOnClickListener {
            Toast.makeText(this, "PDF export feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.shareButton.setOnClickListener {
            shareReport()
        }
    }
    
    private fun shareReport() {
        val shareText = buildString {
            appendLine("VERUM OMNIS FORENSIC REPORT")
            appendLine("=" .repeat(30))
            appendLine()
            appendLine("Truth Stability Index: 85/100")
            appendLine("Cross-Brain Consensus: 92%")
            appendLine()
            appendLine("VERDICT: EVIDENCE STABLE")
            appendLine()
            appendLine("Constitutional Compliance: FULL")
            appendLine()
            appendLine("\"Nine Brains. One Truth.\"")
            appendLine("Generated by Verum Omnis Forensic Engine v5.2.6")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Verum Omnis Forensic Report")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_report)))
    }
}

data class FindingItem(
    val severity: Severity,
    val category: String,
    val description: String,
    val confidence: Double,
    val sourceBrain: String
)

class FindingsAdapter(
    private val findings: List<FindingItem>
) : androidx.recyclerview.widget.RecyclerView.Adapter<FindingsAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val severityIndicator: View = itemView.findViewById(R.id.severityIndicator)
        val severityText: TextView = itemView.findViewById(R.id.severityText)
        val categoryText: TextView = itemView.findViewById(R.id.categoryText)
        val confidenceText: TextView = itemView.findViewById(R.id.confidenceText)
        val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        val sourceBrainText: TextView = itemView.findViewById(R.id.sourceBrainText)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_finding, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val finding = findings[position]
        val context = holder.itemView.context
        
        val severityColor = when (finding.severity) {
            Severity.CRITICAL -> R.color.severity_critical
            Severity.HIGH -> R.color.severity_high
            Severity.MEDIUM -> R.color.severity_medium
            Severity.LOW -> R.color.severity_low
            Severity.INFO -> R.color.severity_info
        }
        
        holder.severityIndicator.setBackgroundColor(ContextCompat.getColor(context, severityColor))
        holder.severityText.text = finding.severity.name
        holder.severityText.setTextColor(ContextCompat.getColor(context, severityColor))
        holder.categoryText.text = finding.category
        holder.confidenceText.text = "${(finding.confidence * 100).toInt()}%"
        holder.descriptionText.text = finding.description
        holder.sourceBrainText.text = "Source: ${finding.sourceBrain}"
    }
    
    override fun getItemCount() = findings.size
}
