package dev.allgoals.tracking;

import net.minecraft.server.level.ServerPlayer;

/**
 * Carries the player responsible for nested vanilla actions that do not pass
 * that player into the final success method.
 */
public final class GoalActionContext {
    private static final ThreadLocal<ServerPlayer> BLOCK_PLACING_PLAYER = new ThreadLocal<>();
    private static final ThreadLocal<ServerPlayer> PUMPKIN_CARVING_PLAYER = new ThreadLocal<>();

    private GoalActionContext() {
    }

    public static void captureBlockPlacer(ServerPlayer player) {
        BLOCK_PLACING_PLAYER.set(player);
    }

    public static ServerPlayer blockPlacer() {
        return BLOCK_PLACING_PLAYER.get();
    }

    public static void clearBlockPlacer() {
        BLOCK_PLACING_PLAYER.remove();
    }

    public static void capturePumpkinCarver(ServerPlayer player) {
        PUMPKIN_CARVING_PLAYER.set(player);
    }

    public static ServerPlayer pumpkinCarver() {
        return PUMPKIN_CARVING_PLAYER.get();
    }

    public static void clearPumpkinCarver() {
        PUMPKIN_CARVING_PLAYER.remove();
    }
}
