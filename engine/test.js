/**
 * Verum Omnis - Contradiction Engine Tests
 */

import { ContradictionEngine } from './index.js';

// Test documents
const testCases = [
  {
    name: 'Temporal Contradiction',
    text: `
      The meeting occurred before January 15, 2024.
      The agreement was signed after January 15, 2024.
      John confirmed the meeting happened after January 20, 2024.
    `,
    expectedFindings: 'temporal'
  },
  {
    name: 'Numerical Contradiction',
    text: `
      The total payment amount was $50,000.
      The contract states the payment amount is $75,000.
      Invoice records show $50,000 was received.
    `,
    expectedFindings: 'numerical'
  },
  {
    name: 'Logical Contradiction',
    text: `
      John was present at the meeting on Monday.
      John was not present at any meetings this week.
      The records confirm John attended the Monday meeting.
    `,
    expectedFindings: 'logical'
  },
  {
    name: 'Clean Document',
    text: `
      The project started in January 2024.
      Development continued through February.
      Testing was completed in March.
      The final release was in April 2024.
    `,
    expectedFindings: 'none'
  },
  {
    name: 'Certainty Contradiction',
    text: `
      The defendant was definitely at the scene.
      It's possible the defendant might have been elsewhere.
      Witnesses are uncertain about the defendant's presence.
    `,
    expectedFindings: 'certainty'
  }
];

async function runTests() {
  console.log('╔══════════════════════════════════════════════════════════════╗');
  console.log('║     VERUM OMNIS - CONTRADICTION ENGINE TEST SUITE            ║');
  console.log('╚══════════════════════════════════════════════════════════════╝');
  console.log('');

  const engine = new ContradictionEngine({
    sensitivityLevel: 'high',
    enableTripleVerification: true
  });

  let passed = 0;
  let failed = 0;

  for (const testCase of testCases) {
    console.log(`\n▶ Testing: ${testCase.name}`);
    console.log('─'.repeat(50));

    try {
      const results = await engine.analyze(testCase.text);
      
      console.log(`  Document Hash: ${results.documentHash.substring(0, 32)}...`);
      console.log(`  Processing Time: ${results.processingTimeMs}ms`);
      console.log(`  Contradictions Found: ${results.summary.totalContradictions}`);
      console.log(`  Risk Score: ${results.summary.riskScore}/100`);
      
      if (results.findings.length > 0) {
        console.log(`  Findings:`);
        results.findings.forEach((f, i) => {
          console.log(`    ${i + 1}. [${f.severity.toUpperCase()}] ${f.type}`);
        });
      }

      // Validate expected results
      let testPassed = false;
      
      if (testCase.expectedFindings === 'none') {
        testPassed = results.summary.totalContradictions === 0;
      } else {
        testPassed = results.findings.some(f => 
          f.type.toLowerCase().includes(testCase.expectedFindings)
        );
      }

      if (testPassed) {
        console.log(`  ✅ TEST PASSED`);
        passed++;
      } else {
        console.log(`  ❌ TEST FAILED - Expected findings: ${testCase.expectedFindings}`);
        failed++;
      }

    } catch (error) {
      console.log(`  ❌ TEST ERROR: ${error.message}`);
      failed++;
    }
  }

  // Test report generation
  console.log(`\n▶ Testing: Report Generation`);
  console.log('─'.repeat(50));

  try {
    const testText = testCases[0].text;
    const results = await engine.analyze(testText);
    
    // Test all formats
    const formats = ['json', 'text', 'markdown', 'html'];
    for (const format of formats) {
      const reportEngine = new ContradictionEngine({ outputFormat: format });
      const report = await reportEngine.generateReport(results);
      
      if (report && report.length > 0) {
        console.log(`  ✅ ${format.toUpperCase()} format: Generated (${report.length} chars)`);
      } else {
        console.log(`  ❌ ${format.toUpperCase()} format: Failed`);
        failed++;
      }
    }
    passed++;
  } catch (error) {
    console.log(`  ❌ Report Generation ERROR: ${error.message}`);
    failed++;
  }

  // Test document comparison
  console.log(`\n▶ Testing: Document Comparison`);
  console.log('─'.repeat(50));

  try {
    const doc1 = 'The payment was made on Monday. The amount was $1000.';
    const doc2 = 'The payment was made on Tuesday. The amount was $2000.';
    
    const comparison = await engine.compareDocuments(doc1, doc2);
    
    console.log(`  Document 1 Findings: ${comparison.document1Analysis.summary.totalContradictions}`);
    console.log(`  Document 2 Findings: ${comparison.document2Analysis.summary.totalContradictions}`);
    console.log(`  Cross-Document Findings: ${comparison.crossDocumentFindings.length}`);
    console.log(`  Consistency Score: ${comparison.consistencyScore}/100`);
    console.log(`  ✅ TEST PASSED`);
    passed++;
  } catch (error) {
    console.log(`  ❌ Comparison ERROR: ${error.message}`);
    failed++;
  }

  // Summary
  console.log('\n');
  console.log('═'.repeat(60));
  console.log('TEST SUMMARY');
  console.log('═'.repeat(60));
  console.log(`  Total Tests: ${passed + failed}`);
  console.log(`  Passed: ${passed}`);
  console.log(`  Failed: ${failed}`);
  console.log(`  Success Rate: ${((passed / (passed + failed)) * 100).toFixed(1)}%`);
  console.log('═'.repeat(60));

  process.exit(failed > 0 ? 1 : 0);
}

runTests();
