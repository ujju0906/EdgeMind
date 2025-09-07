# Dynamic Configuration System

This project now supports dynamic configuration through environment variables and automated scripts, making it easier to manage different build environments and sensitive information.

## 🚀 Quick Start

### For macOS/Linux:
```bash
# Make the script executable
chmod +x generate-local-properties.sh

# Generate local.properties with auto-detected settings
./generate-local-properties.sh
```

### For Windows:
```batch
# Run the batch file
generate-local-properties.bat
```

## 📁 Files Overview

- `local.properties.template` - Template showing the structure
- `generate-local-properties.sh` - Unix/Linux/macOS script
- `generate-local-properties.bat` - Windows batch script
- `gradle.properties.template` - Gradle properties template
- `build-scripts/dynamic-config.gradle.kts` - Dynamic Gradle configuration

## 🔧 Environment Variables

### SDK Configuration
```bash
# Set Android SDK path (optional - auto-detected)
export ANDROID_SDK_ROOT=/path/to/your/android/sdk
```

### Keystore Configuration (for production builds)
```bash
# Production keystore settings
export ANDROID_KEYSTORE_PATH=/path/to/your/keystore.jks
export ANDROID_KEYSTORE_PASSWORD=your_store_password
export ANDROID_KEY_ALIAS=your_key_alias
export ANDROID_KEY_PASSWORD=your_key_password
```

### API Keys
```bash
# Gemini API key (if needed)
export GEMINI_API_KEY=your_gemini_api_key_here
```

### Build Configuration
```bash
# Build settings
export BUILD_TYPE=release
export BUILD_FLAVOR=production
export ENABLE_DEBUG_LOGGING=true
export ENABLE_CRASH_REPORTING=false
```

## 🏗️ Build Environments

### Development Environment
```bash
# Set development environment variables
export BUILD_TYPE=debug
export BUILD_FLAVOR=development
export ENABLE_DEBUG_LOGGING=true
export ENABLE_CRASH_REPORTING=false

# Generate configuration
./generate-local-properties.sh
```

### Production Environment
```bash
# Set production environment variables
export BUILD_TYPE=release
export BUILD_FLAVOR=production
export ANDROID_KEYSTORE_PATH=/path/to/production/keystore.jks
export ANDROID_KEYSTORE_PASSWORD=prod_password
export ANDROID_KEY_ALIAS=prod_alias
export ANDROID_KEY_PASSWORD=prod_key_password
export ENABLE_DEBUG_LOGGING=false
export ENABLE_CRASH_REPORTING=true

# Generate configuration
./generate-local-properties.sh
```

## 🔒 Security Best Practices

1. **Never commit sensitive information** to version control
2. **Use environment variables** for production credentials
3. **Set up CI/CD secrets** for automated builds
4. **Use different keystores** for debug and release builds
5. **Rotate API keys** regularly

## 🐳 Docker/CI Integration

### GitHub Actions Example
```yaml
- name: Set up Android SDK
  uses: android-actions/setup-jdk@v1
  with:
    distribution: 'zulu'
    java-version: '17'

- name: Set up Android SDK
  uses: android-actions/setup-android@v1

- name: Generate local.properties
  run: |
    export ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT
    export ANDROID_KEYSTORE_PATH=${{ secrets.ANDROID_KEYSTORE_PATH }}
    export ANDROID_KEYSTORE_PASSWORD=${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
    export ANDROID_KEY_ALIAS=${{ secrets.ANDROID_KEY_ALIAS }}
    export ANDROID_KEY_PASSWORD=${{ secrets.ANDROID_KEY_PASSWORD }}
    ./generate-local-properties.sh
```

### Docker Example
```dockerfile
# Set environment variables
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV BUILD_TYPE=release
ENV ENABLE_DEBUG_LOGGING=false

# Generate configuration
COPY generate-local-properties.sh /app/
RUN chmod +x /app/generate-local-properties.sh
RUN /app/generate-local-properties.sh
```

## 🔍 Troubleshooting

### SDK Not Found
```bash
# Check if Android SDK is installed
ls $ANDROID_SDK_ROOT

# Set the correct path
export ANDROID_SDK_ROOT=/correct/path/to/android/sdk
./generate-local-properties.sh
```

### Permission Issues
```bash
# Make script executable
chmod +x generate-local-properties.sh

# Run with proper permissions
./generate-local-properties.sh
```

### Environment Variables Not Working
```bash
# Check if variables are set
echo $ANDROID_SDK_ROOT
echo $ANDROID_KEYSTORE_PATH

# Set them if needed
export ANDROID_SDK_ROOT=/path/to/sdk
```

## 📝 Manual Configuration

If you prefer to set up manually:

1. Copy `local.properties.template` to `local.properties`
2. Replace the placeholder values with your actual configuration
3. Set environment variables as needed
4. Run your build

## 🔄 Updating Configuration

To update your configuration:

1. Modify environment variables
2. Run the generation script again
3. The script will overwrite `local.properties` with new values

```bash
# Update environment variables
export ANDROID_KEYSTORE_PATH=/new/path/to/keystore.jks

# Regenerate configuration
./generate-local-properties.sh
```

## 📊 Configuration Priority

The system uses this priority order:

1. **Environment Variables** (highest priority)
2. **local.properties** file
3. **gradle.properties** file
4. **Default values** (lowest priority)

This ensures that environment variables always override file-based configuration, making it perfect for CI/CD environments. 