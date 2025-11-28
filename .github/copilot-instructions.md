✅ copilot-instructions.md (MASTER VERSION — DO NOT EDIT)

PROJECT ROOT RULES

The ONLY valid app in this repository is the native Android Kotlin Verum Omnis forensic engine.

Delete ALL web/Capacitor/React/Vite folders including:

public/, www/, dist/, vite.config.js, capacitor.config.*, index.html, App.js, node_modules/.


Never reintroduce Capacitor, Expo, React Native, Vite, WebViews, or web bundles.


ARCHITECTURE RULES

The app MUST run:

100% offline

stateless

with no backend

with all logic local

with support for:

voice, image, PDF verification

OCR

SHA-512 hashing

watermarking

QR code generation

9-brain analysis logic




UI RULES

Enforce Verum Omnis official dark theme.

Enforce the official Verum Omnis 3D logo (already provided in repo).

Fix layout so that:

the chat works

the upload portal works

video and image previews work

the forensic report viewer works



BUILD RULES

Ensure the root contains:

app/

gradle/

gradlew

settings.gradle

build.gradle

AndroidManifest.xml


Repair all Gradle paths.

Ensure app builds with no warnings.

Produce a working Debug APK and Release APK.


ACTION RULES
Whenever user instructs “Fix repo” or “Build APK”, Copilot must:

Clean unused folders

Move native app to root if needed

Patch Gradle

Patch Manifest

Add missing imports

Fix resource linker errors

Ensure videos load correctly

Ensure chat logic executes offline

Ensure QR + SHA-512 engine is wired

Generate build instructions

Update .gitignore safely


CONSTITUTIONAL RULES

The app MUST stay offline.

NEVER send raw data to external APIs.

NEVER add telemetry or analytics.

NEVER rely on a running website.

NEVER remove SHA-512 / watermark / QR from PDFs.

