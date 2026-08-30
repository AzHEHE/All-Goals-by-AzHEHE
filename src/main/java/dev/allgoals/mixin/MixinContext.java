package dev.allgoals.mixin;

import net.minecraft.server.level.ServerPlayer;

final class MixinContext {
    static final ThreadLocal<ServerPlayer> BLOCK_PLACING_PLAYER = new ThreadLocal<>();
    static final ThreadLocal<ServerPlayer> PUMPKIN_CARVING_PLAYER = new ThreadLocal<>();

    private MixinContext() {
    }
}
