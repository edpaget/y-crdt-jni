#!/bin/bash
# Build yffi (Y-CRDT C FFI) from the upstream y-crdt repository.
#
# This script clones the y-crdt repo (if not already present), builds the yffi
# library, and copies it to the build output directory.
#
# Usage: ./scripts/build-yffi.sh
#
# Output: build/native/libyrs.dylib (macOS) or libyrs.so (Linux) or yrs.dll (Windows)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_ROOT/build/native"
YFFI_REPO="https://github.com/y-crdt/y-crdt.git"
YFFI_VERSION="v0.25.0"
CLONE_DIR="$PROJECT_ROOT/build/y-crdt"

echo "Building yffi from upstream..."

# Create build directory
mkdir -p "$BUILD_DIR"

# Clone or update y-crdt repo
if [ -d "$CLONE_DIR" ]; then
    echo "Updating existing y-crdt clone..."
    cd "$CLONE_DIR"
    git fetch --tags
    git checkout "$YFFI_VERSION"
else
    echo "Cloning y-crdt repository..."
    git clone --depth 1 --branch "$YFFI_VERSION" "$YFFI_REPO" "$CLONE_DIR"
fi

# Build yffi
# Clear RUSTFLAGS to avoid treating warnings as errors in upstream code
echo "Building yffi library..."
cd "$CLONE_DIR/yffi"
RUSTFLAGS="" cargo build --release

# Determine library name based on OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    LIB_NAME="libyrs.dylib"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    LIB_NAME="libyrs.so"
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "win32" ]]; then
    LIB_NAME="yrs.dll"
else
    echo "Unknown OS: $OSTYPE"
    exit 1
fi

# Copy library to build directory (yffi is part of workspace, so target is at workspace root)
echo "Copying $LIB_NAME to $BUILD_DIR..."
cp "$CLONE_DIR/target/release/$LIB_NAME" "$BUILD_DIR/"

# Also copy the header file
echo "Copying libyrs.h header..."
mkdir -p "$BUILD_DIR/include"
cp "$CLONE_DIR/tests-ffi/include/libyrs.h" "$BUILD_DIR/include/"

echo ""
echo "Build complete!"
echo "  Library: $BUILD_DIR/$LIB_NAME"
echo "  Header:  $BUILD_DIR/include/libyrs.h"
