# Monitoring Setup Guide

This guide helps you set up production monitoring for Verum Omnis.

## Error Tracking with Sentry

### 1. Create Sentry Account

1. Go to https://sentry.io and sign up
2. Create a new project
3. Select "Browser" as the platform
4. Copy your DSN

### 2. Configure Sentry

Add to your `.env` file:

```bash
VITE_SENTRY_DSN=https://xxxxx@xxxxx.ingest.sentry.io/xxxxx
VITE_SENTRY_ENVIRONMENT=production
```

### 3. Initialize in Your App

In your main application file (e.g., `main.jsx` or `App.jsx`):

```javascript
import { initSentry } from './sentry-init.js';

// Initialize Sentry before rendering
initSentry();

// Rest of your app initialization
```

### 4. Test Error Tracking

Trigger a test error:

```javascript
import { captureException } from './sentry-init.js';

try {
  throw new Error('Test error');
} catch (error) {
  captureException(error, { test: true });
}
```

### 5. Verify in Sentry Dashboard

Check your Sentry dashboard to see the error appear.

---

## Performance Monitoring with Firebase

### 1. Set Up Firebase Performance

```bash
npm install firebase
```

### 2. Initialize Firebase

Create `firebase-init.js`:

```javascript
import { initializeApp } from 'firebase/app';
import { getPerformance } from 'firebase/performance';
import { getAnalytics } from 'firebase/analytics';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Performance Monitoring
const perf = getPerformance(app);

// Initialize Analytics
const analytics = getAnalytics(app);

export { app, perf, analytics };
```

### 3. Add to .env

```bash
VITE_FIREBASE_API_KEY=your_api_key
VITE_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=your-project-id
VITE_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=123456789
VITE_FIREBASE_APP_ID=1:123456789:web:abcdef
VITE_FIREBASE_MEASUREMENT_ID=G-XXXXXXXXXX
```

---

## Uptime Monitoring

### Option 1: UptimeRobot (Free)

1. Go to https://uptimerobot.com
2. Add new monitor
3. Enter: https://verumglobal.foundation
4. Set check interval: 5 minutes
5. Add email alert

### Option 2: Firebase Hosting

Firebase automatically monitors uptime. Check:
- Firebase Console > Hosting > Usage

### Option 3: Google Cloud Monitoring

If using Google Cloud:
1. Go to Cloud Console > Monitoring
2. Create uptime check
3. Configure alerts

---

## Analytics with Google Analytics 4

### 1. Create GA4 Property

1. Go to https://analytics.google.com
2. Create new property
3. Select "Web" data stream
4. Copy Measurement ID

### 2. Add to index.html

```html
<!-- Google Analytics -->
<script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-XXXXXXXXXX');
</script>
```

Or initialize programmatically:

```javascript
import { analytics } from './firebase-init.js';
import { logEvent } from 'firebase/analytics';

// Track custom events
logEvent(analytics, 'page_view', {
  page_title: document.title,
  page_location: window.location.href
});
```

---

## Logging Strategy

### Production Logging

Create `logger.js`:

```javascript
const isDev = import.meta.env.MODE !== 'production';

export const logger = {
  info: (message, data) => {
    if (isDev) {
      console.log(`[INFO] ${message}`, data);
    }
    // In production, send to logging service
  },
  
  warn: (message, data) => {
    if (isDev) {
      console.warn(`[WARN] ${message}`, data);
    }
    // In production, send to logging service
  },
  
  error: (message, error, data) => {
    if (isDev) {
      console.error(`[ERROR] ${message}`, error, data);
    }
    // In production, send to Sentry
    import('./sentry-init.js').then(({ captureException }) => {
      captureException(error, { message, ...data });
    });
  },
};
```

---

## Monitoring Checklist

### Setup Checklist

- [ ] Sentry error tracking configured
- [ ] Firebase Performance Monitoring enabled
- [ ] Uptime monitoring configured (UptimeRobot or similar)
- [ ] Google Analytics 4 tracking code added
- [ ] Email alerts configured for critical errors
- [ ] Slack/Discord webhook for alerts (optional)

### Regular Monitoring

- [ ] Check Sentry dashboard daily
- [ ] Review Firebase Performance weekly
- [ ] Monitor uptime status
- [ ] Review analytics monthly
- [ ] Set up automated reports

---

## Alerting Rules

### Critical Alerts (Immediate)

- Error rate > 5% of requests
- Uptime < 99.9%
- Response time > 5 seconds
- Server errors (500+)

### Warning Alerts (Review within 24h)

- Error rate > 1%
- Response time > 2 seconds
- Unusual traffic patterns
- Failed deployments

### Info Alerts (Review weekly)

- Performance degradation trends
- Increased error types
- New error patterns

---

## Dashboards

### Recommended Dashboard Tools

1. **Sentry** - Errors and performance
2. **Firebase Console** - Hosting, performance, analytics
3. **Google Analytics** - User behavior
4. **UptimeRobot** - Availability

### Custom Dashboard (Optional)

Use tools like:
- Grafana
- Datadog
- New Relic
- Application Insights

---

## Cost Estimates

### Free Tier Limits

| Service | Free Tier |
|---------|-----------|
| Sentry | 5,000 events/month |
| Firebase | 10GB storage, 360MB/day downloads |
| UptimeRobot | 50 monitors, 5-min checks |
| Google Analytics | Unlimited (standard) |

### Paid Plans (if needed)

| Service | Entry Plan |
|---------|------------|
| Sentry Team | $26/month |
| Firebase Blaze | Pay-as-you-go |
| UptimeRobot Pro | $7/month |

---

## Testing Your Monitoring

### 1. Test Error Tracking

```javascript
// Trigger test error
throw new Error('Test error - monitoring check');
```

### 2. Test Performance Tracking

```javascript
// Trigger performance measurement
const start = performance.now();
// Do some work
const duration = performance.now() - start;
console.log('Operation took:', duration, 'ms');
```

### 3. Test Uptime Monitor

- Visit your site
- Verify uptime checker can access it
- Test alert by temporarily taking site offline

---

## Support

For monitoring issues:
- Sentry: https://docs.sentry.io
- Firebase: https://firebase.google.com/docs
- Contact: liam@verumglobal.foundation

---

**Last Updated:** 2025-11-26
