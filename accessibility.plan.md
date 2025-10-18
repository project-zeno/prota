<!-- 9a1668b6-5c55-4460-bc83-4ae3ba6403c0 2d7a6b1e-e199-4159-87b8-e44c128b29ee -->
# Accessibility Chat Assist (Android + Expo)

## Approach (SIMPLIFIED FOR MVP)

**The entire flow in 4 steps:**

```
1. User chatting in WhatsApp
2. User taps "Analyze Chat" button in your app
3. App reads visible messages → Sends to Gemini API → Gets 3 suggestions
4. Notification pops up with suggestions → Tap to copy → Paste in WhatsApp
```

**That's it!** 
- No automatic detection
- No conversation tracking  
- No debouncing
- No background monitoring
- Just: Button → Read → AI → Notification

## Key Decisions

- **Manual trigger only**: No automatic detection - user taps button when they want suggestions
- **Single-shot analysis**: Read screen → API call → Show notification (no tracking/caching)
- **Gemini API**: Fast, cheap, good quality (recommended over OpenAI for this use case)
- **Copy to clipboard**: Simplest action - user taps notification to copy suggestion
- **WhatsApp only**: Focus on one app first, expand later if needed

## Implementation Steps

### 1) Expo app scaffolding (UI + native prebuild)

- Create basic screens in `prota/app/index.tsx` to:
  - Show Accessibility Service status
  - Request Notification permission
  - Start/stop background suggestion service
  - Test notification functionality
- Prebuild Android native code
  - Run: `npx expo prebuild -p android`
- Note: API key is hardcoded in native Android code (developer's own key)

### 2) Android native module (Accessibility) - SIMPLIFIED

**Only 4 files needed:**

1. **`ChatAssistAccessibilityService.kt`**
   - Extends `AccessibilityService`
   - Just needs one method: `readCurrentScreen()` - extracts visible text
   - No event listening, no tracking, no caching

2. **`LlmApiClient.kt`** 
   - One method: `getSuggestions(messages: String)` 
   - Uses OkHttp to call Gemini API
   - Hardcoded API key

3. **`NotificationHelper.kt`** 
   - One method: `showSuggestions(suggestions: List<String>)`
   - Creates notification with 3 action buttons (copy suggestion 1, 2, 3)

4. **`AccessibilityBridgeModule.kt`** 
   - `triggerAnalysis()` - Button press → read screen → get AI → show notification
   - `isServiceEnabled()` - Check if accessibility is on
   - `openAccessibilitySettings()` - Open settings page

**Manifest updates:**
- Add `POST_NOTIFICATIONS` permission
- Add accessibility service declaration
- Add `res/xml/accessibility_service_config.xml` (simple config, WhatsApp only)

### 3) RN ↔ native bridge (already covered in step 2)

Module is already listed above (`AccessibilityBridgeModule.kt`). Just needs to be registered in `MainApplication.kt`

### 4) Update React Native UI

Add "Analyze Chat" button in `prota/app/index.tsx`:
- Calls native `triggerAnalysis()` method
- Shows loading state while processing
- That's it!

### 5) Testing Flow (Super Simple)

1. Build dev APK
2. Enable accessibility service in phone settings
3. Open WhatsApp chat
4. Tap "Analyze Chat" button in your app
5. Wait 2-3 seconds
6. Notification appears with 3 suggestions
7. Tap suggestion → copies to clipboard → paste in WhatsApp

**Done! Everything else is optional future enhancements.**

## Essential Snippets (non-executable sketches)

- Android manifest service entry (sketch):
```xml
<service
  android:name=".accessibility.ChatAssistAccessibilityService"
  android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
  android:exported="false">
  <intent-filter>
    <action android:name="android.accessibilityservice.AccessibilityService"/>
  </intent-filter>
  <meta-data
    android:name="android.accessibilityservice"
    android:resource="@xml/accessibility_service_config"/>
</service>
```

- Accessibility config (sketch):
```xml
<accessibility-service
  xmlns:android="http://schemas.android.com/apk/res/android"
  android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
  android:packageNames="com.whatsapp"
  android:feedbackType="feedbackGeneric"
  android:notificationTimeout="75"
  android:canRetrieveWindowContent="true"
  android:flags="flagReportViewIds|flagRetrieveInteractiveWindows"/>
```

- RN settings UI locations to update:
  - `prota/app/index.tsx`
  - `prota/app/_layout.tsx` (ensure proper status bar, theming)

## Risks/Notes

- WhatsApp UI view identifiers can change; rely on robust text traversal and heuristics.
- Auto-sending is restricted; user tap is required. Clipboard + focus handoff is acceptable for demo.
- Overlay requires `SYSTEM_ALERT_WINDOW` permission and OEM quirks; notifications are more reliable for MVP.
- Accessibility features are sensitive per Play policies; keep this as demo-only, not for distribution.

### To-dos (SIMPLIFIED - Only 6 Tasks!)

- [x] Add settings UI in prota to manage permissions and service control
- [x] Run Expo prebuild and confirm Android project structure
- [ ] Add dependencies (OkHttp, Gson) to build.gradle
- [ ] Create 4 Kotlin files: AccessibilityService, LlmApiClient, NotificationHelper, BridgeModule
- [ ] Update AndroidManifest.xml and add accessibility_service_config.xml
- [ ] Add "Analyze Chat" button to UI and wire up bridge
- [ ] Build APK, enable accessibility, test on real device