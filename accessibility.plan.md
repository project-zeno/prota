<!-- 9a1668b6-5c55-4460-bc83-4ae3ba6403c0 2d7a6b1e-e199-4159-87b8-e44c128b29ee -->
# Accessibility Chat Assist (Android + Expo)

## Approach

- Use Android AccessibilityService to observe WhatsApp chat screens, extract visible messages, and detect the active conversation.
- Send recent chat context to a cloud LLM API; return 2-3 suggestions + action hints.
- MVP surface: Android notification with quick actions (fastest to ship, robust, no overlay permission).
- Phase 2: Optional floating overlay bubble/chip using `TYPE_APPLICATION_OVERLAY`.

## Key Decisions

- Suggestions generation: Cloud API (e.g., OpenAI/Gemini) called directly from Android native code.
- First surface: Notifications with action buttons (copy, speak, open WhatsApp with prefilled text intent).
- Target app to start: WhatsApp; architect selectors to be data-driven for other apps later.

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

### 2) Android native module (Accessibility)

- Files to add under `android/app/src/main/java/.../accessibility/`:
  - `ChatAssistAccessibilityService.kt`: extends `AccessibilityService`
    - Listen for `TYPE_WINDOW_CONTENT_CHANGED`, `TYPE_WINDOW_STATE_CHANGED`
    - Filter for `packageName == com.whatsapp`
    - Parse view hierarchy for message list (use role/class names, content descriptions, text aggregation)
    - Debounce, build recent chat transcript (last ~8-12 messages, role-tagged)
    - Post work to `SuggestionWorker` (WorkManager) or a foreground service
  - `SuggestionWorker.kt` (or `SuggestionService.kt`):
    - Receive chat context payload
    - Call Gemini/OpenAI API directly (HTTP POST)
    - Cache recent suggestions keyed by conversation
  - `NotificationHelper.kt`:
    - Build channel and show heads-up notification with suggestions as action buttons
    - Actions: Copy to clipboard, TTS speak, `ACTION_SENDTO`/deep-link back to WhatsApp with text
  - `ClipboardUtils.kt`, `TtsUtils.kt`, `IntentUtils.kt`
- Manifest updates in `android/app/src/main/AndroidManifest.xml`:
  - `<service android:name=.accessibility.ChatAssistAccessibilityService ... android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">`
    - Add `<intent-filter><action android:name="android.accessibilityservice.AccessibilityService"/></intent-filter>`
    - Provide `res/xml/accessibility_service_config.xml` with event types, feedback type, target packages (WhatsApp), flags
  - Permissions: `BIND_ACCESSIBILITY_SERVICE`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE` (if needed)
- Resources:
  - `res/xml/accessibility_service_config.xml`
  - `res/values/strings.xml` entries for service labels

### 3) RN ↔ native bridge

- Create `AccessibilityBridgeModule.kt` exposing:
  - `isServiceEnabled()` - Check if accessibility service is active
  - `openAccessibilitySettings()` - Open system accessibility settings
  - `requestNotificationPermission()` - Request notification permission
  - `testNotification()` - Send test notification with sample suggestions
  - `checkPermissions()` - Return status of all required permissions
- Register module/package; call from `prota` UI

### 4) Direct API integration (Gemini/OpenAI)

- Add `LlmApiClient.kt` in Android native code:
  - Use OkHttp/Retrofit to call Gemini or OpenAI API directly from native
  - Hardcode API key in BuildConfig or gradle.properties for security
  - Build prompt from chat context: "You are a helpful chat assistant. Given this conversation, suggest 2-3 contextually appropriate replies..."
  - Parse JSON response and return suggestions list
- No backend needed; API calls directly from Android service
- Recommended: Use Gemini API for faster responses and better rate limits

### 5) Notifications MVP (ship fast)

- Trigger notification on new incoming/outgoing message events (debounced)
- Show up to 3 suggestions as actions
- Action behavior:
  - Copy: Copy to clipboard; toast
  - Speak: TTS speak
  - Reply: Open WhatsApp chat via intent; optionally prefill via clipboard + instructions (WhatsApp restricts auto-send)

### 6) Phase 2: Overlay bubble (optional)

- Add `BubbleService.kt` drawing a small draggable chip using `WindowManager` with `TYPE_APPLICATION_OVERLAY`
- Tap opens a small view with suggestions; long-press to dismiss
- Ask for `SYSTEM_ALERT_WINDOW` permission via settings intent

### 7) App UI polish and states

- `prota/app/index.tsx`:
  - Status tiles: Accessibility enabled/disabled, Notification permission, Service running/stopped
  - Buttons: Open Accessibility settings; Request notifications; Start/Stop service; Test notification
  - Dark theme with modern UI design

### 8) Hardening

- Debounce and rate-limit calls (e.g., min 2-4s between requests per conversation)
- Handle offline or API errors gracefully with cached last suggestion
- Battery: Use foreground service only when necessary; otherwise WorkManager
- Privacy: Process only visible text; never store unless user opts in

### 9) Packaging and demo

- Provide a simple seed script to enable required settings checklist
- Prepare a test flow video demonstrating WhatsApp conversation suggestions

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

### To-dos

- [x] Add settings UI in prota to manage permissions and service control
- [ ] Run Expo prebuild and confirm Android project structure
- [ ] Create ChatAssistAccessibilityService with WhatsApp filtering and text extraction
- [ ] Implement LlmApiClient for direct Gemini/OpenAI calls and caching (with hardcoded API key)
- [ ] Show heads-up notifications with suggestion actions
- [ ] Expose native state and settings to RN via bridge module
- [ ] Debounce/rate-limit suggestion requests per conversation
- [ ] Implement optional overlay bubble with draw-over-apps permission
- [ ] Add test hooks and record demo flow