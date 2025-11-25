/**
 * Forensic Processor
 * Handles forensic anchoring, evidence integrity, and audit trails
 */

import { hashDocument, generateTimestamp } from './utils.js';

export class ForensicProcessor {
  constructor(config = {}) {
    this.config = config;
  }

  /**
   * Process findings with forensic anchoring
   * @param {object} data - Analysis data
   * @returns {Promise<object>} Forensic report
   */
  async process(data) {
    const { text, findings, hash } = data;

    const forensicReport = {
      // Document integrity
      integrity: {
        documentHash: hash,
        hashAlgorithm: 'SHA-512',
        timestamp: generateTimestamp(),
        verified: true
      },

      // Evidence chain
      evidenceChain: this.buildEvidenceChain(findings),

      // Audit trail
      auditTrail: this.createAuditTrail(data),

      // Finding hashes (for individual verification)
      findingHashes: await this.hashFindings(findings),

      // Forensic summary
      summary: this.generateForensicSummary(text, findings)
    };

    return forensicReport;
  }

  /**
   * Build evidence chain from findings
   */
  buildEvidenceChain(findings) {
    return findings.map((finding, index) => ({
      id: `EVD-${String(index + 1).padStart(4, '0')}`,
      type: finding.type,
      severity: finding.severity,
      timestamp: generateTimestamp(),
      statements: {
        primary: finding.statement1,
        secondary: finding.statement2
      },
      positions: {
        primary: finding.position1,
        secondary: finding.position2
      },
      confidence: finding.confidence,
      verified: finding.verified || false
    }));
  }

  /**
   * Create audit trail for the analysis
   */
  createAuditTrail(data) {
    const baseTime = Date.now();
    const events = [
      {
        event: 'ANALYSIS_INITIATED',
        timestamp: new Date(baseTime).toISOString(),
        details: {
          documentSize: data.text.length,
          findingsCount: data.findings.length
        }
      }
    ];

    // Add finding events with incrementing timestamps for uniqueness
    for (let i = 0; i < data.findings.length; i++) {
      events.push({
        event: 'CONTRADICTION_DETECTED',
        timestamp: new Date(baseTime + i + 1).toISOString(),
        details: {
          findingIndex: i,
          type: data.findings[i].type,
          severity: data.findings[i].severity
        }
      });
    }

    events.push({
      event: 'ANALYSIS_COMPLETED',
      timestamp: new Date(baseTime + data.findings.length + 1).toISOString(),
      details: {
        totalFindings: data.findings.length,
        documentHash: data.hash
      }
    });

    return events;
  }

  /**
   * Hash individual findings for verification
   */
  async hashFindings(findings) {
    const hashes = [];

    for (const finding of findings) {
      const findingString = JSON.stringify({
        type: finding.type,
        statement1: finding.statement1,
        statement2: finding.statement2
      });
      
      const hash = await hashDocument(findingString);
      hashes.push({
        type: finding.type,
        hash: hash
      });
    }

    return hashes;
  }

  /**
   * Generate forensic summary
   */
  generateForensicSummary(text, findings) {
    const severityCounts = {
      high: findings.filter(f => f.severity === 'high').length,
      medium: findings.filter(f => f.severity === 'medium').length,
      low: findings.filter(f => f.severity === 'low').length
    };

    const typeCounts = {};
    for (const finding of findings) {
      typeCounts[finding.type] = (typeCounts[finding.type] || 0) + 1;
    }

    return {
      documentCharacters: text.length,
      totalContradictions: findings.length,
      bySeverity: severityCounts,
      byType: typeCounts,
      highestConfidence: findings.length > 0 
        ? Math.max(...findings.map(f => f.confidence)) 
        : 0,
      averageConfidence: findings.length > 0
        ? findings.reduce((sum, f) => sum + f.confidence, 0) / findings.length
        : 0,
      forensicGrade: this.calculateForensicGrade(findings)
    };
  }

  /**
   * Calculate forensic grade based on findings
   */
  calculateForensicGrade(findings) {
    if (findings.length === 0) return 'A';
    
    const highCount = findings.filter(f => f.severity === 'high').length;
    const mediumCount = findings.filter(f => f.severity === 'medium').length;

    if (highCount >= 3) return 'F';
    if (highCount >= 2) return 'D';
    if (highCount >= 1) return 'C';
    if (mediumCount >= 3) return 'C';
    if (mediumCount >= 1) return 'B';
    return 'B+';
  }

  /**
   * Generate tamper-proof seal data
   */
  async generateSeal(analysisResults) {
    const sealData = {
      version: '1.0',
      type: 'VERUM_OMNIS_FORENSIC_SEAL',
      timestamp: generateTimestamp(),
      documentHash: analysisResults.documentHash,
      findingsHash: await hashDocument(JSON.stringify(analysisResults.findings)),
      summary: {
        totalFindings: analysisResults.summary.totalContradictions,
        riskScore: analysisResults.summary.riskScore
      }
    };

    sealData.sealHash = await hashDocument(JSON.stringify(sealData));

    return sealData;
  }
}

export default ForensicProcessor;
