/**
 * Contradiction Detector
 * Core detection logic for identifying contradictions and inconsistencies
 */

export class ContradictionDetector {
  constructor(config = {}) {
    this.config = config;
    this.sensitivityLevel = config.sensitivityLevel || 'medium';
    
    // Contradiction patterns to detect
    this.patterns = {
      temporal: [
        /(\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4})/g,
        /((january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{1,2},?\s+\d{4})/gi,
        /(before|after|during|prior to|following)\s+/gi
      ],
      numerical: [
        /\$[\d,]+(\.\d{2})?/g,
        /\b\d+(\.\d+)?\s*(percent|%|dollars|usd|eur|gbp)\b/gi,
        /\b(one|two|three|four|five|six|seven|eight|nine|ten|\d+)\s+(times?|occasions?|instances?)\b/gi
      ],
      negation: [
        /\b(never|not|no|none|neither|nor|nobody|nothing|nowhere)\b/gi,
        /\b(did not|didn't|does not|doesn't|was not|wasn't|were not|weren't|have not|haven't|has not|hasn't)\b/gi
      ],
      certainty: [
        /\b(always|definitely|certainly|absolutely|never|impossible)\b/gi,
        /\b(maybe|perhaps|possibly|might|could|uncertain|unclear)\b/gi
      ]
    };
  }

  /**
   * Detect contradictions in text
   * @param {string} text - Text to analyze
   * @param {object} textAnalysis - Pre-computed text analysis
   * @returns {Promise<Array>} Detected contradictions
   */
  async detect(text, textAnalysis) {
    const findings = [];
    const sentences = this.splitIntoSentences(text);

    // Detect temporal contradictions
    const temporalFindings = this.detectTemporalContradictions(sentences);
    findings.push(...temporalFindings);

    // Detect numerical contradictions
    const numericalFindings = this.detectNumericalContradictions(sentences);
    findings.push(...numericalFindings);

    // Detect logical contradictions
    const logicalFindings = this.detectLogicalContradictions(sentences);
    findings.push(...logicalFindings);

    // Detect statement contradictions
    const statementFindings = this.detectStatementContradictions(sentences);
    findings.push(...statementFindings);

    return this.rankBySeverity(findings);
  }

  /**
   * Split text into sentences
   * @param {string} text - Input text
   * @returns {Array} Array of sentence objects
   */
  splitIntoSentences(text) {
    const sentenceRegex = /[^.!?]+[.!?]+/g;
    const matches = text.match(sentenceRegex) || [text];
    
    return matches.map((sentence, index) => ({
      text: sentence.trim(),
      index,
      position: text.indexOf(sentence)
    }));
  }

  /**
   * Detect temporal contradictions (date/time inconsistencies)
   */
  detectTemporalContradictions(sentences) {
    const findings = [];
    const temporalStatements = [];

    for (const sentence of sentences) {
      for (const pattern of this.patterns.temporal) {
        const matches = sentence.text.match(pattern);
        if (matches) {
          temporalStatements.push({
            sentence,
            matches,
            type: 'temporal'
          });
        }
      }
    }

    // Compare temporal statements for contradictions
    for (let i = 0; i < temporalStatements.length; i++) {
      for (let j = i + 1; j < temporalStatements.length; j++) {
        const contradiction = this.compareTemporalStatements(
          temporalStatements[i],
          temporalStatements[j]
        );
        if (contradiction) {
          findings.push(contradiction);
        }
      }
    }

    return findings;
  }

  /**
   * Compare two temporal statements for contradictions
   */
  compareTemporalStatements(stmt1, stmt2) {
    // Check for conflicting temporal markers
    const beforeAfterPattern = /(before|after|prior to|following)/i;
    const match1 = stmt1.sentence.text.match(beforeAfterPattern);
    const match2 = stmt2.sentence.text.match(beforeAfterPattern);

    if (match1 && match2) {
      const isConflict = (match1[0].toLowerCase().includes('before') && 
                          match2[0].toLowerCase().includes('after')) ||
                         (match1[0].toLowerCase().includes('after') && 
                          match2[0].toLowerCase().includes('before'));
      
      if (isConflict) {
        return {
          type: 'temporal_contradiction',
          severity: 'high',
          statement1: stmt1.sentence.text,
          statement2: stmt2.sentence.text,
          position1: stmt1.sentence.position,
          position2: stmt2.sentence.position,
          description: 'Conflicting temporal references detected',
          confidence: 0.85
        };
      }
    }

    return null;
  }

  /**
   * Detect numerical contradictions
   */
  detectNumericalContradictions(sentences) {
    const findings = [];
    const numericalStatements = [];

    for (const sentence of sentences) {
      for (const pattern of this.patterns.numerical) {
        const matches = sentence.text.match(pattern);
        if (matches) {
          numericalStatements.push({
            sentence,
            matches,
            values: this.extractNumericValues(matches)
          });
        }
      }
    }

    // Look for same-context different-value contradictions
    for (let i = 0; i < numericalStatements.length; i++) {
      for (let j = i + 1; j < numericalStatements.length; j++) {
        const contradiction = this.compareNumericalStatements(
          numericalStatements[i],
          numericalStatements[j]
        );
        if (contradiction) {
          findings.push(contradiction);
        }
      }
    }

    return findings;
  }

  /**
   * Extract numeric values from matches
   */
  extractNumericValues(matches) {
    return matches.map(m => {
      const num = m.replace(/[$,%a-z\s]/gi, '').replace(/,/g, '');
      return parseFloat(num) || m;
    });
  }

  /**
   * Compare numerical statements
   */
  compareNumericalStatements(stmt1, stmt2) {
    // Check if statements discuss similar topics with different values
    const similarity = this.calculateTextSimilarity(
      stmt1.sentence.text,
      stmt2.sentence.text
    );

    if (similarity > 0.5) {
      const valuesDiffer = !this.arraysEqual(stmt1.values, stmt2.values);
      
      if (valuesDiffer) {
        return {
          type: 'numerical_contradiction',
          severity: 'high',
          statement1: stmt1.sentence.text,
          statement2: stmt2.sentence.text,
          values1: stmt1.values,
          values2: stmt2.values,
          position1: stmt1.sentence.position,
          position2: stmt2.sentence.position,
          description: 'Conflicting numerical values in similar context',
          confidence: similarity
        };
      }
    }

    return null;
  }

  /**
   * Detect logical contradictions (negation conflicts)
   */
  detectLogicalContradictions(sentences) {
    const findings = [];

    for (let i = 0; i < sentences.length; i++) {
      for (let j = i + 1; j < sentences.length; j++) {
        const contradiction = this.checkLogicalContradiction(
          sentences[i],
          sentences[j]
        );
        if (contradiction) {
          findings.push(contradiction);
        }
      }
    }

    return findings;
  }

  /**
   * Check for logical contradiction between two sentences
   */
  checkLogicalContradiction(sentence1, sentence2) {
    const negationPattern = /\b(never|not|no|none|didn't|doesn't|wasn't|weren't|haven't|hasn't|cannot|can't|won't|wouldn't)\b/gi;
    
    const hasNegation1 = negationPattern.test(sentence1.text);
    negationPattern.lastIndex = 0; // Reset regex
    const hasNegation2 = negationPattern.test(sentence2.text);

    // Calculate semantic similarity
    const similarity = this.calculateTextSimilarity(
      sentence1.text.replace(negationPattern, ''),
      sentence2.text.replace(negationPattern, '')
    );

    if (similarity > 0.6 && hasNegation1 !== hasNegation2) {
      return {
        type: 'logical_contradiction',
        severity: 'high',
        statement1: sentence1.text,
        statement2: sentence2.text,
        position1: sentence1.position,
        position2: sentence2.position,
        description: 'Contradictory statements with negation conflict',
        confidence: similarity
      };
    }

    return null;
  }

  /**
   * Detect statement contradictions (conflicting claims)
   */
  detectStatementContradictions(sentences) {
    const findings = [];
    const certaintyStatements = [];

    for (const sentence of sentences) {
      for (const pattern of this.patterns.certainty) {
        if (pattern.test(sentence.text)) {
          certaintyStatements.push({
            sentence,
            isCertain: /\b(always|definitely|certainly|absolutely|never|impossible)\b/i.test(sentence.text),
            isUncertain: /\b(maybe|perhaps|possibly|might|could|uncertain|unclear)\b/i.test(sentence.text)
          });
        }
      }
    }

    // Check for certainty conflicts about same topic
    for (let i = 0; i < certaintyStatements.length; i++) {
      for (let j = i + 1; j < certaintyStatements.length; j++) {
        const stmt1 = certaintyStatements[i];
        const stmt2 = certaintyStatements[j];

        const similarity = this.calculateTextSimilarity(
          stmt1.sentence.text,
          stmt2.sentence.text
        );

        if (similarity > 0.5 && stmt1.isCertain && stmt2.isUncertain) {
          findings.push({
            type: 'certainty_contradiction',
            severity: 'medium',
            statement1: stmt1.sentence.text,
            statement2: stmt2.sentence.text,
            position1: stmt1.sentence.position,
            position2: stmt2.sentence.position,
            description: 'Conflicting certainty levels about similar topic',
            confidence: similarity
          });
        }
      }
    }

    return findings;
  }

  /**
   * Calculate text similarity using word overlap
   */
  calculateTextSimilarity(text1, text2) {
    const words1 = new Set(text1.toLowerCase().split(/\W+/).filter(w => w.length > 3));
    const words2 = new Set(text2.toLowerCase().split(/\W+/).filter(w => w.length > 3));
    
    const intersection = new Set([...words1].filter(w => words2.has(w)));
    const union = new Set([...words1, ...words2]);

    return union.size > 0 ? intersection.size / union.size : 0;
  }

  /**
   * Check if arrays are equal
   */
  arraysEqual(arr1, arr2) {
    if (arr1.length !== arr2.length) return false;
    return arr1.every((val, idx) => val === arr2[idx]);
  }

  /**
   * Rank findings by severity
   */
  rankBySeverity(findings) {
    const severityOrder = { high: 0, medium: 1, low: 2 };
    return findings.sort((a, b) => 
      (severityOrder[a.severity] || 3) - (severityOrder[b.severity] || 3)
    );
  }

  /**
   * Semantic verification for triple verification
   */
  semanticVerify(finding, text) {
    // Verify the finding makes semantic sense
    return finding.confidence > 0.6;
  }

  /**
   * Pattern verification for triple verification
   */
  patternVerify(finding, text) {
    // Verify finding matches expected patterns
    return text.includes(finding.statement1) && 
           text.includes(finding.statement2);
  }

  /**
   * Context verification for triple verification
   */
  contextVerify(finding, text) {
    // Verify statements are in relevant context
    const context1 = this.getContext(text, finding.position1);
    const context2 = this.getContext(text, finding.position2);
    return context1.length > 0 && context2.length > 0;
  }

  /**
   * Get surrounding context for a position
   */
  getContext(text, position, windowSize = 100) {
    const start = Math.max(0, position - windowSize);
    const end = Math.min(text.length, position + windowSize);
    return text.substring(start, end);
  }

  /**
   * Cross-compare two documents
   */
  async crossCompare(doc1, doc2) {
    const sentences1 = this.splitIntoSentences(doc1);
    const sentences2 = this.splitIntoSentences(doc2);
    const findings = [];

    for (const s1 of sentences1) {
      for (const s2 of sentences2) {
        const contradiction = this.checkLogicalContradiction(s1, s2);
        if (contradiction) {
          contradiction.crossDocument = true;
          findings.push(contradiction);
        }
      }
    }

    return this.rankBySeverity(findings);
  }
}

export default ContradictionDetector;
