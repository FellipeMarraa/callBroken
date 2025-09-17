import React, { useEffect, useState } from "react";
import { SafeAreaView, View, Text, Button, Alert, PermissionsAndroid, Platform } from "react-native";
import { NativeModules, NativeEventEmitter } from "react-native";

const { WhatsappBridge } = NativeModules;

const emitter = WhatsappBridge ? new NativeEventEmitter(WhatsappBridge) : null;

export default function App() {
    const [notifEnabled, setNotifEnabled] = useState<boolean | null>(null);
    const [accEnabled, setAccEnabled] = useState<boolean | null>(null);
    const [lastBlocked, setLastBlocked] = useState<string | null>(null);

    useEffect(() => {
        checkServices();
        const subs: any[] = [];
        if (emitter) {
            subs.push(emitter.addListener("WhatsappUnknownCall", (e: any) => {
                setLastBlocked(e?.caller ?? "Desconhecido");
            }));
        }
        return () => {
            subs.forEach(s => s.remove && s.remove());
        };
    }, []);

    async function checkServices() {
        try {
            if (WhatsappBridge && WhatsappBridge.isNotificationListenerEnabled) {
                const result = await WhatsappBridge.isNotificationListenerEnabled();
                setNotifEnabled(!!result);
            }
        } catch (e) {
            console.warn(e);
        }
        try {
            if (WhatsappBridge && WhatsappBridge.isAccessibilityServiceEnabled) {
                const result = await WhatsappBridge.isAccessibilityServiceEnabled();
                setAccEnabled(!!result);
            }
        } catch (e) {
            console.warn(e);
        }
    }

    async function requestContactsPermission() {
        if (Platform.OS !== "android") return;

        try {
            const granted = await PermissionsAndroid.request(
                PermissionsAndroid.PERMISSIONS.READ_CONTACTS,
                {
                    title: "Permiss�o para contatos",
                    message: "Precisamos acessar contatos para permitir chamadas de conhecidos.",
                    buttonPositive: "OK"
                }
            );
            if (granted === PermissionsAndroid.RESULTS.GRANTED) {
                Alert.alert("Permiss�o concedida", "Agora o app consegue verificar se quem liga est� nos contatos.");
            } else {
                Alert.alert("Permiss�o negada", "Sem essa permiss�o o app bloquear� chamadas de n�meros que n�o s�o contatos.");
            }
        } catch (err) {
            console.warn(err);
        }
    }

    function openNotificationSettings() {
        if (WhatsappBridge && WhatsappBridge.openNotificationAccessSettings) {
            WhatsappBridge.openNotificationAccessSettings();
        } else {
            Alert.alert("Erro", "M�dulo nativo n�o dispon�vel.");
        }
    }

    function openAccessibilitySettings() {
        if (WhatsappBridge && WhatsappBridge.openAccessibilitySettings) {
            WhatsappBridge.openAccessibilitySettings();
        } else {
            Alert.alert("Erro", "M�dulo nativo n�o dispon�vel.");
        }
    }

    return (
        <SafeAreaView style={{ flex: 1, padding: 16 }}>
            <Text style={{ fontSize: 18, fontWeight: "700", marginBottom: 8 }}>CallBlocker � Prote��o de chamadas WhatsApp</Text>

            <View style={{ marginVertical: 8 }}>
                <Text>Notifica��es ativadas: {notifEnabled === null ? "..." : notifEnabled ? "Sim" : "N�o"}</Text>
                <Button title="Abrir configura��es de Notifica��es" onPress={openNotificationSettings} />
            </View>

            <View style={{ marginVertical: 8 }}>
                <Text>Servi�o de Acessibilidade: {accEnabled === null ? "..." : accEnabled ? "Ativo" : "Inativo"}</Text>
                <Button title="Abrir Acessibilidade" onPress={openAccessibilitySettings} />
            </View>

            <View style={{ marginVertical: 8 }}>
                <Button title="Pedir permiss�o para Contatos" onPress={requestContactsPermission} />
            </View>

            <View style={{ marginVertical: 12 }}>
                <Text style={{ fontWeight: "600" }}>�ltima chamada bloqueada:</Text>
                <Text>{lastBlocked ?? "Nenhuma"}</Text>
            </View>

            <View style={{ marginTop: 20 }}>
                <Text style={{ color: "#666" }}>Observa��es:</Text>
                <Text>- Ative Notifica��es e Acessibilidade para o app manualmente nas telas que abrir.</Text>
                <Text>- Ap�s ativar, fa�a um teste pedindo para algu�m ligar via WhatsApp para ver se a chamada � recusada automaticamente.</Text>
            </View>
        </SafeAreaView>
    );
}