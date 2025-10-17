import { Stack } from "expo-router";
import { StatusBar } from "react-native";

export default function RootLayout() {
  return (
    <>
      <StatusBar barStyle="light-content" backgroundColor="#0a0a0a" />
      <Stack
        screenOptions={{
          headerStyle: {
            backgroundColor: "#0a0a0a",
          },
          headerTintColor: "#fff",
          headerShadowVisible: false,
          contentStyle: {
            backgroundColor: "#0a0a0a",
          },
        }}
      >
        <Stack.Screen
          name="index"
          options={{
            headerShown: false,
          }}
        />
      </Stack>
    </>
  );
}
