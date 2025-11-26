/**
 * Sentry Error Tracking Configuration
 * 
 * This file provides error tracking and monitoring for production environments.
 * 
 * Setup Instructions:
 * 1. Create a Sentry account at https://sentry.io
 * 2. Create a new project
 * 3. Copy your DSN from Project Settings
 * 4. Add VITE_SENTRY_DSN to your .env file
 * 5. Import and initialize in your main app file
 * 
 * Usage:
 * import { initSentry } from './sentry-init.js';
 * initSentry();
 */

import * as Sentry from '@sentry/browser';

/**
 * Initialize Sentry error tracking
 * Only runs in production environment when DSN is configured
 */
export function initSentry() {
  // Only initialize in production
  if (import.meta.env.MODE !== 'production') {
    console.log('[Sentry] Skipping initialization in development mode');
    return;
  }

  const dsn = import.meta.env.VITE_SENTRY_DSN;
  
  if (!dsn) {
    console.warn('[Sentry] DSN not configured. Set VITE_SENTRY_DSN in your .env file');
    return;
  }

  try {
    Sentry.init({
      dsn,
      
      // Set environment
      environment: import.meta.env.MODE || 'production',
      
      // Release tracking (optional - set via CI/CD)
      release: import.meta.env.VITE_APP_VERSION || '1.0.0',
      
      // Performance monitoring
      tracesSampleRate: 0.1, // Capture 10% of transactions to balance performance and cost
      
      // Session replay for debugging
      replaysSessionSampleRate: 0.1, // 10% of sessions
      replaysOnErrorSampleRate: 1.0, // 100% of sessions with errors
      
      // Integrations
      integrations: [
        new Sentry.BrowserTracing(),
        new Sentry.Replay({
          maskAllText: true,
          blockAllMedia: true,
        }),
      ],
      
      // Filter out local development errors
      beforeSend(event, hint) {
        // Don't send errors from localhost
        if (window.location.hostname === 'localhost' || 
            window.location.hostname === '127.0.0.1') {
          return null;
        }
        return event;
      },
      
      // Ignore common non-critical errors
      ignoreErrors: [
        // Browser extensions
        'top.GLOBALS',
        'chrome-extension://',
        'moz-extension://',
        // Network errors that are expected
        'NetworkError',
        'Network request failed',
        // Third-party script errors
        'Script error.',
      ],
    });

    console.log('[Sentry] Error tracking initialized');
    
    // Set user context (optional)
    // This should be called after user logs in
    // Sentry.setUser({ id: 'user-id', email: 'user@example.com' });
    
  } catch (error) {
    console.error('[Sentry] Failed to initialize:', error);
  }
}

/**
 * Manually capture an exception
 * @param {Error} error - The error to capture
 * @param {Object} context - Additional context
 */
export function captureException(error, context = {}) {
  if (import.meta.env.MODE !== 'production') {
    console.error('[Dev Error]', error, context);
    return;
  }
  
  Sentry.captureException(error, {
    contexts: { custom: context },
  });
}

/**
 * Manually capture a message
 * @param {string} message - The message to capture
 * @param {string} level - Severity level (error, warning, info, debug)
 */
export function captureMessage(message, level = 'info') {
  if (import.meta.env.MODE !== 'production') {
    console.log(`[Dev ${level}]`, message);
    return;
  }
  
  Sentry.captureMessage(message, level);
}

/**
 * Set user context
 * Call this after user authentication
 * @param {Object} user - User information
 */
export function setUser(user) {
  if (import.meta.env.MODE !== 'production') {
    return;
  }
  
  Sentry.setUser({
    id: user.id,
    email: user.email,
    username: user.username,
  });
}

/**
 * Clear user context
 * Call this on logout
 */
export function clearUser() {
  if (import.meta.env.MODE !== 'production') {
    return;
  }
  
  Sentry.setUser(null);
}

/**
 * Add breadcrumb for debugging
 * @param {Object} breadcrumb - Breadcrumb data
 */
export function addBreadcrumb(breadcrumb) {
  if (import.meta.env.MODE !== 'production') {
    console.log('[Breadcrumb]', breadcrumb);
    return;
  }
  
  Sentry.addBreadcrumb(breadcrumb);
}
