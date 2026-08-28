package dev.allgoals.client;

import dev.allgoals.network.LeaderboardDataPayload;
import dev.allgoals.network.LeaderboardRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class LeaderboardClient {
    private static boolean openRequested;
    private static Screen requestedParent;

    private LeaderboardClient() {
    }

    static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(LeaderboardDataPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            if (openRequested) {
                openRequested = false;
                Screen parent = requestedParent;
                requestedParent = null;
                client.setScreen(new LeaderboardScreen(parent, payload));
            } else if (client.screen instanceof LeaderboardScreen leaderboard) {
                leaderboard.update(payload);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
            openRequested = false;
            requestedParent = null;
        });
    }

    static void open(Screen parent) {
        if (!ClientPlayNetworking.canSend(LeaderboardRequestPayload.TYPE)) return;
        openRequested = true;
        requestedParent = parent;
        ClientPlayNetworking.send(LeaderboardRequestPayload.INSTANCE);
    }

    static void refresh() {
        if (ClientPlayNetworking.canSend(LeaderboardRequestPayload.TYPE)) {
            ClientPlayNetworking.send(LeaderboardRequestPayload.INSTANCE);
        }
    }
}
