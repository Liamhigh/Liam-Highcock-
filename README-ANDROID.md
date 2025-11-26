# Verum Omnis Android App Build Guide

This guide explains how to build the Verum Omnis Android APK from the web application.

## Overview

The Verum Omnis web application has been wrapped as a native Android application using [Capacitor](https://capacitorjs.com/), which allows the web app to run as a native Android app with full access to device features.

## Prerequisites

Before building the Android APK, ensure you have the following installed:

1. **Node.js** (v14 or later)
   - Download from: https://nodejs.org/
   - Verify: `node --version`

2. **Java Development Kit (JDK)** (v11 or later)
   - Download from: https://adoptium.net/
   - Verify: `java -version`

3. **Android Studio** or **Android SDK Command-line Tools**
   - Download from: https://developer.android.com/studio
   - Required SDK components:
     - Android SDK Platform 34 or higher
     - Android SDK Build-Tools 34.0.0 or higher
     - Android SDK Platform-Tools

4. **Environment Variables**
   Set the following environment variables:
   ```bash
   export ANDROID_SDK_ROOT=/path/to/android/sdk
   export ANDROID_HOME=/path/to/android/sdk
   ```

## Project Structure

```
.
├── index.html              # Main HTML file
├── assets/                 # Web assets (JS, CSS, images, videos)
├── www/                    # Capacitor web directory (generated)
├── android/                # Android native project (generated)
├── capacitor.config.json   # Capacitor configuration
├── package.json            # Node.js dependencies
├── build-android.sh        # Automated build script
└── README-ANDROID.md       # This file
```

## Quick Start

### Option 1: Automated Build (Recommended)

Use the provided build script:

```bash
./build-android.sh
```

This script will:
1. Check all prerequisites
2. Install dependencies
3. Prepare web assets
4. Sync Capacitor
5. Build the Android APK

The APK will be created at:
- `android/app/build/outputs/apk/debug/app-debug.apk`
- `verum-omnis-debug.apk` (copy in root directory)

### Option 2: Manual Build

Follow these steps for manual building:

#### Step 1: Install Dependencies

```bash
npm install
```

#### Step 2: Prepare Web Assets

```bash
# Create www directory if it doesn't exist
mkdir -p www

# Copy web files
cp index.html www/
cp -r assets www/
cp favicon.png www/
```

#### Step 3: Add/Sync Android Platform

First time setup:
```bash
npx cap add android
```

Or sync existing platform:
```bash
npx cap sync android
```

#### Step 4: Build the APK

```bash
cd android
./gradlew assembleDebug
```

The APK will be at: `android/app/build/outputs/apk/debug/app-debug.apk`

## Building Release APK

For a release APK (signed and optimized):

### Step 1: Generate Keystore

```bash
keytool -genkey -v -keystore verum-omnis.keystore -alias verum-omnis -keyalg RSA -keysize 2048 -validity 10000
```

### Step 2: Configure Signing

Create `android/app/keystore.properties`:

```properties
storeFile=/path/to/verum-omnis.keystore
storePassword=your-keystore-password
keyAlias=verum-omnis
keyPassword=your-key-password
```

### Step 3: Update build.gradle

Add to `android/app/build.gradle`:

```gradle
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    ...
    signingConfigs {
        release {
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}
```

### Step 4: Build Release APK

```bash
cd android
./gradlew assembleRelease
```

The release APK will be at: `android/app/build/outputs/apk/release/app-release.apk`

## Installing the APK

### Option 1: Using ADB

```bash
adb install verum-omnis-debug.apk
```

### Option 2: Manual Installation

1. Transfer the APK to your Android device
2. Enable "Install from Unknown Sources" in device settings
3. Open the APK file on your device
4. Follow the installation prompts

## Capacitor Configuration

The Capacitor configuration is in `capacitor.config.json`:

```json
{
  "appId": "com.verumglobal.foundation",
  "appName": "Verum Omnis",
  "webDir": "www"
}
```

### App Details

- **Package Name**: com.verumglobal.foundation
- **App Name**: Verum Omnis
- **Version**: 1.0 (versionCode: 1)
- **Min SDK**: 22 (Android 5.1)
- **Target SDK**: 34 (Android 14)

## Customization

### Changing App Icon

Replace icon files in:
```
android/app/src/main/res/mipmap-*/ic_launcher.png
android/app/src/main/res/mipmap-*/ic_launcher_round.png
```

Or use Android Asset Studio: https://romannurik.github.io/AndroidAssetStudio/

### Changing App Name

Edit `android/app/src/main/res/values/strings.xml`:

```xml
<string name="app_name">Verum Omnis</string>
```

### Changing Package Name

1. Update `capacitor.config.json`:
   ```json
   "appId": "your.new.package.name"
   ```

2. Rebuild the Android platform:
   ```bash
   rm -rf android
   npx cap add android
   ```

## Troubleshooting

### Build Failures

If the build fails:

1. **Clean the build**:
   ```bash
   cd android
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. **Check SDK installation**:
   ```bash
   $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --list
   ```

3. **Update dependencies**:
   ```bash
   cd android
   ./gradlew --refresh-dependencies
   ```

### Network Issues

If you encounter network errors downloading Gradle dependencies:

- Ensure you have internet connectivity
- Check firewall/proxy settings
- Try using a VPN if certain repositories are blocked

### Sync Issues

If Capacitor sync fails:

```bash
rm -rf android
npx cap add android
npx cap sync android
```

## Advanced Options

### Building from Android Studio

1. Open Android Studio
2. Open the `android` folder as a project
3. Wait for Gradle sync to complete
4. Build > Build Bundle(s) / APK(s) > Build APK(s)

### Running on Emulator

```bash
npx cap run android
```

### Live Reload During Development

```bash
npx cap run android -l --external
```

## Resources

- [Capacitor Documentation](https://capacitorjs.com/docs)
- [Android Developer Guide](https://developer.android.com/guide)
- [Gradle Build Guide](https://developer.android.com/studio/build)

## Support

For issues or questions:
- Email: liam@verumglobal.foundation
- GitHub Issues: https://github.com/Liamhigh/Liam-Highcock-/issues

---

**Note**: This is a web-to-native wrapper application. The core application logic runs in a WebView using the existing web assets. Native plugins can be added via Capacitor if needed.
