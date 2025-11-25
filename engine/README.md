# Verum Omnis - Contradiction Engine

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Legal%20AI-Fraud%20Detection-purple?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Triple%20Verified-✓-green?style=for-the-badge" />
</p>

The **Contradiction Engine** is the core analysis module of Verum Omnis — the world's first constitutional legal AI firewall. It provides zero-false-negative fraud detection through intelligent text analysis.

## 🎯 Features

- **🔍 Contradiction Detection**: Identifies temporal, numerical, logical, and certainty contradictions
- **✅ Triple Verification**: 3 independent verification strategies cross-check every finding
- **🔗 Forensic Anchoring**: SHA-512 hashing for tamper-proof evidence integrity
- **📊 Multi-Format Reports**: JSON, Text, Markdown, and HTML output formats
- **📄 Document Comparison**: Cross-document analysis for inconsistencies

## 🚀 Quick Start

### Installation

```bash
cd engine
npm install
```

### Basic Usage

```javascript
import { ContradictionEngine } from './index.js';

const engine = new ContradictionEngine();

const text = `
  The meeting occurred before January 15, 2024.
  John confirmed the meeting happened after January 20, 2024.
`;

const results = await engine.analyze(text);
console.log(results.summary);
// { totalContradictions: 1, highSeverity: 1, riskScore: 30 }
```

### CLI Usage

```bash
# Analyze a file
node cli.js --file document.txt

# Analyze text directly
node cli.js --text "Sample text to analyze"

# Generate markdown report
node cli.js --file doc.txt --format markdown --output report.md

# Compare two documents
node cli.js --file doc1.txt --compare doc2.txt
```

## ⚙️ Configuration

```javascript
const engine = new ContradictionEngine({
  sensitivityLevel: 'high',      // 'low' | 'medium' | 'high'
  enableTripleVerification: true, // Enable 3-way verification
  generateForensicHash: true,     // Generate SHA-512 document hash
  outputFormat: 'json'            // 'json' | 'text' | 'markdown' | 'html'
});
```

## 📋 Detection Types

| Type | Description | Severity |
|------|-------------|----------|
| `temporal_contradiction` | Conflicting dates/times | High |
| `numerical_contradiction` | Inconsistent amounts/values | High |
| `logical_contradiction` | Negation conflicts | High |
| `certainty_contradiction` | Conflicting certainty levels | Medium |

## 🔬 Analysis Pipeline

1. **Text Analysis**: Extract sentences, entities, and linguistic features
2. **Contradiction Detection**: Apply pattern matching and semantic analysis
3. **Triple Verification**: Cross-verify findings with 3 independent strategies
4. **Forensic Processing**: Generate audit trail and evidence chain
5. **Report Generation**: Format results for output

## 📊 Output Structure

```javascript
{
  success: true,
  timestamp: "2024-01-15T10:30:00Z",
  processingTimeMs: 45,
  documentHash: "abc123...",
  summary: {
    totalContradictions: 2,
    highSeverity: 1,
    mediumSeverity: 1,
    lowSeverity: 0,
    riskScore: 45
  },
  findings: [
    {
      type: "temporal_contradiction",
      severity: "high",
      statement1: "...",
      statement2: "...",
      confidence: 0.85,
      verified: true
    }
  ],
  forensicData: {
    integrity: { documentHash: "...", verified: true },
    evidenceChain: [...],
    auditTrail: [...]
  }
}
```

## 🧪 Testing

```bash
npm test
```

## 🏛️ Core Principles

This engine implements the Verum Omnis constitutional principles:

- **Zero False Negatives**: Every potential contradiction is detected
- **Triple Verification**: Findings are cross-checked by 3 independent strategies
- **Forensic Anchors**: SHA-512 hashing ensures evidence integrity
- **Transparent Audit**: Complete audit trail for every analysis

## 📜 License

MIT License - Part of the Verum Omnis Legal AI Platform

---

<p align="center">
  <strong>Verum Omnis</strong> — Access to Justice for All
</p>
