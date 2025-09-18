import React, { useEffect, useState } from "react";
import { View, Text, Button, StyleSheet, NativeModules } from "react-native";

const { WhatsappBridge } = NativeModules;

export default function App() {
    const [accessibilityEnabled, setAccessibilityEnabled] = useState(false);

    useEffect(() => {
        // Aqui você poderia criar métodos nativos que verificam se o serviço está ativo
    }, []);

    return (
        <View style={styles.container}>
            <Text style={styles.title}>CallBlocker</Text>
            <Text>Status da acessibilidade: {accessibilityEnabled ? "Ativo" : "Desativado"}</Text>
            <Button title="Abrir Configurações de Acessibilidade" onPress={() => WhatsappBridge.openAccessibilitySettings()} />
            <Button title="Abrir Configurações de Notificações" onPress={() => WhatsappBridge.openNotificationAccessSettings()} />
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, justifyContent: "center", alignItems: "center" },
    title: { fontSize: 24, marginBottom: 20 }
});