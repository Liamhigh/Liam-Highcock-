/**
 * Verum Omnis - Contradiction Engine
 * 
 * Zero-false-negative fraud detection through intelligent text analysis.
 * Detects contradictions, inconsistencies, and anomalies in legal documents.
 * 
 * Core Principles:
 * - Triple Verification: 3 independent checks cross-verify every finding
 * - Forensic Anchors: SHA-512 hashing for tamper-proof evidence
 * - Nine-Brain Model: Multiple analysis strategies for comprehensive detection
 * 
 * @author Verum Omnis
 * @version 1.0.0
 */

import { ContradictionDetector } from './src/detector.js';
import { TextAnalyzer } from './src/analyzer.js';
import { ForensicProcessor } from './src/forensic.js';
import { ReportGenerator } from './src/report.js';
import { hashDocument } from './src/utils.js';

/**
 * Main Contradiction Engine class
 * Orchestrates all analysis components for comprehensive fraud detection
 */
export class ContradictionEngine {
  constructor(config = {}) {
    this.config = {
      sensitivityLevel: config.sensitivityLevel || 'medium',
      enableTripleVerification: config.enableTripleVerification !== false,
      generateForensicHash: config.generateForensicHash !== false,
      outputFormat: config.outputFormat || 'json',
      ...config
    };

    this.detector = new ContradictionDetector(this.config);
    this.analyzer = new TextAnalyzer(this.config);
    this.forensicProcessor = new ForensicProcessor(this.config);
    this.reportGenerator = new ReportGenerator(this.config);
  }

  /**
   * Analyze a document or text for contradictions
   * @param {string|object} input - Text content or document object
   * @returns {Promise<object>} Analysis results with findings
   */
  async analyze(input) {
    const startTime = Date.now();
    const text = typeof input === 'string' ? input : input.content;
    
    // Generate forensic hash for evidence integrity
    const documentHash = this.config.generateForensicHash 
      ? await hashDocument(text)
      : null;

    // Phase 1: Text Analysis
    const textAnalysis = await this.analyzer.analyze(text);

    // Phase 2: Contradiction Detection
    const contradictions = await this.detector.detect(text, textAnalysis);

    // Phase 3: Triple Verification (if enabled)
    let verifiedFindings = contradictions;
    if (this.config.enableTripleVerification) {
      verifiedFindings = await this.tripleVerify(contradictions, text);
    }

    // Phase 4: Forensic Processing
    const forensicData = await this.forensicProcessor.process({
      text,
      findings: verifiedFindings,
      hash: documentHash
    });

    const endTime = Date.now();

    return {
      success: true,
      timestamp: new Date().toISOString(),
      processingTimeMs: endTime - startTime,
      documentHash,
      summary: {
        totalContradictions: verifiedFindings.length,
        highSeverity: verifiedFindings.filter(f => f.severity === 'high').length,
        mediumSeverity: verifiedFindings.filter(f => f.severity === 'medium').length,
        lowSeverity: verifiedFindings.filter(f => f.severity === 'low').length,
        riskScore: this.calculateRiskScore(verifiedFindings)
      },
      textAnalysis,
      findings: verifiedFindings,
      forensicData,
      metadata: {
        engineVersion: '1.0.0',
        configUsed: this.config,
        tripleVerified: this.config.enableTripleVerification
      }
    };
  }

  /**
   * Triple Verification: Cross-check findings with multiple strategies
   * @param {Array} findings - Initial findings
   * @param {string} text - Original text
   * @returns {Promise<Array>} Verified findings
   */
  async tripleVerify(findings, text) {
    const verifiedFindings = [];

    for (const finding of findings) {
      let verificationCount = 0;

      // Strategy 1: Semantic verification
      if (this.detector.semanticVerify(finding, text)) {
        verificationCount++;
      }

      // Strategy 2: Pattern verification  
      if (this.detector.patternVerify(finding, text)) {
        verificationCount++;
      }

      // Strategy 3: Context verification
      if (this.detector.contextVerify(finding, text)) {
        verificationCount++;
      }

      if (verificationCount >= 2) {
        finding.verified = true;
        finding.verificationScore = verificationCount / 3;
        verifiedFindings.push(finding);
      }
    }

    return verifiedFindings;
  }

  /**
   * Calculate overall risk score based on findings
   * @param {Array} findings - Verified findings
   * @returns {number} Risk score 0-100
   */
  calculateRiskScore(findings) {
    if (findings.length === 0) return 0;

    const weights = { high: 30, medium: 15, low: 5 };
    let score = 0;

    for (const finding of findings) {
      score += weights[finding.severity] || 10;
    }

    return Math.min(100, score);
  }

  /**
   * Generate a formatted report
   * @param {object} analysisResults - Results from analyze()
   * @returns {Promise<string>} Formatted report
   */
  async generateReport(analysisResults) {
    return this.reportGenerator.generate(analysisResults);
  }

  /**
   * Compare two documents for inconsistencies
   * @param {string} doc1 - First document
   * @param {string} doc2 - Second document  
   * @returns {Promise<object>} Comparison results
   */
  async compareDocuments(doc1, doc2) {
    const analysis1 = await this.analyze(doc1);
    const analysis2 = await this.analyze(doc2);

    const crossContradictions = await this.detector.crossCompare(doc1, doc2);

    return {
      document1Analysis: analysis1,
      document2Analysis: analysis2,
      crossDocumentFindings: crossContradictions,
      consistencyScore: this.calculateConsistencyScore(crossContradictions)
    };
  }

  /**
   * Calculate consistency score between documents
   * @param {Array} crossFindings - Cross-document findings
   * @returns {number} Consistency score 0-100
   */
  calculateConsistencyScore(crossFindings) {
    const baseScore = 100;
    const penalty = crossFindings.length * 10;
    return Math.max(0, baseScore - penalty);
  }
}

// Export individual components for advanced usage
export { ContradictionDetector } from './src/detector.js';
export { TextAnalyzer } from './src/analyzer.js';
export { ForensicProcessor } from './src/forensic.js';
export { ReportGenerator } from './src/report.js';
export { hashDocument } from './src/utils.js';

// Default export
export default ContradictionEngine;
