@echo off
setlocal enabledelayedexpansion

REM Script to manually download the Qwen2.5-1.5B-Instruct model for Android Document QA
REM This script downloads the model and pushes it to the device using adb

set MODEL_URL=https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task?download=true
set MODEL_FILENAME=Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task
set DEVICE_PATH=/data/local/tmp/llm/

echo 🚀 Android Document QA - Model Download Script
echo ==============================================
echo.

REM Check if adb is available
where adb >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: adb is not installed or not in PATH
    echo Please install Android SDK Platform Tools and add adb to your PATH
    pause
    exit /b 1
)

REM Check if device is connected
echo 📱 Checking for connected devices...
adb devices | findstr "device$" >nul
if %errorlevel% neq 0 (
    echo ❌ Error: No Android device connected
    echo Please connect your device via USB and enable USB debugging
    pause
    exit /b 1
)

echo ✅ Device connected successfully
echo.

REM Create directory on device
echo 📁 Creating directory on device...
adb shell mkdir -p %DEVICE_PATH%
if %errorlevel% neq 0 (
    echo ❌ Error: Failed to create directory on device
    pause
    exit /b 1
)

REM Check if model already exists
echo 🔍 Checking if model already exists...
adb shell test -f "%DEVICE_PATH%%MODEL_FILENAME%"
if %errorlevel% equ 0 (
    echo ✅ Model already exists on device
    echo Model path: %DEVICE_PATH%%MODEL_FILENAME%
    pause
    exit /b 0
)

REM Download model
echo ⬇️  Downloading model from Hugging Face...
echo URL: %MODEL_URL%
echo This may take several minutes depending on your internet connection...
echo.

REM Download with curl (Windows 10+ has curl built-in)
curl -L -o "%MODEL_FILENAME%" "%MODEL_URL%"
if %errorlevel% neq 0 (
    echo ❌ Error: Failed to download model
    echo Please check your internet connection and try again
    pause
    exit /b 1
)

REM Check file size
for %%A in ("%MODEL_FILENAME%") do set FILE_SIZE=%%~zA
echo ✅ Model downloaded successfully
echo File size: %FILE_SIZE% bytes

REM Push to device
echo 📤 Pushing model to device...
adb push "%MODEL_FILENAME%" "%DEVICE_PATH%%MODEL_FILENAME%"
if %errorlevel% neq 0 (
    echo ❌ Error: Failed to push model to device
    pause
    exit /b 1
)

REM Verify file on device
echo 🔍 Verifying model on device...
adb shell test -f "%DEVICE_PATH%%MODEL_FILENAME%"
if %errorlevel% equ 0 (
    echo ✅ Model successfully pushed to device
    echo Model path: %DEVICE_PATH%%MODEL_FILENAME%
) else (
    echo ❌ Error: Model verification failed
    pause
    exit /b 1
)

REM Clean up local file
echo 🧹 Cleaning up local file...
del "%MODEL_FILENAME%"

echo.
echo 🎉 Model setup completed successfully!
echo You can now run the Android Document QA app with local LLM support.
echo.
echo Note: The app will automatically detect and use the local model.
echo Look for the green computer icon in the app's top bar to confirm local LLM is active.
pause 