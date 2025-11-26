# Android APK Build Setup - Summary

## Overview
The Verum Omnis web application has been successfully configured to build as an Android APK using Capacitor. This document summarizes what has been set up and how to use it.

## What Was Done

### 1. Capacitor Integration
- ✅ Installed Capacitor (@capacitor/core, @capacitor/cli, @capacitor/android v7.4.4)
- ✅ Initialized Capacitor configuration with:
  - **App ID**: com.verumglobal.foundation
  - **App Name**: Verum Omnis
  - **Web Directory**: www/
- ✅ Created Android project structure in `android/` directory
- ✅ Synced web assets to Android project

### 2. Build Infrastructure
- ✅ Created `build-android.sh` - automated build script with:
  - Prerequisite checking
  - Dependency installation
  - Web asset preparation
  - Capacitor syncing
  - Gradle APK building
  
- ✅ Updated `package.json` with Android build scripts:
  ```json
  {
    "android:prepare": "mkdir -p www && cp index.html www/ && cp -r assets www/ && cp favicon.png www/",
    "android:add": "npm run android:prepare && npx cap add android",
    "android:sync": "npm run android:prepare && npx cap sync android",
    "android:build": "npm run android:sync && cd android && ./gradlew assembleDebug",
    "android:build:release": "npm run android:sync && cd android && ./gradlew assembleRelease",
    "android:open": "npx cap open android",
    "android:run": "npx cap run android"
  }
  ```

### 3. CI/CD Workflow
- ✅ Created `.github/workflows/build-android.yml` - GitHub Actions workflow that:
  - Sets up Node.js, Java 17, and Android SDK
  - Installs dependencies
  - Prepares web assets
  - Adds/syncs Android platform
  - Builds debug APK
  - Attempts release APK build
  - Uploads APKs as artifacts

### 4. Documentation
- ✅ Created `README-ANDROID.md` - comprehensive guide covering:
  - Prerequisites (Node.js, Java, Android SDK)
  - Quick start guide
  - Manual build steps
  - Release APK building
  - APK installation methods
  - Customization options (icon, name, package)
  - Troubleshooting guide
  - Advanced options (Android Studio, emulator, live reload)

### 5. Git Configuration
- ✅ Updated `.gitignore` to exclude:
  - `www/` (generated)
  - `android/` (generated)
  - `ios/` (if added later)
  - `*.apk` and `*.aab` (build outputs)
  - `apk-outputs/` (artifact directory)

## App Specifications

- **Package Name**: com.verumglobal.foundation
- **App Name**: Verum Omnis
- **Version**: 1.0 (versionCode: 1)
- **Min SDK**: 22 (Android 5.1 Lollipop)
- **Target SDK**: 34 (Android 14)
- **Build Tool**: Gradle 8.11.1
- **Android Gradle Plugin**: 8.7.2

## How to Build the APK

### Option 1: GitHub Actions (Recommended)
The GitHub Actions workflow will automatically build the APK when:
- Code is pushed to `main` or `copilot/build-android-project-apk` branches
- Pull requests are created targeting `main`
- Manually triggered via "Run workflow" in GitHub Actions

**To download the APK:**
1. Go to Actions tab in GitHub
2. Click on the latest "Build Android APK" workflow run
3. Download "verum-omnis-debug-apk" artifact
4. Extract the ZIP file to get the APK

### Option 2: Local Build Script
If you have all prerequisites installed:
```bash
./build-android.sh
```

This will build the APK at:
- `android/app/build/outputs/apk/debug/app-debug.apk`
- `verum-omnis-debug.apk` (copied to root)

### Option 3: npm Scripts
```bash
npm run android:build        # Build debug APK
npm run android:build:release  # Build release APK (requires signing config)
```

### Option 4: Manual Build
```bash
# 1. Install dependencies
npm install

# 2. Prepare web assets
npm run android:prepare

# 3. Sync Capacitor
npx cap sync android

# 4. Build APK
cd android
./gradlew assembleDebug
```

## Current Status

### ✅ Completed
- Capacitor configuration
- Android project structure
- Build scripts and automation
- CI/CD pipeline
- Comprehensive documentation

### ⚠️ Pending
- GitHub Actions workflow approval (requires repository permissions)
- Local build may require network access to download Gradle dependencies

### 🔒 Network Limitation
The local build environment has restricted access to `dl.google.com`, which is needed to download Gradle and Android build dependencies. This is why the GitHub Actions workflow is the recommended approach - it has full network access.

## Next Steps

1. **Approve GitHub Actions Workflow** (if needed)
   - The workflow is set up and ready to run
   - May require approval for first-time workflow runs

2. **Build the APK**
   - Trigger the GitHub Actions workflow manually or by pushing code
   - Or build locally if you have network access to Android repositories

3. **Test the APK**
   - Install on a physical Android device or emulator
   - Verify the web app loads correctly
   - Test all functionality

4. **Optional: Set up Release Signing**
   - Generate a signing keystore
   - Configure signing in `android/app/build.gradle`
   - Build signed release APK for production

5. **Optional: Customize**
   - Replace app icon
   - Update splash screen
   - Add native plugins if needed

## Files Added/Modified

### New Files
- `capacitor.config.json` - Capacitor configuration
- `package.json` - Node.js dependencies and scripts
- `package-lock.json` - Locked dependency versions
- `build-android.sh` - Automated build script
- `README-ANDROID.md` - Android build documentation
- `.github/workflows/build-android.yml` - CI/CD workflow
- `BUILD-SUMMARY.md` - This file

### Modified Files
- `.gitignore` - Excluded generated Android files and APKs

### Generated (Excluded from Git)
- `www/` - Web assets for Capacitor
- `android/` - Android project
- `node_modules/` - npm dependencies

## Support

For questions or issues:
- Review `README-ANDROID.md` for detailed instructions
- Check GitHub Actions logs for build errors
- Email: liam@verumglobal.foundation

## Technical Notes

- This is a **hybrid app** - it wraps the web application in a native Android container
- The app uses a WebView to display the web content
- All existing web functionality should work as-is
- Native features can be added via Capacitor plugins if needed
- The build process is reproducible and automated via CI/CD

---

**Last Updated**: 2025-11-26
**Verum Omnis Version**: 1.0.0
**Capacitor Version**: 7.4.4
