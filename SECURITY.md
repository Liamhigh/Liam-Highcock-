# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in Verum Omnis, please report it responsibly:

### How to Report

**Email:** liam@verumglobal.foundation

Please include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

### Response Timeline

- **Initial Response:** Within 48 hours
- **Status Update:** Within 7 days
- **Fix Timeline:** Based on severity (critical issues prioritized)

### Security Best Practices

When deploying Verum Omnis in production:

1. **HTTPS/TLS:** Always use HTTPS for all deployments
2. **Environment Variables:** Never commit sensitive data (API keys, secrets) to the repository
3. **Dependencies:** Regularly update dependencies and monitor for vulnerabilities
4. **Access Control:** Implement proper authentication and authorization
5. **Data Privacy:** Follow GDPR and relevant data protection regulations
6. **Content Security Policy:** Configure CSP headers appropriately

### Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

### Security Updates

Security updates are released as needed. Subscribe to repository notifications for updates.

## Disclosure Policy

- We ask that you give us reasonable time to fix issues before public disclosure
- We will acknowledge your contribution in release notes (unless you prefer to remain anonymous)
- We do not currently offer a bug bounty program
