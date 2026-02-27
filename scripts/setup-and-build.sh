#!/bin/bash
# Peak AI — Mac build script
# Installs Android SDK if needed, then builds debug APK
# Run from the project root: ./scripts/setup-and-build.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SDK_DIR="$HOME/Library/Android/sdk"
CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"

echo "═══════════════════════════════════════"
echo "  Peak AI — Android Build Setup"
echo "═══════════════════════════════════════"

# ── 1. Java check ──────────────────────────────────────────────────────────
echo ""
echo "▶ Checking Java..."
if ! java -version 2>&1 | grep -q "17\|18\|19\|20\|21"; then
    echo "  Java 17+ required. Install with: brew install openjdk@17"
    echo "  Then: sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk"
    # Try to find a usable Java anyway
    if command -v /usr/libexec/java_home &>/dev/null; then
        JAVA17=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
        if [ -n "$JAVA17" ]; then
            export JAVA_HOME="$JAVA17"
            echo "  Found Java 17 at: $JAVA17"
        fi
    fi
else
    echo "  ✅ Java OK: $(java -version 2>&1 | head -1)"
fi

# ── 2. Android SDK ─────────────────────────────────────────────────────────
echo ""
echo "▶ Checking Android SDK..."
if [ ! -d "$SDK_DIR/platform-tools" ]; then
    echo "  Android SDK not found. Installing command line tools..."
    mkdir -p "$SDK_DIR/cmdline-tools"
    TMP_ZIP="/tmp/cmdtools.zip"
    curl -L "$CMDTOOLS_URL" -o "$TMP_ZIP"
    unzip -q "$TMP_ZIP" -d "$SDK_DIR/cmdline-tools"
    mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest" 2>/dev/null || true
    rm "$TMP_ZIP"
    
    SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
    echo "  Installing SDK components (this takes a few minutes)..."
    yes | "$SDKMANAGER" --licenses > /dev/null 2>&1
    "$SDKMANAGER" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
    
    # Create local.properties
    echo "sdk.dir=$SDK_DIR" > "$PROJECT_DIR/local.properties"
    echo "  ✅ Android SDK installed"
else
    echo "  ✅ Android SDK found at $SDK_DIR"
    echo "sdk.dir=$SDK_DIR" > "$PROJECT_DIR/local.properties"
fi

# ── 3. Build ───────────────────────────────────────────────────────────────
echo ""
echo "▶ Building Peak AI debug APK..."
cd "$PROJECT_DIR"
./gradlew assembleDebug --no-daemon 2>&1

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "═══════════════════════════════════════"
    echo "  ✅ BUILD SUCCESSFUL"
    echo "  APK: $APK_PATH"
    echo "  Size: $(du -h "$APK_PATH" | cut -f1)"
    echo ""
    echo "  Install via ADB:"
    echo "  adb install $APK_PATH"
    echo ""
    echo "  Or transfer APK to phone and sideload."
    echo "═══════════════════════════════════════"
else
    echo "  ❌ Build failed — APK not found"
    exit 1
fi
