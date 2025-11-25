#!/usr/bin/env node

/**
 * Verum Omnis - Contradiction Engine CLI
 * Command-line interface for analyzing documents
 */

import { ContradictionEngine } from './index.js';
import { readFileSync, writeFileSync, existsSync } from 'fs';

const args = process.argv.slice(2);

function printHelp() {
  console.log(`
╔══════════════════════════════════════════════════════════════╗
║          VERUM OMNIS - CONTRADICTION ENGINE CLI              ║
║     Zero-false-negative fraud detection through analysis     ║
╚══════════════════════════════════════════════════════════════╝

Usage: node cli.js [options] <input>

Options:
  --help, -h              Show this help message
  --file, -f <path>       Analyze a file
  --text, -t <text>       Analyze text directly
  --output, -o <path>     Output file for report
  --format <format>       Output format: json, text, markdown, html
  --sensitivity <level>   Detection sensitivity: low, medium, high
  --no-verify            Disable triple verification
  --compare <file>        Compare with another document

Examples:
  node cli.js --file document.txt
  node cli.js --text "Sample text to analyze"
  node cli.js --file doc.txt --format markdown --output report.md
  node cli.js --file doc1.txt --compare doc2.txt
`);
}

async function main() {
  if (args.length === 0 || args.includes('--help') || args.includes('-h')) {
    printHelp();
    process.exit(0);
  }

  const config = {
    sensitivityLevel: 'medium',
    enableTripleVerification: true,
    outputFormat: 'text'
  };

  let inputText = null;
  let compareText = null;
  let outputPath = null;

  // Parse arguments
  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    
    switch (arg) {
      case '--file':
      case '-f':
        const filePath = args[++i];
        if (!existsSync(filePath)) {
          console.error(`Error: File not found: ${filePath}`);
          process.exit(1);
        }
        inputText = readFileSync(filePath, 'utf-8');
        break;
      
      case '--text':
      case '-t':
        inputText = args[++i];
        break;
      
      case '--output':
      case '-o':
        outputPath = args[++i];
        break;
      
      case '--format':
        config.outputFormat = args[++i];
        break;
      
      case '--sensitivity':
        config.sensitivityLevel = args[++i];
        break;
      
      case '--no-verify':
        config.enableTripleVerification = false;
        break;
      
      case '--compare':
        const comparePath = args[++i];
        if (!existsSync(comparePath)) {
          console.error(`Error: Comparison file not found: ${comparePath}`);
          process.exit(1);
        }
        compareText = readFileSync(comparePath, 'utf-8');
        break;
    }
  }

  if (!inputText) {
    console.error('Error: No input provided. Use --file or --text option.');
    process.exit(1);
  }

  console.log('');
  console.log('╔══════════════════════════════════════════════════════════════╗');
  console.log('║          VERUM OMNIS - CONTRADICTION ENGINE                  ║');
  console.log('╚══════════════════════════════════════════════════════════════╝');
  console.log('');
  console.log('Initializing analysis...');

  const engine = new ContradictionEngine(config);

  try {
    let results;

    if (compareText) {
      console.log('Comparing documents...');
      results = await engine.compareDocuments(inputText, compareText);
    } else {
      console.log('Analyzing document...');
      results = await engine.analyze(inputText);
    }

    console.log('Generating report...');
    const report = await engine.generateReport(results);

    if (outputPath) {
      writeFileSync(outputPath, report);
      console.log(`Report saved to: ${outputPath}`);
    } else {
      console.log('');
      console.log(report);
    }

    // Summary output
    console.log('');
    console.log('═══════════════════════════════════════════════════════════════');
    console.log('ANALYSIS COMPLETE');
    console.log(`  Contradictions Found: ${results.summary?.totalContradictions || 0}`);
    console.log(`  Risk Score: ${results.summary?.riskScore || 0}/100`);
    console.log('═══════════════════════════════════════════════════════════════');

  } catch (error) {
    console.error('Error during analysis:', error.message);
    process.exit(1);
  }
}

main();
