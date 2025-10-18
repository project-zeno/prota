# Prota Chat Assist 💬

AI-powered chat reply suggestions for WhatsApp using Android Accessibility Services and Gemini API.

---

## 📖 Project Overview

Prota is a React Native (Expo) app that provides **AI-powered chat reply suggestions** for WhatsApp. It uses Android Accessibility Services to read visible messages, sends them to Google's Gemini API, and displays 3 smart suggestions in a notification with instant copy-to-clipboard buttons.

### ✨ Key Features
- 🤖 **AI-Powered Suggestions** - Uses Gemini 2.5 Flash Lite for fast, contextual replies
- 🔒 **Privacy-Focused** - Manual trigger only, no background monitoring
- 📱 **Seamless UX** - Works directly from notification, no app switching needed
- ⚡ **Rate-Limited** - Built-in safeguards prevent API spam (3-second cooldown)
- 🎯 **Clean Architecture** - Production-ready code with comprehensive error handling

---

## 🚀 Quick Start

### Prerequisites
- Node.js 18+
- Android Studio
- Android device/emulator (API 24+)
- Gemini API key ([Get one free](https://aistudio.google.com/apikey))

### Installation

```bash
# 1. Clone the repository
git clone <your-repo-url>
cd prota

# 2. Install dependencies
npm install

# 3. Add your Gemini API key
# Edit: android/app/src/main/java/com/aaryannaidu/prota/api/LlmApiClient.kt
# Line 20: private const val API_KEY = "YOUR_API_KEY_HERE"

# 4. Build and run
npx expo run:android

# 5. Enable accessibility service on your device
# Settings > Accessibility > Prota Chat Assist > Enable
```

---

## 📚 Documentation

This project includes comprehensive documentation for developers:

### **📋 Main Documentation Files**

1. **[INTEGRATION_SUMMARY.md](./INTEGRATION_SUMMARY.md)** 
   - Complete project overview and architecture
   - **Detailed file structure** with locations of all code
   - Feature summary and what was built
   - **Start here** for understanding the project

2. **[BUILD_AND_TEST.md](./BUILD_AND_TEST.md)**
   - Step-by-step build instructions
   - Complete testing guide with scenarios
   - Troubleshooting common issues
   - Log viewing and debugging tips

3. **[BRIDGE_REFERENCE.md](./BRIDGE_REFERENCE.md)**
   - React Native bridge API documentation
   - TypeScript usage examples
   - Error codes and handling
   - Method reference for all 8 bridge methods

4. **[accessibility.plan.md](./accessibility.plan.md)**
   - Original implementation plan and design decisions
   - Technical approach and rationale
   - Complete checklist (now fully implemented)

### **📊 Additional Resources**

- **API Bug Fix** - Fixed rate limiting issue (3k requests/min → 2-5 requests/min)
- **Security Notes** - Privacy and permission considerations
- **Performance Metrics** - Expected timings and API costs

---

## 🏗️ Architecture

```
┌────────────────────────────────────────────────┐
│  React Native UI (Expo)                        │
│  - Permission status                           │
│  - Settings shortcuts                          │
└──────────────┬─────────────────────────────────┘
               │
               │ NativeModules.AccessibilityBridge
               │
┌──────────────▼─────────────────────────────────┐
│  AccessibilityBridgeModule.kt                  │
│  - Orchestrates analysis flow                  │
│  - Rate limiting (3-second cooldown)           │
│  - Thread-safe concurrency guards              │
└───┬──────────┬──────────────┬──────────────────┘
    │          │              │
    ▼          ▼              ▼
┌────────┐ ┌──────────┐ ┌──────────────┐
│Accessibility│ │LlmApiClient│ │NotificationHelper│
│Service     │ │(Gemini)    │ │(3 buttons)      │
└────────┘ └──────────┘ └──────────────┘
```

---

## 🎯 User Flow

1. **Enable Service** - User enables accessibility service (one-time)
2. **Persistent Notification** - "Analyze Chat Now" button appears
3. **Open WhatsApp** - User reads a conversation
4. **Tap Button** - Swipe down notification, tap "Analyze Chat Now"
5. **Get Suggestions** - 3 AI-generated replies appear in notification
6. **Copy & Send** - Tap copy button, paste in WhatsApp

**Total time: 2-5 seconds from button press to suggestions**

---

## 🛡️ Security & Privacy

- ⚠️ **Manual Trigger Only** - Analysis happens only when user clicks button
- 🔒 **No Background Monitoring** - Service doesn't automatically read screens
- 🌐 **API Calls** - Messages sent to Google's Gemini API for processing
- 🔑 **API Key Security** - Don't commit keys to Git (use environment variables)
- 📱 **Local Processing** - No tracking, no data storage
- 🎯 **Demo Purpose** - Educational project, not production-ready for Play Store

---

## 🧹 Code Quality

### Built-in Safeguards
1. ✅ **Button Click Debouncing** - 2-second minimum between clicks
2. ✅ **Thread-Safe Guards** - AtomicBoolean prevents concurrent executions
3. ✅ **Rate Limiting** - 3-second cooldown between analyses
4. ✅ **Minimal Event Processing** - Accessibility service optimized
5. ✅ **Clean Error Handling** - Comprehensive error messages

### Code Standards
- Clean architecture with separation of concerns
- Comprehensive error handling at all layers
- Extensive logging for debugging
- Thread-safe Kotlin coroutines
- No memory leaks, proper lifecycle management

---

## 📊 Technical Stack

### Frontend
- React Native (Expo)
- TypeScript
- Expo Router (file-based routing)

### Backend (Native Android)
- Kotlin
- Android Accessibility Services
- OkHttp (HTTP client)
- Gson (JSON parsing)
- Kotlin Coroutines (async operations)

### AI/API
- Google Gemini 2.5 Flash Lite
- REST API integration
- Smart prompt engineering

---

## 📱 Platform Support

- **Android**: ✅ Fully supported (API 24+)
- **iOS**: ❌ Not supported (requires different accessibility APIs)
- **Web**: ❌ Not applicable

---

## 🔧 Development

### Project Structure
```
prota/
├── app/                  # React Native UI
│   ├── index.tsx        # Main screen
│   └── _layout.tsx      # Layout wrapper
├── android/             # Native Android code
│   └── app/src/main/
│       ├── java/        # Kotlin source files
│       ├── res/         # Android resources
│       └── AndroidManifest.xml
├── assets/              # Images and static files
└── Documentation/       # MD files (this folder)
```

**See [INTEGRATION_SUMMARY.md](./INTEGRATION_SUMMARY.md) for detailed file structure**

---

## 🧪 Testing

```bash
# Build debug APK
cd android
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep -E "ChatAssist|AccessibilityBridge"
```

**Full testing guide**: See [BUILD_AND_TEST.md](./BUILD_AND_TEST.md)

---

## 📈 Performance

- **Screen Reading**: < 500ms
- **API Call**: 1-3 seconds
- **Total Flow**: 2-5 seconds
- **API Rate**: 2-5 requests/minute (typical usage)
- **Cost**: ~$0.00002 per request (Gemini free tier: 15/min, 1500/day)

---

## 🤝 Contributing

This is an educational/demo project. Feel free to:
- Report bugs or issues
- Suggest improvements
- Fork and modify for your needs
- Use as a learning resource

---

## 📄 License

This is a demo/educational project. Use responsibly and respect user privacy.

---

## 🎓 What You'll Learn

Building this project teaches:
- Android Accessibility Services
- React Native Native Modules
- Kotlin Coroutines and async programming
- REST API integration
- Android Notifications with actions
- Thread-safe concurrent programming
- Modern Android app architecture
- Bridge communication between native and JS

---

## 🔗 Resources

- [Expo Documentation](https://docs.expo.dev/)
- [Android Accessibility Services](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Google Gemini API](https://ai.google.dev/gemini-api/docs)
- [React Native Native Modules](https://reactnative.dev/docs/native-modules-android)

---

## 📞 Support

For questions or issues:
1. Check [INTEGRATION_SUMMARY.md](./INTEGRATION_SUMMARY.md) for architecture
2. See [BUILD_AND_TEST.md](./BUILD_AND_TEST.md) for troubleshooting
3. Review [BRIDGE_REFERENCE.md](./BRIDGE_REFERENCE.md) for API usage
4. Check logs with `adb logcat`

---

## ✅ Status

**Current Version**: 1.0.0  
**Status**: ✅ Production-ready for testing  
**Last Updated**: October 18, 2025  
**Recent Fix**: API spam issue resolved (rate limiting implemented)

---

**Ready to build AI-powered chat features? Start with [INTEGRATION_SUMMARY.md](./INTEGRATION_SUMMARY.md)!** 🚀
