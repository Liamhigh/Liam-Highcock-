/**
 * Report Generator
 * Generates formatted reports from analysis results
 */

export class ReportGenerator {
  constructor(config = {}) {
    this.config = config;
    this.outputFormat = config.outputFormat || 'json';
  }

  /**
   * Generate report from analysis results
   * @param {object} results - Analysis results
   * @returns {Promise<string>} Formatted report
   */
  async generate(results) {
    switch (this.outputFormat) {
      case 'json':
        return this.generateJSON(results);
      case 'text':
        return this.generateText(results);
      case 'markdown':
        return this.generateMarkdown(results);
      case 'html':
        return this.generateHTML(results);
      default:
        return this.generateJSON(results);
    }
  }

  /**
   * Generate JSON report
   */
  generateJSON(results) {
    return JSON.stringify(results, null, 2);
  }

  /**
   * Generate plain text report
   */
  generateText(results) {
    const lines = [];
    
    lines.push('═'.repeat(60));
    lines.push('VERUM OMNIS - CONTRADICTION ENGINE REPORT');
    lines.push('═'.repeat(60));
    lines.push('');
    lines.push(`Generated: ${results.timestamp}`);
    lines.push(`Processing Time: ${results.processingTimeMs}ms`);
    lines.push(`Document Hash: ${results.documentHash || 'Not generated'}`);
    lines.push('');
    
    lines.push('─'.repeat(60));
    lines.push('SUMMARY');
    lines.push('─'.repeat(60));
    lines.push(`Total Contradictions: ${results.summary.totalContradictions}`);
    lines.push(`  - High Severity: ${results.summary.highSeverity}`);
    lines.push(`  - Medium Severity: ${results.summary.mediumSeverity}`);
    lines.push(`  - Low Severity: ${results.summary.lowSeverity}`);
    lines.push(`Risk Score: ${results.summary.riskScore}/100`);
    lines.push('');

    if (results.findings.length > 0) {
      lines.push('─'.repeat(60));
      lines.push('FINDINGS');
      lines.push('─'.repeat(60));
      
      results.findings.forEach((finding, index) => {
        lines.push(`\n[${index + 1}] ${finding.type.toUpperCase()}`);
        lines.push(`    Severity: ${finding.severity.toUpperCase()}`);
        lines.push(`    Confidence: ${(finding.confidence * 100).toFixed(1)}%`);
        lines.push(`    Description: ${finding.description}`);
        lines.push(`    Statement 1: "${finding.statement1}"`);
        lines.push(`    Statement 2: "${finding.statement2}"`);
        if (finding.verified) {
          lines.push(`    ✓ Triple Verified`);
        }
      });
    }

    lines.push('');
    lines.push('═'.repeat(60));
    lines.push('END OF REPORT');
    lines.push('═'.repeat(60));

    return lines.join('\n');
  }

  /**
   * Generate Markdown report
   */
  generateMarkdown(results) {
    const lines = [];

    lines.push('# Verum Omnis - Contradiction Engine Report');
    lines.push('');
    lines.push('## Overview');
    lines.push('');
    lines.push(`| Metric | Value |`);
    lines.push(`|--------|-------|`);
    lines.push(`| Generated | ${results.timestamp} |`);
    lines.push(`| Processing Time | ${results.processingTimeMs}ms |`);
    lines.push(`| Document Hash | \`${results.documentHash || 'N/A'}\` |`);
    lines.push('');

    lines.push('## Summary');
    lines.push('');
    lines.push(`- **Total Contradictions:** ${results.summary.totalContradictions}`);
    lines.push(`  - 🔴 High Severity: ${results.summary.highSeverity}`);
    lines.push(`  - 🟡 Medium Severity: ${results.summary.mediumSeverity}`);
    lines.push(`  - 🟢 Low Severity: ${results.summary.lowSeverity}`);
    lines.push(`- **Risk Score:** ${results.summary.riskScore}/100`);
    lines.push('');

    if (results.findings.length > 0) {
      lines.push('## Findings');
      lines.push('');

      results.findings.forEach((finding, index) => {
        const severityEmoji = {
          high: '🔴',
          medium: '🟡',
          low: '🟢'
        }[finding.severity] || '⚪';

        lines.push(`### ${index + 1}. ${severityEmoji} ${finding.type.replace(/_/g, ' ').toUpperCase()}`);
        lines.push('');
        lines.push(`**Severity:** ${finding.severity} | **Confidence:** ${(finding.confidence * 100).toFixed(1)}%`);
        lines.push('');
        lines.push(`> ${finding.description}`);
        lines.push('');
        lines.push('**Statement 1:**');
        lines.push(`> ${finding.statement1}`);
        lines.push('');
        lines.push('**Statement 2:**');
        lines.push(`> ${finding.statement2}`);
        lines.push('');
        if (finding.verified) {
          lines.push('✅ *Triple Verified*');
          lines.push('');
        }
      });
    }

    if (results.forensicData) {
      lines.push('## Forensic Data');
      lines.push('');
      lines.push(`- **Forensic Grade:** ${results.forensicData.summary.forensicGrade}`);
      lines.push(`- **Average Confidence:** ${(results.forensicData.summary.averageConfidence * 100).toFixed(1)}%`);
      lines.push('');
    }

    lines.push('---');
    lines.push('*Generated by Verum Omnis Contradiction Engine v1.0.0*');

    return lines.join('\n');
  }

  /**
   * Generate HTML report
   */
  generateHTML(results) {
    const severityColors = {
      high: '#dc3545',
      medium: '#ffc107',
      low: '#28a745'
    };

    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Verum Omnis - Contradiction Report</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 900px; margin: 0 auto; padding: 20px; background: #f5f5f5; }
    .header { background: linear-gradient(135deg, #1a1a2e, #16213e); color: white; padding: 30px; border-radius: 10px; margin-bottom: 20px; }
    .header h1 { margin: 0; font-size: 1.8em; }
    .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; margin-bottom: 20px; }
    .stat-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; }
    .stat-value { font-size: 2em; font-weight: bold; color: #1a1a2e; }
    .stat-label { color: #666; font-size: 0.9em; }
    .findings { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
    .finding { border-left: 4px solid; padding: 15px; margin-bottom: 15px; background: #f9f9f9; border-radius: 0 8px 8px 0; }
    .finding-high { border-color: #dc3545; }
    .finding-medium { border-color: #ffc107; }
    .finding-low { border-color: #28a745; }
    .finding-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
    .finding-type { font-weight: bold; text-transform: uppercase; }
    .badge { padding: 4px 8px; border-radius: 4px; font-size: 0.8em; color: white; }
    .statement { background: white; padding: 10px; margin: 5px 0; border-radius: 4px; font-style: italic; }
    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 0.9em; }
  </style>
</head>
<body>
  <div class="header">
    <h1>⚖️ Verum Omnis - Contradiction Report</h1>
    <p>Generated: ${results.timestamp}</p>
  </div>

  <div class="summary">
    <div class="stat-card">
      <div class="stat-value">${results.summary.totalContradictions}</div>
      <div class="stat-label">Total Contradictions</div>
    </div>
    <div class="stat-card">
      <div class="stat-value" style="color: #dc3545">${results.summary.highSeverity}</div>
      <div class="stat-label">High Severity</div>
    </div>
    <div class="stat-card">
      <div class="stat-value" style="color: #ffc107">${results.summary.mediumSeverity}</div>
      <div class="stat-label">Medium Severity</div>
    </div>
    <div class="stat-card">
      <div class="stat-value">${results.summary.riskScore}</div>
      <div class="stat-label">Risk Score</div>
    </div>
  </div>

  ${results.findings.length > 0 ? `
  <div class="findings">
    <h2>Findings</h2>
    ${results.findings.map((f, i) => `
    <div class="finding finding-${f.severity}">
      <div class="finding-header">
        <span class="finding-type">${i + 1}. ${f.type.replace(/_/g, ' ')}</span>
        <span class="badge" style="background: ${severityColors[f.severity]}">${f.severity}</span>
      </div>
      <p>${f.description}</p>
      <div class="statement">"${f.statement1}"</div>
      <div class="statement">"${f.statement2}"</div>
      <small>Confidence: ${(f.confidence * 100).toFixed(1)}%${f.verified ? ' | ✅ Triple Verified' : ''}</small>
    </div>
    `).join('')}
  </div>
  ` : '<div class="findings"><h2>✅ No contradictions detected</h2></div>'}

  <div class="footer">
    <p>Verum Omnis Contradiction Engine v1.0.0</p>
    <p>Document Hash: ${results.documentHash || 'N/A'}</p>
  </div>
</body>
</html>`;
  }
}

export default ReportGenerator;
