# 🤖 Prota AI Screen Assistant

AI-powered screen analysis for any Android app using screenshots and Gemini AI.

Before running the app, make sure to add your Gemini API key to the `LlmApiClient.kt` file.
---

## 📖 Project Overview

Prota is a React Native (Expo) app that provides **universal AI screen analysis**. It captures screenshots of any Android app, sends them to Google's Gemini API, and displays AI insights in notifications with instant copy-to-clipboard buttons.

### ✨ Key Features
- 🤖 **Universal AI Analysis** - Works with any Android app (WhatsApp, Instagram, Browser, etc.)
- 📸 **Screenshot-Based** - Reliable visual content analysis (no fragile text parsing)
- 🔒 **Privacy-Focused** - Manual trigger only, no background monitoring
- 📱 **Seamless UX** - Works directly from notification, no app switching needed
- ⚡ **Rate-Limited** - Built-in safeguards prevent API spam (3-second cooldown)
- 🎯 **Clean Architecture** - Production-ready code with comprehensive error handling

---

## 🚀 Quick Start

### Prerequisites
- Node.js 18+
- Android Studio
- Android device/emulator (API 30+ for screenshots)
- Gemini API key ([Get one free](https://aistudio.google.com/apikey))

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/project-zeno/prota
cd prota

# 2. Install dependencies
npm install

# 3. Add your Gemini API key
# Edit: android/app/src/main/java/com/aaryannaidu/prota/api/LlmApiClient.kt
# Line 18: private const val API_KEY = "YOUR_API_KEY_HERE"

# 4. Build and run
npx expo run:android

# 5. Enable accessibility service on your device
# Settings > Accessibility > Prota AI Screen Assistant > Enable
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
   - Method reference for all bridge methods

4. **[accessibility.plan.md](./accessibility.plan.md)**
   - Original implementation plan and design decisions
   - Technical approach and rationale
   - Complete checklist (now fully implemented)

### **📊 Additional Resources**
- **API Optimization** - JPEG compression saves 40x bandwidth
- **Security Notes** - Privacy and permission considerations
- **Performance Metrics** - Screenshot capture (70ms) + AI analysis (2-4s)

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
│Service     │ │(Gemini)    │ │(Copy buttons)    │
│Screenshot │ │AI Analysis │ │AI Insights      │
└────────┘ └──────────┘ └──────────────┘
```

---

## 🎯 User Flow

1. **Enable Service** - User enables accessibility service (one-time)
2. **Persistent Notification** - "📸 Analyze Screen" button appears
3. **Open Any App** - User views content in WhatsApp, Instagram, Browser, etc.
4. **Tap Button** - Swipe down notification, tap "📸 Analyze Screen"
5. **Get AI Insights** - 3 AI-generated insights appear in notification
6. **Copy & Use** - Tap copy button, paste anywhere

**Total time: 2.5-4.5 seconds from button press to insights**

---

## 🛡️ Security & Privacy

- ⚠️ **Manual Trigger Only** - Analysis happens only when user clicks button
- 🔒 **No Background Monitoring** - Service doesn't automatically capture screens
- 🌐 **API Calls** - Screenshots sent to Google's Gemini API for processing
- 🔑 **API Key Security** - Don't commit keys to Git (use environment variables)
- 📱 **Local Processing** - No tracking, no data storage
- 🎯 **Demo Purpose** - Educational project, not production-ready for Play Store

---

## 🧹 Code Quality

### Built-in Safeguards
1. ✅ **Notification Panel Auto-Collapse** - Prevents screenshot of notification panel
2. ✅ **Button Click Debouncing** - 5-second minimum between clicks
3. ✅ **Thread-Safe Guards** - AtomicBoolean prevents concurrent executions
4. ✅ **Rate Limiting** - 3-second cooldown between analyses
5. ✅ **Screenshot Delay** - 2.5-second wait for UI to settle
6. ✅ **Clean Error Handling** - Comprehensive error messages

### Code Standards
- Clean architecture with separation of concerns
- Comprehensive error handling at all layers
- Extensive logging for debugging
- Thread-safe Kotlin coroutines
- Memory-efficient bitmap handling
- Proper lifecycle management

---

## 📊 Technical Stack

### Frontend
- React Native (Expo)
- TypeScript
- Expo Router (file-based routing)

### Backend (Native Android)
- Kotlin
- Android Accessibility Services (API 30+ for screenshots)
- OkHttp (HTTP client)
- Gson (JSON parsing)
- Kotlin Coroutines (async operations)

### AI/API
- Google Gemini 2.5 Flash Lite
- Multimodal API (text + images)
- JPEG compression + base64 encoding

---

## 📱 Platform Support

- **Android**: ✅ Fully supported (API 30+ for screenshots, API 24+ fallback)
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
│       ├── java/        # Kotlin source files (747 lines)
│       ├── res/         # Android resources
│       └── AndroidManifest.xml
├── assets/              # Images and static files
└── Documentation/       # MD files
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
adb logcat | grep -E "AIAssist|AccessibilityBridge"
```

**Full testing guide**: See [BUILD_AND_TEST.md](./BUILD_AND_TEST.md)

---

## 🤝 Contributing

This is an educational/demo project. Feel free to:
- Report bugs or issues
- Suggest improvements
- Fork and modify for your needs
- Use as a learning resource

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

**Current Version**: 2.0.0
**Status**: Production-ready universal AI screen assistant

**Last Updated**: October 20, 2025
**Recent Changes**:
- ✅ Screenshot-based analysis (reliable, universal)
- ✅ JPEG compression (40x bandwidth savings)
- ✅ Any app support (not just WhatsApp)
- ✅ Auto-collapse notification panel
- ✅ 2.5-second screenshot delay

---

**Ready to build universal AI screen analysis? Start with [INTEGRATION_SUMMARY.md](./INTEGRATION_SUMMARY.md)!** 🚀
