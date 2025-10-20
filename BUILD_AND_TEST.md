# Build & Test Guide - Prota AI Screen Assistant

## 1. EAS/Expo Go (Simplest)
```
npm install
npx expo run:android
```
- Follows the prompts and the app will open in an emulator or attached device.

## 2. Android Studio (Recommended for debugging)
- Open the `prota/android` folder in Android Studio
- Wait for Gradle sync
- Click ➡️ Run to select your device or emulator

## 3. Manual APK via Gradle CLI
```
cd prota/android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Testing Checklist

### Setup (One-time)
1. **Install Prota** on your Android device or emulator (API 30+ recommended)
2. **Enable Accessibility Service**:
   - Settings > Accessibility > Prota AI Screen Assistant > Enable
3. **Grant Notification Permission** (if prompted)

### Testing the AI Assistant
4. **Open any Android app** (WhatsApp, Instagram, Chrome, etc.)
5. **Pull down notification shade**, you should see: "🤖 AI Screen Assistant"
6. **Tap "📸 Analyze Screen"** button
7. **Notification panel auto-collapses** (important!)
8. **Wait 2.5 seconds** (toast shows countdown)
9. **AI insights appear** in new notification with copy buttons
10. **Tap "Copy 1/2/3"** to copy insight to clipboard

### Testing Different Scenarios
- **WhatsApp chat**: "Suggest 3 reply options"
- **Instagram post**: "Describe this image"
- **Error message**: "What does this error mean?"
- **Article**: "Summarize this page"
- **Custom prompt**: Use `analyzeScreenWithPrompt()` in React Native

---

## Troubleshooting

### Common Issues

#### ❌ "Screenshot failed" error
- **Cause**: Android API level too low
- **Solution**: Requires Android 11+ (API 30) for modern screenshots
- **Check**: `adb shell getprop ro.build.version.sdk`

#### ❌ No notification appears
- **Check**: Notification permission granted
- **Check**: Accessibility service enabled
- **Check**: Logs for errors: `adb logcat | grep AIAssist`

#### ❌ Same insights every time
- **Cause**: Notification panel not auto-collapsing
- **Check**: When you tap "Analyze Screen", notification panel should disappear immediately
- **Manual test**: Close notification panel manually before analysis

#### ❌ API errors
- **Check**: Gemini API key is set correctly in `LlmApiClient.kt`
- **Check**: Internet connection
- **Check**: API quota not exceeded

#### ❌ Service not enabled
- **Go to**: Settings > Accessibility > Prota AI Screen Assistant
- **Enable** the service
- **Restart** the app

---

## Log Viewing

### View all logs
```bash
adb logcat | grep -E "AIAssist|AccessibilityBridge|LlmApiClient"
```

### View only errors
```bash
adb logcat | grep -E "AIAssist|AccessibilityBridge" | grep -i error
```

### View screenshot process
```bash
adb logcat | grep -i screenshot
```

### View API calls
```bash
adb logcat | grep -i gemini
```

---

## Performance Expectations

### Timing Breakdown
- **Button tap → Panel collapse**: Instant (0ms)
- **UI settle delay**: 2.5 seconds
- **Screenshot capture**: ~70ms
- **AI API call**: 2-4 seconds
- **Total UX time**: 4.5-6.5 seconds

### Network Impact
- **Screenshot size**: ~200KB (JPEG compressed)
- **API payload**: ~280KB (base64 encoded)
- **Response**: ~1KB (3 insights)

### Battery/Performance
- **CPU usage**: Minimal (only during analysis)
- **Memory**: Efficient bitmap recycling
- **Network**: One API call per analysis
- **Background**: Zero activity when not analyzing

---

## Advanced Testing

### Custom Prompts Testing
Use the React Native bridge to test custom analysis:

```typescript
// In your React Native code
const { AccessibilityBridge } = NativeModules;

// Test different prompts
await AccessibilityBridge.analyzeScreenWithPrompt(
  "What language is this text in?"
);

await AccessibilityBridge.analyzeScreenWithPrompt(
  "Count how many people are in this image"
);
```

### Multiple App Testing
Test with various apps:
- **Social**: WhatsApp, Instagram, Twitter
- **Browser**: Chrome, Firefox
- **Messaging**: Telegram, Signal
- **Productivity**: Gmail, Docs, Calendar
- **Entertainment**: YouTube, Netflix

---

## Development Tips

### Quick Iteration
1. Make code changes
2. Build: `npx expo run:android`
3. Test with: `adb logcat | grep AIAssist`
4. Iterate

### API Key Setup
```kotlin
// android/app/src/main/java/com/aaryannaidu/prota/api/LlmApiClient.kt
private const val API_KEY = "your_actual_api_key_here"
```

### Debug Builds
```bash
# Clean build
cd android && ./gradlew clean

# Debug APK
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Success Criteria

✅ **App installs** without errors
✅ **Accessibility service enables** in settings
✅ **Notification appears** when service is active
✅ **Screenshot captures** current app (not notification panel)
✅ **AI generates insights** relevant to screen content
✅ **Copy buttons work** (clipboard receives text)
✅ **Rate limiting works** (no spam analyses)

---

**Testing complete? Your universal AI screen assistant is ready!** 🚀