# Build & Test Guide - Prota Chat Assist

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
1. Install Prota on your Android device or emulator
2. Enable the Accessibility Service:
   - Settings > Accessibility > Prota Chat Assist > Enable
3. Open WhatsApp and a chat with visible messages
4. Swipe down notification shade, tap "Analyze Chat Now"
5. Wait for notification with 3 suggestions
6. Tap a "Copy" button, paste in WhatsApp

---

For troubleshooting or logs:
```
adb logcat | grep -E "ChatAssist|AccessibilityBridge"
```

For more details, see README.md or contact the developer.

