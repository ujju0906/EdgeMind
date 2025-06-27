#!/bin/bash

# Script to manually download the Qwen2.5-1.5B-Instruct model for Android Document QA
# This script downloads the model and pushes it to the device using adb

MODEL_URL="https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task?download=true"
MODEL_FILENAME="Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task"
DEVICE_PATH="/data/local/tmp/llm/"

echo "🚀 Android Document QA - Model Download Script"
echo "=============================================="
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ Error: adb is not installed or not in PATH"
    echo "Please install Android SDK Platform Tools and add adb to your PATH"
    exit 1
fi

# Check if device is connected
echo "📱 Checking for connected devices..."
adb devices | grep -q "device$"
if [ $? -ne 0 ]; then
    echo "❌ Error: No Android device connected"
    echo "Please connect your device via USB and enable USB debugging"
    exit 1
fi

echo "✅ Device connected successfully"
echo ""

# Create directory on device
echo "📁 Creating directory on device..."
adb shell mkdir -p $DEVICE_PATH
if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to create directory on device"
    exit 1
fi

# Check if model already exists
echo "🔍 Checking if model already exists..."
adb shell test -f "$DEVICE_PATH$MODEL_FILENAME"
if [ $? -eq 0 ]; then
    echo "✅ Model already exists on device"
    echo "Model path: $DEVICE_PATH$MODEL_FILENAME"
    exit 0
fi

# Download model
echo "⬇️  Downloading model from Hugging Face..."
echo "URL: $MODEL_URL"
echo "This may take several minutes depending on your internet connection..."
echo ""

# Download with progress
if command -v wget &> /dev/null; then
    wget --progress=bar:force:noscroll -O "$MODEL_FILENAME" "$MODEL_URL"
elif command -v curl &> /dev/null; then
    curl -L -o "$MODEL_FILENAME" "$MODEL_URL"
else
    echo "❌ Error: Neither wget nor curl is available"
    echo "Please install wget or curl to download the model"
    exit 1
fi

if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to download model"
    exit 1
fi

# Check file size
FILE_SIZE=$(stat -f%z "$MODEL_FILENAME" 2>/dev/null || stat -c%s "$MODEL_FILENAME" 2>/dev/null || echo "unknown")
echo "✅ Model downloaded successfully"
echo "File size: $FILE_SIZE bytes"

# Push to device
echo "📤 Pushing model to device..."
adb push "$MODEL_FILENAME" "$DEVICE_PATH$MODEL_FILENAME"
if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to push model to device"
    exit 1
fi

# Verify file on device
echo "🔍 Verifying model on device..."
adb shell test -f "$DEVICE_PATH$MODEL_FILENAME"
if [ $? -eq 0 ]; then
    echo "✅ Model successfully pushed to device"
    echo "Model path: $DEVICE_PATH$MODEL_FILENAME"
else
    echo "❌ Error: Model verification failed"
    exit 1
fi

# Clean up local file
echo "🧹 Cleaning up local file..."
rm "$MODEL_FILENAME"

echo ""
echo "🎉 Model setup completed successfully!"
echo "You can now run the Android Document QA app with local LLM support."
echo ""
echo "Note: The app will automatically detect and use the local model."
echo "Look for the green computer icon in the app's top bar to confirm local LLM is active." 