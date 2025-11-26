#!/bin/bash

# Verum Omnis Android APK Build Script
# This script sets up and builds the Android APK from the web application

set -e  # Exit on error

echo "================================================"
echo "Verum Omnis Android APK Build Script"
echo "================================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
echo "Checking prerequisites..."

# Check Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}Error: Node.js is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Node.js found: $(node --version)${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java found: $(java -version 2>&1 | head -n 1)${NC}"

# Check Android SDK
if [ -z "$ANDROID_SDK_ROOT" ] && [ -z "$ANDROID_HOME" ]; then
    echo -e "${RED}Error: ANDROID_SDK_ROOT or ANDROID_HOME is not set${NC}"
    exit 1
fi
ANDROID_SDK="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
echo -e "${GREEN}✓ Android SDK found: $ANDROID_SDK${NC}"

echo ""
echo "Step 1: Installing dependencies..."
if [ ! -d "node_modules" ]; then
    npm install
else
    echo -e "${YELLOW}Dependencies already installed${NC}"
fi

echo ""
echo "Step 2: Preparing web assets..."
if [ ! -d "www" ]; then
    mkdir -p www
fi

# Copy web files to www directory
echo "Copying web assets..."
cp -f index.html www/
cp -rf assets www/
cp -f favicon.png www/
[ -f firebase.json ] && cp -f firebase.json www/ || true

echo -e "${GREEN}✓ Web assets prepared${NC}"

echo ""
echo "Step 3: Syncing Capacitor..."
if [ ! -d "android" ]; then
    echo "Adding Android platform..."
    npx cap add android
else
    echo "Syncing existing Android platform..."
    npx cap sync android
fi

echo -e "${GREEN}✓ Capacitor sync complete${NC}"

echo ""
echo "Step 4: Building Android APK..."
cd android

# Build the debug APK
echo "Building debug APK with Gradle..."
./gradlew assembleDebug --no-daemon --warning-mode all

echo ""
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo -e "${GREEN}================================================${NC}"
    echo -e "${GREEN}✓ APK built successfully!${NC}"
    echo -e "${GREEN}================================================${NC}"
    echo ""
    echo "APK Location:"
    echo "  $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    
    # Get APK info
    APK_SIZE=$(du -h "app/build/outputs/apk/debug/app-debug.apk" | cut -f1)
    echo "APK Size: $APK_SIZE"
    echo ""
    
    # Copy to root for easy access
    cp app/build/outputs/apk/debug/app-debug.apk ../verum-omnis-debug.apk
    echo "APK also copied to:"
    echo "  $(dirname $(pwd))/verum-omnis-debug.apk"
    echo ""
    
    echo "To install on a device:"
    echo "  adb install verum-omnis-debug.apk"
    echo ""
    echo "Or transfer the APK to your Android device and install manually."
else
    echo -e "${RED}Error: APK was not generated${NC}"
    exit 1
fi

cd ..
