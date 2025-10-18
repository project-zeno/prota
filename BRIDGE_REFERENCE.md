# AccessibilityBridge Module - React Native Reference

## How to Build & Test Quickly
- See [BUILD_AND_TEST.md](./BUILD_AND_TEST.md) for super simple instructions to build and run the app on your device/emulator. Supports Expo/EAS, Android Studio, or Gradle CLI.
- Once installed and permissions enabled, **you only need to call `triggerAnalysis()` to test the full native chat suggestion workflow**.

---

This document explains how to use the native Android bridge module from React Native.

## Import the Module

```typescript
import { NativeModules } from 'react-native';

const { AccessibilityBridge } = NativeModules;
```

## Available Methods

### 1. `triggerAnalysis()` - Main Flow ⭐

**Triggers the complete chat analysis flow**

```typescript
try {
  const result = await AccessibilityBridge.triggerAnalysis();
  console.log('Success:', result);
  // result = {
  //   status: "success",
  //   suggestionCount: 3,
  //   suggestions: ["Reply 1", "Reply 2", "Reply 3"]
  // }
} catch (error) {
  console.error('Error:', error.code, error.message);
  // Possible error codes:
  // - "SERVICE_NOT_ENABLED": Accessibility service not enabled
  // - "NOTIFICATION_PERMISSION_DENIED": Notification permission required
  // - "NO_MESSAGES": No chat messages found on screen
  // - "API_ERROR": Gemini API failed
  // - "ANALYSIS_ERROR": General error during analysis
}
```

**What it does:**
1. Checks if accessibility service is enabled
2. Reads messages from WhatsApp screen
3. Sends to Gemini API for suggestions
4. Shows notification with 3 suggestions

---

### 2. `isServiceEnabled()` - Check Service Status

**Check if accessibility service is currently running**

```typescript
const isEnabled = await AccessibilityBridge.isServiceEnabled();
console.log('Service enabled:', isEnabled); // true or false
```

Use this to show/hide the "Enable Service" button in your UI.

---

### 3. `openAccessibilitySettings()` - Open Settings

**Opens Android Accessibility Settings page**

```typescript
await AccessibilityBridge.openAccessibilitySettings();
// User will be taken to Settings > Accessibility
// They can enable "Prota Chat Assist" service there
```

---

### 4. `checkNotificationPermission()` - Check Notification Permission

**Check if app can show notifications**

```typescript
const isGranted = await AccessibilityBridge.checkNotificationPermission();
console.log('Notification permission:', isGranted); // true or false
```

---

### 5. `shouldRequestNotificationPermission()` - Should Request Permission

**Check if we need to request notification permission (Android 13+)**

```typescript
const shouldRequest = await AccessibilityBridge.shouldRequestNotificationPermission();
if (shouldRequest) {
  // Show permission request dialog
  // (You need to implement this in MainActivity for Android 13+)
}
```

---

### 6. `openNotificationSettings()` - Open Notification Settings

**Opens app's notification settings**

```typescript
await AccessibilityBridge.openNotificationSettings();
// User can enable notifications manually
```

---

### 7. `dismissNotification()` - Dismiss Notification

**Programmatically dismiss the suggestion notification**

```typescript
await AccessibilityBridge.dismissNotification();
```

---

### 8. `testBridge()` - Test Connection

**Test if the bridge is working**

```typescript
const response = await AccessibilityBridge.testBridge("Hello from React Native");
console.log(response); // "Bridge working! Received: Hello from React Native"
```

---

## Constants

The module exports some constants:

```typescript
console.log(AccessibilityBridge.MODULE_NAME); // "AccessibilityBridge"
console.log(AccessibilityBridge.REQUIRES_NOTIFICATION_PERMISSION); // true on Android 13+
```

---

## Complete Example Usage

```typescript
import React, { useState, useEffect } from 'react';
import { View, Button, Text, Alert } from 'react-native';
import { NativeModules } from 'react-native';

const { AccessibilityBridge } = NativeModules;

export default function ChatAssistScreen() {
  const [serviceEnabled, setServiceEnabled] = useState(false);
  const [loading, setLoading] = useState(false);

  // Check service status on mount
  useEffect(() => {
    checkStatus();
  }, []);

  const checkStatus = async () => {
    const enabled = await AccessibilityBridge.isServiceEnabled();
    setServiceEnabled(enabled);
  };

  const handleAnalyze = async () => {
    setLoading(true);
    try {
      // Main analysis flow
      const result = await AccessibilityBridge.triggerAnalysis();
      Alert.alert('Success!', `Generated ${result.suggestionCount} suggestions`);
    } catch (error) {
      // Handle errors with helpful messages
      if (error.code === 'SERVICE_NOT_ENABLED') {
        Alert.alert(
          'Service Not Enabled',
          'Please enable the accessibility service',
          [
            { text: 'Cancel', style: 'cancel' },
            { 
              text: 'Open Settings', 
              onPress: () => AccessibilityBridge.openAccessibilitySettings() 
            }
          ]
        );
      } else if (error.code === 'NO_MESSAGES') {
        Alert.alert('No Messages', 'Please open a WhatsApp chat first');
      } else {
        Alert.alert('Error', error.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <View>
      {!serviceEnabled ? (
        <Button
          title="Enable Accessibility Service"
          onPress={() => AccessibilityBridge.openAccessibilitySettings()}
        />
      ) : (
        <Button
          title={loading ? "Analyzing..." : "Analyze Chat"}
          onPress={handleAnalyze}
          disabled={loading}
        />
      )}
    </View>
  );
}
```

---

## Error Handling

All methods return Promises and should be wrapped in try-catch:

```typescript
try {
  await AccessibilityBridge.someMethod();
} catch (error) {
  console.error('Code:', error.code);
  console.error('Message:', error.message);
}
```

Common error codes:
- `SERVICE_NOT_ENABLED` - Accessibility service not enabled
- `NOTIFICATION_PERMISSION_DENIED` - Notification permission required
- `NO_MESSAGES` - No messages found on screen
- `API_ERROR` - Gemini API call failed
- `ANALYSIS_ERROR` - General error during analysis
- `SETTINGS_ERROR` - Failed to open settings
- `UNEXPECTED_ERROR` - Unknown error

---

## Testing

Before testing on a real device:

1. **Add your Gemini API key** in `LlmApiClient.kt`:
   ```kotlin
   private const val API_KEY = "YOUR_ACTUAL_API_KEY_HERE"
   ```

2. **Build the APK**:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

3. **Enable accessibility service**:
   - Settings > Accessibility > Prota Chat Assist > Enable

4. **Grant notification permission** (Android 13+):
   - Settings > Apps > Prota > Notifications > Allow

5. **Test the flow**:
   - Open WhatsApp chat
   - Open your app
   - Tap "Analyze Chat"
   - Wait for notification
   - Tap "Copy" button
   - Paste in WhatsApp

---

## Architecture Overview

```
React Native UI
    ↓
NativeModules.AccessibilityBridge
    ↓
AccessibilityBridgeModule.kt (Kotlin)
    ↓
┌─────────────────────────────────────┐
│ ChatAssistAccessibilityService.kt   │ → Reads WhatsApp screen
│ LlmApiClient.kt                     │ → Calls Gemini API
│ NotificationHelper.kt               │ → Shows notification
└─────────────────────────────────────┘
```

---

## Next Steps

See `accessibility.plan.md` for remaining tasks:
- Update AndroidManifest.xml
- Add accessibility_service_config.xml
- Wire up the "Analyze Chat" button in UI
- Build and test on real device

