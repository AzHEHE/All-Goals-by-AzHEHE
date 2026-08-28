package dev.allgoals.network;

import dev.allgoals.AllGoals;
import dev.allgoals.party.PartyManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LeaderboardNetworking {
    private static final long REQUEST_COOLDOWN_NANOS = 250_000_000L;
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();

    private LeaderboardNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                LeaderboardRequestPayload.TYPE, LeaderboardRequestPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                LeaderboardDataPayload.TYPE, LeaderboardDataPayload.STREAM_CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(LeaderboardRequestPayload.TYPE,
                (payload, context) -> sendRequestedSnapshot(context.player()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_REQUEST.remove(handler.getPlayer().getUUID()));
    }

    public static boolean sendSnapshot(ServerPlayer player) {
        return buildAndSend(player);
    }

    private static void sendRequestedSnapshot(ServerPlayer player) {
        long now = System.nanoTime();
        Long previous = LAST_REQUEST.get(player.getUUID());
        if (previous != null && now - previous < REQUEST_COOLDOWN_NANOS) return;
        LAST_REQUEST.put(player.getUUID(), now);
        buildAndSend(player);
    }

    private static boolean buildAndSend(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, LeaderboardDataPayload.TYPE)) return false;
        var entries = PartyManager.leaderboard(player.level().getServer()).stream()
                .map(entry -> new LeaderboardDataPayload.Entry(
                        entry.headOwner(), entry.name(), entry.party(), entry.completed(),
                        entry.onlineMembers(), entry.totalMembers()
                ))
                .toList();
        ServerPlayNetworking.send(player, new LeaderboardDataPayload(
                entries,
                AllGoals.goalCatalog().goalCount()
        ));
        return true;
    }
}
