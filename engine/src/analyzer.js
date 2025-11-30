/**
 * Text Analyzer
 * Pre-processing and linguistic analysis of documents
 */

export class TextAnalyzer {
  constructor(config = {}) {
    this.config = config;
  }

  /**
   * Analyze text and extract linguistic features
   * @param {string} text - Input text
   * @returns {Promise<object>} Analysis results
   */
  async analyze(text) {
    const sentences = this.extractSentences(text);
    const words = this.extractWords(text);
    const entities = this.extractEntities(text);
    const statistics = this.calculateStatistics(text, sentences, words);

    return {
      sentences,
      words,
      entities,
      statistics,
      structure: this.analyzeStructure(text),
      sentiment: this.analyzeSentiment(text)
    };
  }

  /**
   * Extract sentences from text
   */
  extractSentences(text) {
    const sentenceRegex = /[^.!?]+[.!?]+/g;
    const matches = text.match(sentenceRegex) || [text];
    
    return matches.map((sentence, index) => ({
      text: sentence.trim(),
      index,
      wordCount: sentence.trim().split(/\s+/).length,
      position: text.indexOf(sentence)
    }));
  }

  /**
   * Extract and count words
   */
  extractWords(text) {
    const words = text.toLowerCase()
      .replace(/[^\w\s]/g, '')
      .split(/\s+/)
      .filter(word => word.length > 0);

    const wordFrequency = {};
    for (const word of words) {
      wordFrequency[word] = (wordFrequency[word] || 0) + 1;
    }

    return {
      total: words.length,
      unique: Object.keys(wordFrequency).length,
      frequency: wordFrequency,
      topWords: Object.entries(wordFrequency)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 20)
        .map(([word, count]) => ({ word, count }))
    };
  }

  /**
   * Extract named entities (dates, amounts, names, etc.)
   */
  extractEntities(text) {
    const entities = {
      dates: [],
      amounts: [],
      names: [],
      locations: [],
      organizations: []
    };

    // Extract dates
    const datePatterns = [
      /\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4}/g,
      /(january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{1,2},?\s+\d{4}/gi,
      /\d{4}[\/\-]\d{1,2}[\/\-]\d{1,2}/g
    ];
    
    for (const pattern of datePatterns) {
      const matches = text.match(pattern);
      if (matches) {
        entities.dates.push(...matches);
      }
    }

    // Extract monetary amounts
    const amountPattern = /\$[\d,]+(\.\d{2})?|\d+(\.\d{2})?\s*(dollars|usd|eur|gbp)/gi;
    const amountMatches = text.match(amountPattern);
    if (amountMatches) {
      entities.amounts.push(...amountMatches);
    }

    // Extract potential names (capitalized words)
    const namePattern = /\b[A-Z][a-z]+\s+[A-Z][a-z]+\b/g;
    const nameMatches = text.match(namePattern);
    if (nameMatches) {
      entities.names.push(...new Set(nameMatches));
    }

    return entities;
  }

  /**
   * Calculate text statistics
   */
  calculateStatistics(text, sentences, words) {
    const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim().length > 0);
    
    return {
      characterCount: text.length,
      characterCountNoSpaces: text.replace(/\s/g, '').length,
      wordCount: words.total,
      uniqueWordCount: words.unique,
      sentenceCount: sentences.length,
      paragraphCount: paragraphs.length,
      averageWordLength: text.replace(/\s/g, '').length / words.total || 0,
      averageSentenceLength: words.total / sentences.length || 0,
      lexicalDiversity: words.unique / words.total || 0,
      readabilityScore: this.calculateReadability(text, sentences.length, words.total)
    };
  }

  /**
   * Calculate readability score (Flesch-Kincaid approximation)
   */
  calculateReadability(text, sentenceCount, wordCount) {
    if (sentenceCount === 0 || wordCount === 0) return 0;
    
    const syllables = this.countSyllables(text);
    const score = 206.835 - 1.015 * (wordCount / sentenceCount) - 84.6 * (syllables / wordCount);
    
    return Math.max(0, Math.min(100, Math.round(score)));
  }

  /**
   * Estimate syllable count
   */
  countSyllables(text) {
    const words = text.toLowerCase().match(/\b[a-z]+\b/g) || [];
    let count = 0;
    
    for (const word of words) {
      count += this.countWordSyllables(word);
    }
    
    return count;
  }

  /**
   * Count syllables in a single word
   */
  countWordSyllables(word) {
    word = word.toLowerCase();
    if (word.length <= 3) return 1;
    
    word = word.replace(/(?:[^laeiouy]es|ed|[^laeiouy]e)$/, '');
    word = word.replace(/^y/, '');
    
    const matches = word.match(/[aeiouy]{1,2}/g);
    return matches ? matches.length : 1;
  }

  /**
   * Analyze document structure
   */
  analyzeStructure(text) {
    const structure = {
      hasHeaders: /^#{1,6}\s+|^[A-Z][A-Za-z\s]+:\s*$/m.test(text),
      hasBulletPoints: /^[\s]*[-*•]\s+/m.test(text),
      hasNumberedList: /^[\s]*\d+[\.\)]\s+/m.test(text),
      hasTables: /\|.*\|/.test(text),
      hasQuotes: /"[^"]+"|'[^']+'/.test(text),
      sections: this.identifySections(text)
    };

    return structure;
  }

  /**
   * Identify document sections
   */
  identifySections(text) {
    const sections = [];
    const headerPattern = /^(?:#{1,6}\s+(.+)|([A-Z][A-Za-z\s]+):?\s*)$/gm;
    
    let match;
    while ((match = headerPattern.exec(text)) !== null) {
      sections.push({
        title: (match[1] || match[2]).trim(),
        position: match.index
      });
    }

    return sections;
  }

  /**
   * Basic sentiment analysis
   */
  analyzeSentiment(text) {
    const positiveWords = [
      'good', 'great', 'excellent', 'positive', 'agree', 'correct', 'true',
      'confirmed', 'approved', 'success', 'benefit', 'advantage', 'valid'
    ];
    
    const negativeWords = [
      'bad', 'poor', 'negative', 'disagree', 'incorrect', 'false', 'fraud',
      'denied', 'rejected', 'failure', 'problem', 'issue', 'invalid', 'contradiction'
    ];

    const words = text.toLowerCase().split(/\W+/);
    let positiveCount = 0;
    let negativeCount = 0;

    for (const word of words) {
      if (positiveWords.includes(word)) positiveCount++;
      if (negativeWords.includes(word)) negativeCount++;
    }

    const total = positiveCount + negativeCount;
    let sentiment = 'neutral';
    let score = 0;

    if (total > 0) {
      score = (positiveCount - negativeCount) / total;
      if (score > 0.2) sentiment = 'positive';
      else if (score < -0.2) sentiment = 'negative';
    }

    return {
      sentiment,
      score,
      positiveCount,
      negativeCount
    };
  }
}

export default TextAnalyzer;
