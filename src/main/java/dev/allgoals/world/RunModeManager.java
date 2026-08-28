package dev.allgoals.world;

import dev.allgoals.progress.AllGoalsAttachments;
import dev.allgoals.progress.PlayerGoalProgress;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class RunModeManager {
    private RunModeManager() {
    }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                syncPlayer(handler.getPlayer(), mode(server)));
    }

    public static RunMode mode(MinecraftServer server) {
        return RunModeSavedData.get(server).mode();
    }

    public static boolean tracksGoals(MinecraftServer server) {
        return mode(server) == RunMode.ALL_GOALS;
    }

    public static void setMode(MinecraftServer server, RunMode mode) {
        RunModeSavedData.get(server).setMode(mode);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) syncPlayer(player, mode);
    }

    private static void syncPlayer(ServerPlayer player, RunMode mode) {
        player.setAttached(AllGoalsAttachments.RUN_MODE, mode);
        if (mode == RunMode.NONE) {
            player.setAttached(AllGoalsAttachments.PLAYER_PROGRESS, PlayerGoalProgress.empty());
        }
    }
}
