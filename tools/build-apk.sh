#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "/home/vince/.local/share/mise/installs/java/17.0.2/bin" ]; then
    export PATH="/home/vince/.local/share/mise/installs/java/17.0.2/bin:$PATH"
fi
PROJECT_ROOT="$(cd "$DIR/.." && pwd)"
PARENT_ROOT="$(cd "$PROJECT_ROOT/.." && pwd)"

SDK_ROOT="$PARENT_ROOT/focus-watch/tools/android-sdk"
BUILD_TOOLS="$SDK_ROOT/build-tools/34.0.0"
PLATFORM="$SDK_ROOT/platforms/android-34/android.jar"
SRC="$PROJECT_ROOT/android/app/src/main"
BUILD_DIR="$PROJECT_ROOT/android/build"
DIST_DIR="$PROJECT_ROOT/dist"
OUT_APK="$DIST_DIR/stralingstijd-wear.apk"
KEYSTORE="$PARENT_ROOT/focus-watch/tools/debug.keystore"
WEAR_TILES_LIBS="$PARENT_ROOT/focus-watch/tools/wear-tiles-libs"

echo "=== Building Stralingstijd Wear OS APK for Google Pixel Watch 3 ==="

# Clean build directory
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"/{compiled_res,gen,classes,dex}
mkdir -p "$DIST_DIR"

# 1. Compile Android Resources
echo "[1/6] Compiling resources with aapt2..."
"$BUILD_TOOLS/aapt2" compile --dir "$SRC/res" -o "$BUILD_DIR/compiled_res.zip"

# 2. Link Resources & Assets
echo "[2/6] Linking package with aapt2..."
"$BUILD_TOOLS/aapt2" link \
    -o "$BUILD_DIR/res.apk" \
    -I "$PLATFORM" \
    --manifest "$SRC/AndroidManifest.xml" \
    -A "$SRC/assets" \
    --java "$BUILD_DIR/gen" \
    --auto-add-overlay \
    "$BUILD_DIR/compiled_res.zip"

# 3. Compile Java Source Code
echo "[3/6] Compiling Java code..."
javac -source 8 -target 8 \
    -bootclasspath "$PLATFORM" \
    -cp "$PLATFORM:$BUILD_DIR/gen:$WEAR_TILES_LIBS/*" \
    -d "$BUILD_DIR/classes" \
    $(find "$BUILD_DIR/gen" "$SRC/java" -name "*.java")

# 4. Convert bytecode to Dalvik Executable (classes.dex)
echo "[4/6] Converting bytecode to DEX with d8..."
CLASS_FILES=$(find "$BUILD_DIR/classes" -name "*.class")
"$BUILD_TOOLS/d8" --lib "$PLATFORM" --output "$BUILD_DIR/dex" $CLASS_FILES "$WEAR_TILES_LIBS"/*.jar

# 5. Package classes.dex into APK
echo "[5/6] Packaging APK..."
cp "$BUILD_DIR/res.apk" "$BUILD_DIR/unaligned.apk"
cd "$BUILD_DIR/dex"
zip -u "$BUILD_DIR/unaligned.apk" classes.dex
cd "$PROJECT_ROOT"

# 6. Zipalign & Sign
echo "[6/6] Aligning & signing APK..."
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/unaligned.apk" "$BUILD_DIR/aligned.apk"

"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --ks-key-alias androiddebugkey \
    --key-pass pass:android \
    --out "$OUT_APK" \
    "$BUILD_DIR/aligned.apk"

# Verify signature
"$BUILD_TOOLS/apksigner" verify "$OUT_APK"

echo "=========================================================="
echo " SUCCESS: Stralingstijd APK built successfully!"
echo " Output: $OUT_APK"
echo " Size:   $(du -h "$OUT_APK" | cut -f1)"
echo "=========================================================="
