package dev.allgoals.client;

import dev.allgoals.AllGoals;
import dev.allgoals.network.AnnouncementSettingsPayload;
import dev.allgoals.network.AudioSettingsPayload;
import dev.allgoals.network.VersionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

final class ClientSettingsNetworking {
    private static OverlayConfig config;

    private ClientSettingsNetworking() {
    }

    static void initialize(OverlayConfig loadedConfig) {
        config = loadedConfig;
        ClientPlayNetworking.registerGlobalReceiver(VersionPayload.TYPE, (payload, context) -> {
            if (!AllGoals.modVersion().equals(payload.version())) {
                context.player().connection.getConnection().disconnect(Component.literal(
                        "All Goals version mismatch. Client: " + AllGoals.modVersion()
                                + ", server: " + payload.version()
                                + ". Install the same version on both sides."
                ));
            }
        });
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
            if (!ClientPlayNetworking.canSend(VersionPayload.TYPE)) {
                listener.getConnection().disconnect(Component.literal(
                        "This server does not have All Goals " + AllGoals.RELEASE_VERSION
                                + " (" + AllGoals.modVersion() + ")."
                ));
                return;
            }
            ClientPlayNetworking.send(new VersionPayload(AllGoals.modVersion()));
            syncAudio();
            syncAnnouncements();
        });
    }

    static void syncAudio() {
        if (config != null && ClientPlayNetworking.canSend(AudioSettingsPayload.TYPE)) {
            ClientPlayNetworking.send(new AudioSettingsPayload(config.audioSettings()));
        }
    }

    static void syncAnnouncements() {
        if (config != null && ClientPlayNetworking.canSend(AnnouncementSettingsPayload.TYPE)) {
            ClientPlayNetworking.send(new AnnouncementSettingsPayload(config.announcementsEnabled));
        }
    }
}
