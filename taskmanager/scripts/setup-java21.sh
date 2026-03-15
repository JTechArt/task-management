#!/usr/bin/env bash
# Setup script to ensure Java 21 is being used for TaskManager

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESOLVER="${PROJECT_DIR}/scripts/resolve-java21.sh"

echo "🔍 Checking Java installation..."

if [ ! -x "$RESOLVER" ]; then
    echo "❌ Missing Java resolver script at $RESOLVER"
    exit 1
fi

CURRENT_JAVA=$(java -version 2>&1 | head -n 1)
echo "📌 Current Java: $CURRENT_JAVA"

if ! JAVA_21_HOME="$("$RESOLVER")"; then
    echo "❌ Java 21 not found."
    echo "Install a JDK 21 and re-run this script, or set TASKMANAGER_JAVA_HOME."
    exit 1
fi

echo "✅ Java 21 found at: $JAVA_21_HOME"
echo "🔧 Export these in your shell before using Cursor or Gradle:"
echo "   export JAVA_HOME=\"$JAVA_21_HOME\""
echo "   export PATH=\"\$JAVA_HOME/bin:\$PATH\""

echo ""
echo "✅ Java version after setup:"
"$JAVA_21_HOME/bin/java" -version

echo ""
echo "🎯 Optional:"
echo "   export TASKMANAGER_JAVA_HOME=\"$JAVA_21_HOME\""
echo ""
echo "💡 Cursor tasks use taskmanager/scripts/resolve-java21.sh, so hardcoded IDE paths are no longer required."
echo ""
echo "🔨 Cleaning and rebuilding the project with Java 21..."
cd "$PROJECT_DIR"
JAVA_HOME="$JAVA_21_HOME" PATH="$JAVA_21_HOME/bin:$PATH" ./gradlew clean :desktop-app:build -x test --no-daemon
BUILD_STATUS=$?

if [ $BUILD_STATUS -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "🚀 Next steps:"
    echo "   1. Copy taskmanager/.env.example to taskmanager/.env.local if you need custom config"
    echo "   2. In Cursor, use 'TaskManager: Run Local App' or 'TaskManager: Debug Local App'"
else
    echo ""
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi
