@echo off
REM Dynamic local.properties generator for Windows
REM This script generates local.properties based on environment variables and system detection

echo Generating dynamic local.properties...

REM Detect Windows and set default SDK path
if defined LOCALAPPDATA (
    set DEFAULT_SDK_PATH=%LOCALAPPDATA%\Android\Sdk
) else (
    set DEFAULT_SDK_PATH=%USERPROFILE%\AppData\Local\Android\Sdk
)

REM Use environment variable if set, otherwise use detected path
if defined ANDROID_SDK_ROOT (
    set SDK_DIR=%ANDROID_SDK_ROOT%
) else (
    set SDK_DIR=%DEFAULT_SDK_PATH%
)

REM Check if SDK path exists
if not exist "%SDK_DIR%" (
    echo Warning: Android SDK not found at %SDK_DIR%
    echo Please set ANDROID_SDK_ROOT environment variable or install Android SDK
    set SDK_DIR=%DEFAULT_SDK_PATH%
)

REM Generate local.properties content
(
echo ## This file must *NOT* be checked into Version Control Systems,
echo # as it contains information specific to your local configuration.
echo #
echo # Location of the SDK. This is only used by Gradle.
echo # For customization when using a Version Control System, please read the
echo # header note.
echo # Generated on: %date% %time%
echo.
echo # SDK Directory - Auto-detected or from ANDROID_SDK_ROOT
echo sdk.dir=%SDK_DIR:\=\\%
echo.
echo # Keystore Configuration - Set via environment variables for production
echo # ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
if defined ANDROID_KEYSTORE_PATH (
    echo release.storeFile=%ANDROID_KEYSTORE_PATH%
) else (
    echo release.storeFile=DEFAULT_PATH_TO_YOUR_KEYSTORE
)
if defined ANDROID_KEYSTORE_PASSWORD (
    echo release.storePassword=%ANDROID_KEYSTORE_PASSWORD%
) else (
    echo release.storePassword=DEFAULT_STORE_PASSWORD
)
if defined ANDROID_KEY_ALIAS (
    echo release.keyAlias=%ANDROID_KEY_ALIAS%
) else (
    echo release.keyAlias=DEFAULT_KEY_ALIAS
)
if defined ANDROID_KEY_PASSWORD (
    echo release.keyPassword=%ANDROID_KEY_PASSWORD%
) else (
    echo release.keyPassword=DEFAULT_KEY_PASSWORD
)
echo.
echo # Optional: API Keys (uncomment and set if needed)
if defined GEMINI_API_KEY (
    echo # gemini.api.key=%GEMINI_API_KEY%
) else (
    echo # gemini.api.key=your_api_key_here
)
echo.
echo # Build configurations (uncomment and set if needed)
if defined BUILD_DEBUG (
    echo # build.debug=%BUILD_DEBUG%
) else (
    echo # build.debug=true
)
if defined BUILD_RELEASE (
    echo # build.release=%BUILD_RELEASE%
) else (
    echo # build.release=false
)
) > local.properties

echo local.properties generated successfully!
echo SDK Directory: %SDK_DIR%
echo.
echo To set up production keystore, set these environment variables:
echo set ANDROID_KEYSTORE_PATH=C:\path\to\your\keystore.jks
echo set ANDROID_KEYSTORE_PASSWORD=your_store_password
echo set ANDROID_KEY_ALIAS=your_key_alias
echo set ANDROID_KEY_PASSWORD=your_key_password
echo.
echo Then run this script again to update local.properties
pause 