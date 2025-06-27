# Local LLM Integration for Android Document QA

This document describes the integration of local LLM (Large Language Model) capabilities using Google's MediaPipe LLM Inference API, allowing the app to run completely on-device without requiring an internet connection for LLM operations.

## Overview

The app now supports two LLM modes:
1. **Local LLM**: Uses Qwen2.5-1.5B-Instruct model running completely on-device via MediaPipe
2. **Remote LLM**: Uses Gemini API (cloud-based) as a fallback

## Features

- **On-device LLM processing**: No internet required for LLM operations
- **Automatic model management**: Downloads and manages the local model
- **Fallback mechanism**: Automatically switches to remote LLM if local model is unavailable
- **Visual status indicator**: Shows which LLM mode is currently active
- **Memory efficient**: Uses quantized models optimized for mobile devices
- **UI-based model download**: Easy-to-use interface for downloading models

## Technical Implementation

### Dependencies Added
- `com.google.mediapipe:tasks-genai:0.10.24` - MediaPipe LLM Inference API

### Key Components

1. **LocalLLMAPI**: Implements local LLM inference using MediaPipe
2. **ModelManager**: Handles model downloading and management with progress tracking
3. **LLMFactory**: Manages switching between local and remote LLMs
4. **LLMProvider**: Interface for abstracting LLM implementations
5. **ModelDownloadViewModel**: Manages model download state and progress
6. **ModelDownloadScreen**: UI for model download management

### Model Details
- **Model**: Qwen2.5-1.5B-Instruct (8-bit quantized)
- **Size**: ~3.2GB (downloaded once)
- **Storage**: `/data/local/tmp/llm/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task`
- **Performance**: Optimized for high-end Android devices (Pixel 8, Samsung S23+)
- **Source**: Hugging Face LiteRT community repository

## Usage Instructions

### First Time Setup

1. **Launch the app**: The app will automatically attempt to download the local model on first use
2. **Access model download**: Tap the download icon (⬇️) in the top bar if local model is not available
3. **Wait for download**: The model download may take several minutes depending on your internet connection
4. **Verify status**: Check the LLM status indicator in the top bar:
   - 🖥️ Green computer icon = Local LLM active
   - ☁️ Blue cloud icon = Remote LLM active
   - 🔑 Red key icon = No LLM available

### Using Local LLM

1. **No API key required**: Local LLM works without any API key configuration
2. **Offline operation**: Once the model is downloaded, you can use the app without internet
3. **Automatic fallback**: If local model fails, the app automatically switches to remote LLM (if API key is configured)

### Model Management

The app automatically:
- Downloads the model on first use
- Checks for existing model before downloading
- Manages model storage and cleanup
- Provides download progress logging
- Shows real-time download progress in the UI

## Performance Considerations

### Device Requirements
- **Recommended**: High-end Android devices (Pixel 8, Samsung S23+)
- **Minimum**: Android API 26+ with sufficient RAM (4GB+)
- **Storage**: At least 4GB free space for model download

### Performance Tips
- **First run**: Initial model loading may take 10-30 seconds
- **Subsequent runs**: Model loads much faster after first initialization
- **Memory usage**: Model uses approximately 3-4GB RAM during inference
- **Battery impact**: Local inference may use more battery than cloud API

## Troubleshooting

### Model Download Issues
- **Check internet connection**: Model download requires stable internet
- **Verify storage space**: Ensure at least 4GB free space
- **Check permissions**: App needs storage and internet permissions
- **Use manual download**: Use the provided download scripts if automatic download fails

### Performance Issues
- **Close other apps**: Free up RAM for better performance
- **Restart app**: If model loading fails, restart the app
- **Check device compatibility**: Ensure your device meets minimum requirements

### Fallback to Remote LLM
If local LLM fails:
1. Check if you have a valid Gemini API key configured
2. The app will automatically switch to remote LLM
3. You'll see the cloud icon in the status indicator

## Development Notes

### Architecture Changes
- Replaced direct Gemini API usage with LLM abstraction layer
- Added model management and download capabilities
- Implemented automatic fallback mechanism
- Added visual status indicators
- Created dedicated model download UI

### Code Structure
```
domain/llm/
├── LLMProvider.kt          # Interface for LLM implementations
├── LocalLLMAPI.kt          # MediaPipe local LLM implementation
├── GeminiRemoteAPI.kt      # Remote Gemini API implementation
├── ModelManager.kt         # Model download and management
└── LLMFactory.kt           # LLM instance management

ui/screens/model_download/
├── ModelDownloadViewModel.kt  # Model download state management
└── ModelDownloadScreen.kt     # Model download UI
```

### Configuration
- Model path: `/data/local/tmp/llm/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task`
- Model URL: Hugging Face LiteRT community repository
- Default parameters: maxTokens=1024, topK=64, temperature=0.8

## Future Enhancements

- **Model selection**: Allow users to choose different local models
- **LoRA support**: Enable custom model fine-tuning
- **Model compression**: Further optimize model size and performance
- **Batch processing**: Support for processing multiple queries efficiently
- **Model updates**: Automatic model version management

## References

- [MediaPipe LLM Inference Guide](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)
- [Qwen2.5 Model Documentation](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct)
- [LiteRT Community Models](https://huggingface.co/litert-community) 