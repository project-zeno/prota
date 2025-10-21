# AccessibilityBridge Module - React Native Reference

## How to Build & Test Quickly
- See [BUILD_AND_TEST.md](./BUILD_AND_TEST.md) for super simple instructions to build and run the app on your device/emulator. Supports Expo/EAS, Android Studio, or Gradle CLI.
- Once installed and permissions enabled, **you only need to call `triggerAnalysis()` to test the full native AI screen analysis workflow**.

---

This document explains how to use the native Android bridge module from React Native.

## Import the Module

```typescript
import { NativeModules } from 'react-native';

const { AccessibilityBridge } = NativeModules;
```

## Available Methods

### 1. `triggerAnalysis()` - Main Flow ⭐

**Triggers the complete AI screen analysis flow**

```typescript
try {
  const result = await AccessibilityBridge.triggerAnalysis();
  console.log('Success:', result);
  // result = {
  //   status: "success",
  //   suggestionCount: 3,
  //   suggestions: ["Insight 1", "Insight 2", "Insight 3"]
  // }
} catch (error) {
  console.error('Error:', error.code, error.message);
  // Possible error codes:
  // - "SERVICE_NOT_ENABLED": Accessibility service not enabled
  // - "NOTIFICATION_PERMISSION_DENIED": Notification permission required
  // - "SCREENSHOT_FAILED": Failed to capture screenshot
  // - "API_ERROR": Gemini API failed
  // - "ANALYSIS_ERROR": General error during analysis
}
```

**What it does:**
1. Checks if accessibility service is enabled
2. Captures screenshot of current screen (JPEG compressed)
3. Sends to Gemini API requesting JSON response format
4. Parses structured JSON response with 3 insights
5. Shows notification with AI-generated insights

---

### 2. `analyzeScreenWithPrompt(customPrompt)` - Custom Analysis ⭐

**Analyze screen with your own custom prompt**

```typescript
try {
  const result = await AccessibilityBridge.analyzeScreenWithPrompt(
    "Translate any visible text to Spanish"
  );
  console.log('Custom analysis result:', result);
  // result = {
  //   status: "success",
  //   suggestionCount: 3,
  //   prompt: "Translate any visible text to Spanish",
  //   suggestions: ["Translation 1", "Translation 2", "Translation 3"]
  // }
} catch (error) {
  console.error('Custom analysis error:', error);
}
```

**Common custom prompts:**
- `"Summarize this page in 3 bullet points"`
- `"What does this error message mean?"`
- `"Translate this text to French"`
- `"Explain what I'm looking at"`
- `"Help me reply to this message"`

---

### 3. `isServiceEnabled()` - Check Service Status

**Check if accessibility service is currently running**

```typescript
const isEnabled = await AccessibilityBridge.isServiceEnabled();
console.log('Service enabled:', isEnabled); // true or false
```

Use this to show/hide the "Enable Service" button in your UI.

---

### 4. `openAccessibilitySettings()` - Open Settings

**Opens Android Accessibility Settings page**

```typescript
await AccessibilityBridge.openAccessibilitySettings();
// User will be taken to Settings > Accessibility
// They can enable "Prota AI Screen Assistant" service there
```

---

### 5. `checkNotificationPermission()` - Check Notification Permission

**Check if app can show notifications**

```typescript
const isGranted = await AccessibilityBridge.checkNotificationPermission();
console.log('Notification permission:', isGranted); // true or false
```

---

### 6. `dismissNotification()` - Dismiss Results

**Dismiss any active AI insight notifications**

```typescript
await AccessibilityBridge.dismissNotification();
// Clears the notification showing AI analysis results
```

---

### 7. `testBridge(message)` - Test Connection

**Test the bridge connection**

```typescript
const response = await AccessibilityBridge.testBridge("Hello from RN!");
console.log(response); // "Bridge working! Received: Hello from RN!"
```

---

## Error Handling

All methods return Promises that resolve with success data or reject with error objects:

```typescript
{
  code: "ERROR_CODE",      // Machine-readable error code
  message: "Human readable message",
  userInfo?: {}           // Additional error details (optional)
}
```

### Common Error Codes

| Error Code | Description | Solution |
|------------|-------------|----------|
| `SERVICE_NOT_ENABLED` | Accessibility service not running | Enable in Settings > Accessibility |
| `NOTIFICATION_PERMISSION_DENIED` | No notification permission | Grant in app settings |
| `SCREENSHOT_FAILED` | Screenshot capture failed | Check Android version (needs API 30+) |
| `API_ERROR` | Gemini API call failed | Check API key and internet connection |
| `ANALYSIS_ERROR` | General analysis error | Check logs, try again |
| `RATE_LIMIT` | Too many requests | Wait 3+ seconds between analyses |

---

## Method Reference

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `triggerAnalysis()` | None | `Promise<Result>` | Full analysis with default prompt |
| `analyzeScreenWithPrompt(prompt)` | `string` | `Promise<Result>` | Custom prompt analysis |
| `isServiceEnabled()` | None | `Promise<boolean>` | Service status check |
| `openAccessibilitySettings()` | None | `Promise<void>` | Open accessibility settings |
| `checkNotificationPermission()` | None | `Promise<boolean>` | Notification permission check |
| `dismissNotification()` | None | `Promise<void>` | Dismiss result notifications |
| `testBridge(message)` | `string` | `Promise<string>` | Test bridge connection |

---

## Result Format

All analysis methods return the same result structure:

```typescript
interface AnalysisResult {
  status: "success";
  suggestionCount: number;    // Usually 3
  suggestions: string[];     // Array of AI insights
  prompt?: string;           // Only in analyzeScreenWithPrompt()
}
```

---

## Usage Examples

### Basic Analysis
```typescript
// Simple analysis with default prompt
const result = await AccessibilityBridge.triggerAnalysis();
console.log(`${result.suggestionCount} insights generated`);
result.suggestions.forEach((insight, i) => {
  console.log(`${i+1}. ${insight}`);
});
```

### Custom Analysis
```typescript
// Analyze with specific question
const result = await AccessibilityBridge.analyzeScreenWithPrompt(
  "What are the main points in this article?"
);
console.log(`Analysis for: "${result.prompt}"`);
```

### Error Handling
```typescript
try {
  const result = await AccessibilityBridge.triggerAnalysis();
  // Handle success
} catch (error) {
  switch (error.code) {
    case 'SERVICE_NOT_ENABLED':
      // Prompt user to enable service
      break;
    case 'SCREENSHOT_FAILED':
      // Show Android version requirement
      break;
    default:
      // Generic error
      break;
  }
}
```

---

## Performance Notes

- **Screenshot capture**: ~70ms (includes JPEG compression)
- **AI analysis**: 2-4 seconds (network dependent)
- **Total UX time**: 2.5-4.5 seconds
- **Rate limit**: 3-second cooldown between analyses

The bottleneck is the AI API call - local processing is very fast!

---

## Platform Requirements

- **Android API 30+**: Full screenshot support
- **Android API 24+**: Fallback support (may not work on all devices)
- **iOS**: Not supported
- **Permissions**: Accessibility service + Notifications

---

**Need the bridge API? This is your complete reference!** 📚