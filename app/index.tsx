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
} from "react-native";

export default function Index() {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [isAccessibilityEnabled, setIsAccessibilityEnabled] = useState(false);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [isNotificationEnabled, setIsNotificationEnabled] = useState(false);
  const [isServiceRunning, setIsServiceRunning] = useState(false);

  useEffect(() => {
    checkPermissions();
  }, []);

  const checkPermissions = async () => {
    // TODO: Check accessibility and notification permissions via native bridge
    // Will update setIsAccessibilityEnabled and setIsNotificationEnabled
    console.log("Checking permissions...");
    // Temporary mock values for testing UI
    // setIsAccessibilityEnabled(false);
    // setIsNotificationEnabled(false);
  };

  const openAccessibilitySettings = () => {
    // TODO: Call native bridge to open accessibility settings
    Alert.alert(
      "Accessibility Settings",
      "This will open your device's Accessibility Settings. Enable 'Chat Assist' service."
    );
    console.log("Opening accessibility settings...");
  };

  const requestNotificationPermission = () => {
    // TODO: Call native bridge to request notification permission
    Alert.alert(
      "Notification Permission",
      "Notification permission is required to show chat suggestions."
    );
    console.log("Requesting notification permission...");
  };

  const testNotification = () => {
    // TODO: Call native bridge to test notification
    Alert.alert("Test", "Sending test notification...");
    console.log("Testing notification...");
  };

  const toggleService = () => {
    // TODO: Start/stop the accessibility service monitoring
    setIsServiceRunning(!isServiceRunning);
    console.log("Service toggled:", !isServiceRunning);
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

            <View style={styles.statusRow}>
              <Text style={styles.statusLabel}>Service Status</Text>
              <View
                style={[
                  styles.statusBadge,
                  isServiceRunning ? styles.statusActive : styles.statusInactive,
                ]}
              >
                <Text style={styles.statusBadgeText}>
                  {isServiceRunning ? "Running" : "Stopped"}
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

        {/* Service Control */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Service Control</Text>
          <View style={styles.card}>
            <TouchableOpacity
              style={[
                styles.primaryButton,
                isServiceRunning ? styles.stopButton : styles.startButton,
              ]}
              onPress={toggleService}
            >
              <Text style={styles.primaryButtonText}>
                {isServiceRunning ? "Stop Service" : "Start Service"}
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.secondaryButton}
              onPress={testNotification}
            >
              <Text style={styles.secondaryButtonText}>Test Notification</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Info */}
        <View style={styles.section}>
          <View style={styles.infoCard}>
            <Text style={styles.infoText}>
              💡 This app monitors WhatsApp conversations and provides AI-powered
              reply suggestions via notifications.
            </Text>
            <Text style={styles.infoText}>
              🚀 Demo app for AI Native OS project. AI suggestions are powered
              by integrated language models.
            </Text>
            <Text style={styles.infoText}>
              ⚠️ Requires accessibility permissions to read chat messages.
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
});
