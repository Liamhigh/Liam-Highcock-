# Deployment Guide

This guide covers deploying the Verum Omnis application to various platforms.

## Table of Contents

1. [Firebase Hosting](#firebase-hosting)
2. [Web Server Deployment](#web-server-deployment)
3. [Android APK](#android-apk)
4. [Environment Configuration](#environment-configuration)
5. [Pre-Deployment Checklist](#pre-deployment-checklist)

---

## Firebase Hosting

### Prerequisites

- Firebase CLI installed: `npm install -g firebase-tools`
- Firebase project created
- Proper permissions on the Firebase project

### Initial Setup

1. **Login to Firebase:**
   ```bash
   firebase login
   ```

2. **Initialize Firebase (if not done):**
   ```bash
   firebase init hosting
   ```
   - Select your Firebase project
   - Set public directory to: `.` (current directory)
   - Configure as single-page app: No (unless using client-side routing)
   - Setup automatic builds with GitHub: Optional

### Deploy to Firebase

```bash
# Deploy to production
firebase deploy --only hosting

# Deploy to preview channel
firebase hosting:channel:deploy preview

# Deploy with custom message
firebase deploy --only hosting -m "Release v1.0.1"
```

### Firebase Configuration

The `firebase.json` file contains hosting configuration:

```json
{
  "hosting": {
    "public": ".",
    "ignore": ["**/.*", "**/node_modules/**", "firebase.json"],
    "headers": [
      { "source": "**/*.html", "headers": [{ "key": "Cache-Control", "value": "no-store" }] },
      { "source": "**/*.js",   "headers": [{ "key": "Cache-Control", "value": "no-store" }] }
    ]
  }
}
```

### Recommended: Enhanced Headers

Add these headers to `firebase.json` for better security:

```json
{
  "hosting": {
    "headers": [
      {
        "source": "**",
        "headers": [
          {
            "key": "X-Content-Type-Options",
            "value": "nosniff"
          },
          {
            "key": "X-Frame-Options",
            "value": "DENY"
          },
          {
            "key": "X-XSS-Protection",
            "value": "1; mode=block"
          },
          {
            "key": "Referrer-Policy",
            "value": "strict-origin-when-cross-origin"
          }
        ]
      },
      {
        "source": "**/*.@(jpg|jpeg|gif|png|svg|webp)",
        "headers": [
          {
            "key": "Cache-Control",
            "value": "public, max-age=31536000, immutable"
          }
        ]
      },
      {
        "source": "**/*.@(js|css)",
        "headers": [
          {
            "key": "Cache-Control",
            "value": "public, max-age=31536000, immutable"
          }
        ]
      },
      {
        "source": "index.html",
        "headers": [
          {
            "key": "Cache-Control",
            "value": "no-cache, no-store, must-revalidate"
          }
        ]
      }
    ]
  }
}
```

---

## Web Server Deployment

### Nginx Configuration

```nginx
server {
    listen 80;
    server_name verumglobal.foundation www.verumglobal.foundation;
    
    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name verumglobal.foundation www.verumglobal.foundation;
    
    # SSL Configuration
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # Root directory
    root /var/www/verum-omnis;
    index index.html;
    
    # Security headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    
    # Cache static assets
    location ~* \.(jpg|jpeg|png|gif|ico|svg|webp)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    location ~* \.(js|css)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # Don't cache HTML
    location ~* \.html$ {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }
    
    # Main location
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### Apache Configuration

```apache
<VirtualHost *:80>
    ServerName verumglobal.foundation
    ServerAlias www.verumglobal.foundation
    
    # Redirect HTTP to HTTPS
    Redirect permanent / https://verumglobal.foundation/
</VirtualHost>

<VirtualHost *:443>
    ServerName verumglobal.foundation
    ServerAlias www.verumglobal.foundation
    
    DocumentRoot /var/www/verum-omnis
    
    # SSL Configuration
    SSLEngine on
    SSLCertificateFile /path/to/cert.pem
    SSLCertificateKeyFile /path/to/key.pem
    
    # Security Headers
    Header always set X-Frame-Options "DENY"
    Header always set X-Content-Type-Options "nosniff"
    Header always set X-XSS-Protection "1; mode=block"
    Header always set Referrer-Policy "strict-origin-when-cross-origin"
    
    # Cache static assets
    <FilesMatch "\.(jpg|jpeg|png|gif|ico|svg|webp)$">
        Header set Cache-Control "public, max-age=31536000, immutable"
    </FilesMatch>
    
    <FilesMatch "\.(js|css)$">
        Header set Cache-Control "public, max-age=31536000, immutable"
    </FilesMatch>
    
    # Don't cache HTML
    <FilesMatch "\.(html)$">
        Header set Cache-Control "no-cache, no-store, must-revalidate"
    </FilesMatch>
    
    <Directory /var/www/verum-omnis>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>
</VirtualHost>
```

---

## Android APK

See [README-ANDROID.md](README-ANDROID.md) for detailed Android build and deployment instructions.

### Quick Build

```bash
# Local build
./build-android.sh

# Or using npm
npm run android:build
```

### GitHub Actions Build

The Android APK is automatically built via GitHub Actions on:
- Push to `main` branch
- Pull requests to `main`
- Manual workflow dispatch

Download the APK from the Actions artifacts.

---

## Environment Configuration

### Environment Variables

Create a `.env` file for environment-specific configuration:

```bash
# Firebase Configuration (if needed for functions)
FIREBASE_API_KEY=your_api_key
FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
FIREBASE_PROJECT_ID=your_project_id

# API Endpoints
VITE_API_URL=https://api.verumglobal.foundation

# Feature Flags
VITE_ENABLE_ANALYTICS=true
VITE_ENABLE_ERROR_TRACKING=true

# Environment
NODE_ENV=production
```

### Environment Files

- `.env` - Default environment variables (not committed)
- `.env.local` - Local overrides (not committed)
- `.env.production` - Production environment (not committed)
- `.env.staging` - Staging environment (not committed)

**IMPORTANT:** Never commit `.env` files containing sensitive data!

### Loading Environment Variables

If using Vite:

```javascript
const apiUrl = import.meta.env.VITE_API_URL;
```

If using Create React App:

```javascript
const apiUrl = process.env.REACT_APP_API_URL;
```

---

## Pre-Deployment Checklist

### Security

- [ ] SSL/TLS certificate configured
- [ ] Security headers configured
- [ ] Content Security Policy (CSP) implemented
- [ ] Environment variables not hardcoded
- [ ] Sensitive data not in repository
- [ ] CORS properly configured
- [ ] Rate limiting implemented (if applicable)

### Performance

- [ ] Assets minified and compressed
- [ ] Images optimized
- [ ] Caching strategy configured
- [ ] CDN configured (if applicable)
- [ ] Gzip/Brotli compression enabled
- [ ] Load testing completed

### Monitoring

- [ ] Error tracking configured (Sentry, etc.)
- [ ] Performance monitoring configured
- [ ] Uptime monitoring configured
- [ ] Logging infrastructure set up
- [ ] Alerts configured

### Testing

- [ ] All tests passing
- [ ] E2E tests completed
- [ ] Cross-browser testing done
- [ ] Mobile responsiveness verified
- [ ] Accessibility audit completed

### Documentation

- [ ] Privacy policy added
- [ ] Terms of service added
- [ ] README updated
- [ ] API documentation complete
- [ ] Deployment procedures documented

### Legal & Compliance

- [ ] GDPR compliance verified
- [ ] Cookie consent implemented (if needed)
- [ ] Privacy policy accessible
- [ ] Terms of service accessible
- [ ] Data retention policy documented

### Backup & Recovery

- [ ] Backup strategy implemented
- [ ] Disaster recovery plan documented
- [ ] Rollback procedures tested
- [ ] Database backups configured (if applicable)

---

## Deployment Commands Quick Reference

```bash
# Firebase
firebase deploy --only hosting                    # Deploy to production
firebase hosting:channel:deploy preview           # Deploy to preview

# Build Android APK
./build-android.sh                                # Local build
npm run android:build                             # Via npm

# Install dependencies
npm ci                                            # Clean install (CI)
npm install                                       # Regular install

# Check for security issues
npm audit                                         # Security audit
npm audit fix                                     # Auto-fix vulnerabilities
```

---

## Troubleshooting

### Firebase Deployment Issues

**Problem:** Permission denied
```bash
firebase login --reauth
```

**Problem:** Wrong project selected
```bash
firebase use <project-id>
```

### SSL/TLS Issues

**Problem:** Certificate not trusted
- Ensure certificate chain is complete
- Check certificate expiration date
- Verify domain matches certificate

### Performance Issues

**Problem:** Slow page load
- Check asset sizes
- Verify caching headers
- Enable compression
- Use CDN for static assets

---

## Support

For deployment issues, contact:
- Email: liam@verumglobal.foundation
- Check logs in Firebase Console
- Review GitHub Actions logs

---

**Last Updated:** 2025-11-26
