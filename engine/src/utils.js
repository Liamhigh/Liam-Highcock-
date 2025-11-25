/**
 * Utility functions for the Contradiction Engine
 */

/**
 * Generate SHA-512 hash of document content
 * @param {string} content - Content to hash
 * @returns {Promise<string>} Hex hash string
 */
export async function hashDocument(content) {
  // Use Web Crypto API if available, otherwise use Node.js crypto
  if (typeof crypto !== 'undefined' && crypto.subtle) {
    const encoder = new TextEncoder();
    const data = encoder.encode(content);
    const hashBuffer = await crypto.subtle.digest('SHA-512', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  } else {
    // Node.js environment
    const { createHash } = await import('crypto');
    return createHash('sha512').update(content).digest('hex');
  }
}

/**
 * Generate ISO timestamp
 * @returns {string} ISO timestamp
 */
export function generateTimestamp() {
  return new Date().toISOString();
}

/**
 * Deep clone an object
 * @param {object} obj - Object to clone
 * @returns {object} Cloned object
 */
export function deepClone(obj) {
  return JSON.parse(JSON.stringify(obj));
}

/**
 * Sanitize text input
 * @param {string} text - Text to sanitize
 * @returns {string} Sanitized text
 */
export function sanitizeText(text) {
  if (typeof text !== 'string') {
    throw new Error('Input must be a string');
  }
  return text
    .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '') // Remove control characters
    .trim();
}

/**
 * Validate configuration object
 * @param {object} config - Configuration to validate
 * @returns {object} Validated configuration
 */
export function validateConfig(config) {
  const validatedConfig = { ...config };
  
  // Validate sensitivity level
  const validLevels = ['low', 'medium', 'high'];
  if (!validLevels.includes(validatedConfig.sensitivityLevel)) {
    validatedConfig.sensitivityLevel = 'medium';
  }
  
  // Validate output format
  const validFormats = ['json', 'text', 'markdown', 'html'];
  if (!validFormats.includes(validatedConfig.outputFormat)) {
    validatedConfig.outputFormat = 'json';
  }
  
  return validatedConfig;
}

/**
 * Calculate Levenshtein distance between two strings
 * @param {string} str1 - First string
 * @param {string} str2 - Second string
 * @returns {number} Edit distance
 */
export function levenshteinDistance(str1, str2) {
  const m = str1.length;
  const n = str2.length;
  
  if (m === 0) return n;
  if (n === 0) return m;
  
  const dp = Array(m + 1).fill(null).map(() => Array(n + 1).fill(0));
  
  for (let i = 0; i <= m; i++) dp[i][0] = i;
  for (let j = 0; j <= n; j++) dp[0][j] = j;
  
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      const cost = str1[i - 1] === str2[j - 1] ? 0 : 1;
      dp[i][j] = Math.min(
        dp[i - 1][j] + 1,
        dp[i][j - 1] + 1,
        dp[i - 1][j - 1] + cost
      );
    }
  }
  
  return dp[m][n];
}

/**
 * Calculate string similarity (0 to 1)
 * @param {string} str1 - First string
 * @param {string} str2 - Second string
 * @returns {number} Similarity score
 */
export function stringSimilarity(str1, str2) {
  const distance = levenshteinDistance(str1.toLowerCase(), str2.toLowerCase());
  const maxLength = Math.max(str1.length, str2.length);
  return maxLength === 0 ? 1 : 1 - distance / maxLength;
}

/**
 * Extract key phrases from text
 * @param {string} text - Input text
 * @returns {Array} Key phrases
 */
export function extractKeyPhrases(text) {
  // Common stop words to filter out
  const stopWords = new Set([
    'the', 'a', 'an', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for',
    'of', 'with', 'by', 'from', 'as', 'is', 'was', 'are', 'were', 'been',
    'be', 'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would',
    'could', 'should', 'may', 'might', 'must', 'shall', 'can', 'it',
    'that', 'this', 'these', 'those', 'i', 'you', 'he', 'she', 'we', 'they'
  ]);

  const words = text.toLowerCase()
    .replace(/[^\w\s]/g, '')
    .split(/\s+/)
    .filter(word => word.length > 2 && !stopWords.has(word));

  // Count word frequency
  const frequency = {};
  for (const word of words) {
    frequency[word] = (frequency[word] || 0) + 1;
  }

  // Return top phrases by frequency
  return Object.entries(frequency)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
    .map(([word, count]) => ({ phrase: word, count }));
}

/**
 * Format file size for display
 * @param {number} bytes - Size in bytes
 * @returns {string} Formatted size
 */
export function formatFileSize(bytes) {
  const units = ['B', 'KB', 'MB', 'GB'];
  let unitIndex = 0;
  let size = bytes;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }

  return `${size.toFixed(2)} ${units[unitIndex]}`;
}

export default {
  hashDocument,
  generateTimestamp,
  deepClone,
  sanitizeText,
  validateConfig,
  levenshteinDistance,
  stringSimilarity,
  extractKeyPhrases,
  formatFileSize
};
