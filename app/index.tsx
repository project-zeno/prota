import { useState, useEffect } from "react";
import {
  Text,
  View,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  Platform,
  StatusBar,
  NativeModules,
} from "react-native";

// Import the native bridge module
const { AccessibilityBridge } = NativeModules;

export default function Index() {
  const [isAccessibilityEnabled, setIsAccessibilityEnabled] = useState(false);
  const [isNotificationEnabled, setIsNotificationEnabled] = useState(false);

  useEffect(() => {
    checkPermissions();
  }, []);

  const checkPermissions = async () => {
    try {
      // Check accessibility service status
      const accessibilityEnabled = await AccessibilityBridge.isServiceEnabled();
      setIsAccessibilityEnabled(accessibilityEnabled);

      // Check notification permission
      const notificationEnabled = await AccessibilityBridge.checkNotificationPermission();
      setIsNotificationEnabled(notificationEnabled);

      console.log("Permissions checked:", {
        accessibility: accessibilityEnabled,
        notification: notificationEnabled,
      });
    } catch (error) {
      console.error("Error checking permissions:", error);
    }
  };

  const openAccessibilitySettings = async () => {
    try {
      await AccessibilityBridge.openAccessibilitySettings();
      console.log("Opened accessibility settings");
      
      // Recheck after user returns (they might have enabled it)
      setTimeout(() => {
        checkPermissions();
      }, 2000);
    } catch (error) {
      console.error("Error opening settings:", error);
      Alert.alert("Error", "Failed to open accessibility settings");
    }
  };

  const requestNotificationPermission = async () => {
    try {
      await AccessibilityBridge.openNotificationSettings();
      console.log("Opened notification settings");
      
      // Recheck after user returns
      setTimeout(() => {
        checkPermissions();
      }, 2000);
    } catch (error) {
      console.error("Error opening notification settings:", error);
      Alert.alert("Error", "Failed to open notification settings");
    }
  };


  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" />
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.title}>Chat Assist</Text>
          <Text style={styles.subtitle}>AI-powered chat suggestions</Text>
        </View>

        {/* Status Cards */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Status</Text>

          <View style={styles.statusCard}>
            <View style={styles.statusRow}>
              <Text style={styles.statusLabel}>Accessibility Service</Text>
              <View
                style={[
                  styles.statusBadge,
                  isAccessibilityEnabled ? styles.statusActive : styles.statusInactive,
                ]}
              >
                <Text style={styles.statusBadgeText}>
                  {isAccessibilityEnabled ? "Enabled" : "Disabled"}
                </Text>
              </View>
            </View>

            <View style={styles.statusRow}>
              <Text style={styles.statusLabel}>Notifications</Text>
              <View
                style={[
                  styles.statusBadge,
                  isNotificationEnabled ? styles.statusActive : styles.statusInactive,
                ]}
              >
                <Text style={styles.statusBadgeText}>
                  {isNotificationEnabled ? "Enabled" : "Disabled"}
                </Text>
              </View>
            </View>
          </View>
        </View>

        {/* Permissions */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Permissions</Text>
          <View style={styles.card}>
            <TouchableOpacity
              style={styles.secondaryButton}
              onPress={openAccessibilitySettings}
            >
              <Text style={styles.secondaryButtonText}>
                Open Accessibility Settings
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.secondaryButton}
              onPress={requestNotificationPermission}
            >
              <Text style={styles.secondaryButtonText}>
                Enable Notifications
              </Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* How to Use */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>How to Use</Text>
          <View style={styles.card}>
            <Text style={styles.instructionText}>
              1️⃣ Enable accessibility service above
            </Text>
            <Text style={styles.instructionText}>
              2️⃣ Open WhatsApp and read a chat
            </Text>
            <Text style={styles.instructionText}>
              3️⃣ Swipe down notification shade
            </Text>
            <Text style={styles.instructionText}>
              4️⃣ Tap &ldquo;📊 Analyze Chat Now&rdquo; button
            </Text>
            <Text style={styles.instructionText}>
              5️⃣ Get 3 AI suggestions in notification
            </Text>
            <Text style={styles.instructionText}>
              6️⃣ Tap a &ldquo;Copy&rdquo; button and paste in chat!
            </Text>
          </View>
        </View>

        {/* Info */}
        <View style={styles.section}>
          <View style={styles.infoCard}>
            <Text style={styles.infoText}>
              💡 When accessibility is enabled, you&apos;ll see a persistent
              notification with an &ldquo;Analyze Chat Now&rdquo; button. Use it while in
              WhatsApp to get instant AI suggestions!
            </Text>
            <Text style={styles.infoText}>
              🚀 Demo app for AI Native OS project. Uses Gemini API for fast
              suggestions.
            </Text>
            <Text style={styles.infoText}>
              ⚠️ No background monitoring - analysis only happens when you tap
              the button. Your privacy is protected.
            </Text>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#0a0a0a",
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: 20,
    paddingTop: Platform.OS === "android" ? StatusBar.currentHeight || 40 : 60,
  },
  header: {
    marginBottom: 32,
  },
  title: {
    fontSize: 36,
    fontWeight: "bold",
    color: "#fff",
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: "#888",
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: "600",
    color: "#fff",
    marginBottom: 12,
  },
  card: {
    backgroundColor: "#1a1a1a",
    borderRadius: 16,
    padding: 20,
    borderWidth: 1,
    borderColor: "#2a2a2a",
  },
  statusCard: {
    backgroundColor: "#1a1a1a",
    borderRadius: 16,
    padding: 20,
    borderWidth: 1,
    borderColor: "#2a2a2a",
    gap: 16,
  },
  statusRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  statusLabel: {
    fontSize: 15,
    color: "#ccc",
  },
  statusBadge: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
  },
  statusActive: {
    backgroundColor: "#10b98122",
    borderWidth: 1,
    borderColor: "#10b981",
  },
  statusInactive: {
    backgroundColor: "#ef444422",
    borderWidth: 1,
    borderColor: "#ef4444",
  },
  statusBadgeText: {
    fontSize: 13,
    fontWeight: "600",
    color: "#fff",
  },
  primaryButton: {
    backgroundColor: "#6366f1",
    borderRadius: 12,
    padding: 16,
    alignItems: "center",
    marginBottom: 12,
  },
  analyzeButton: {
    backgroundColor: "#8b5cf6",
    padding: 20,
    marginBottom: 16,
  },
  analyzeButtonText: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "700",
  },
  helperText: {
    fontSize: 13,
    color: "#888",
    lineHeight: 18,
    textAlign: "center",
  },
  startButton: {
    backgroundColor: "#10b981",
  },
  stopButton: {
    backgroundColor: "#ef4444",
  },
  primaryButtonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
  },
  secondaryButton: {
    backgroundColor: "#1a1a1a",
    borderWidth: 1,
    borderColor: "#6366f1",
    borderRadius: 12,
    padding: 16,
    alignItems: "center",
    marginBottom: 12,
  },
  secondaryButtonText: {
    color: "#6366f1",
    fontSize: 16,
    fontWeight: "600",
  },
  infoCard: {
    backgroundColor: "#1a1a1a",
    borderRadius: 16,
    padding: 20,
    borderWidth: 1,
    borderColor: "#2a2a2a",
    gap: 12,
  },
  infoText: {
    fontSize: 14,
    color: "#888",
    lineHeight: 20,
  },
  instructionText: {
    fontSize: 15,
    color: "#ccc",
    marginBottom: 10,
    lineHeight: 22,
  },
});
