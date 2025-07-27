#!/bin/bash

# Dynamic local.properties generator
# This script generates local.properties based on environment variables and system detection

echo "Generating dynamic local.properties..."

# Detect OS and set default SDK path
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    DEFAULT_SDK_PATH="$HOME/Library/Android/sdk"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    DEFAULT_SDK_PATH="$HOME/Android/Sdk"
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    # Windows (Git Bash, Cygwin)
    DEFAULT_SDK_PATH="$LOCALAPPDATA/Android/Sdk"
else
    # Fallback
    DEFAULT_SDK_PATH="$HOME/Android/Sdk"
fi

# Use environment variable if set, otherwise use detected path
SDK_DIR="${ANDROID_SDK_ROOT:-$DEFAULT_SDK_PATH}"

# Check if SDK path exists
if [ ! -d "$SDK_DIR" ]; then
    echo "Warning: Android SDK not found at $SDK_DIR"
    echo "Please set ANDROID_SDK_ROOT environment variable or install Android SDK"
    SDK_DIR="$DEFAULT_SDK_PATH"
fi

# Convert Windows paths to proper format with double backslashes
if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    # Windows (Git Bash, Cygwin) - convert to double backslash format
    SDK_DIR_FORMATTED=$(echo "$SDK_DIR" | sed 's/\\/\\\\/g')
else
    SDK_DIR_FORMATTED="$SDK_DIR"
fi

# Generate local.properties content
cat > local.properties << EOF
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
# For customization when using a Version Control System, please read the
# header note.
# Generated on: $(date)

# SDK Directory - Auto-detected or from ANDROID_SDK_ROOT
sdk.dir=$SDK_DIR_FORMATTED

# Keystore Configuration - Set via environment variables for production
# ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
release.storeFile=${ANDROID_KEYSTORE_PATH:-DEFAULT_PATH_TO_YOUR_KEYSTORE}
release.storePassword=${ANDROID_KEYSTORE_PASSWORD:-DEFAULT_STORE_PASSWORD}
release.keyAlias=${ANDROID_KEY_ALIAS:-DEFAULT_KEY_ALIAS}
release.keyPassword=${ANDROID_KEY_PASSWORD:-DEFAULT_KEY_PASSWORD}

# Optional: API Keys (uncomment and set if needed)
# gemini.api.key=${GEMINI_API_KEY:-your_api_key_here}

# Build configurations (uncomment and set if needed)
# build.debug=${BUILD_DEBUG:-true}
# build.release=${BUILD_RELEASE:-false}
EOF

echo "local.properties generated successfully!"
echo "SDK Directory: $SDK_DIR"
echo ""
echo "To set up production keystore, set these environment variables:"
echo "export ANDROID_KEYSTORE_PATH=/path/to/your/keystore.jks"
echo "export ANDROID_KEYSTORE_PASSWORD=your_store_password"
echo "export ANDROID_KEY_ALIAS=your_key_alias"
echo "export ANDROID_KEY_PASSWORD=your_key_password"
echo ""
echo "Then run this script again to update local.properties" 