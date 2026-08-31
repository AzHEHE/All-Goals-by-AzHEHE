package dev.allgoals.network;

import dev.allgoals.AllGoals;
import dev.allgoals.progress.AllGoalsAttachments;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;

public final class SettingsNetworking {
    private SettingsNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                AudioSettingsPayload.TYPE, AudioSettingsPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                AnnouncementSettingsPayload.TYPE, AnnouncementSettingsPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VersionPayload.TYPE, VersionPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VersionPayload.TYPE, VersionPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(AudioSettingsPayload.TYPE, (payload, context) ->
                context.player().setAttached(AllGoalsAttachments.AUDIO_SETTINGS, payload.settings()));
        ServerPlayNetworking.registerGlobalReceiver(VersionPayload.TYPE, (payload, context) -> {
            if (!ModVersionPolicy.matches(AllGoals.modVersion(), payload.version())) {
                context.player().connection.disconnect(mismatch(payload.version()));
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler.getPlayer(), VersionPayload.TYPE)) {
                handler.disconnect(Component.literal(
                        "All Goals " + AllGoals.RELEASE_VERSION + " (" + AllGoals.modVersion()
                                + ") is required on both the client and server."));
                return;
            }
            ServerPlayNetworking.send(handler.getPlayer(), new VersionPayload(AllGoals.modVersion()));
        });
    }

    private static Component mismatch(String clientVersion) {
        return Component.literal("All Goals version mismatch. Server: " + AllGoals.modVersion()
                + ", client: " + clientVersion + ". Install the same version on both sides.");
    }
}
