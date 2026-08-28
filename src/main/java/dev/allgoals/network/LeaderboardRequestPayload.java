package dev.allgoals.network;

import dev.allgoals.AllGoals;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LeaderboardRequestPayload() implements CustomPacketPayload {
    public static final LeaderboardRequestPayload INSTANCE = new LeaderboardRequestPayload();
    public static final Type<LeaderboardRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "leaderboard_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<LeaderboardRequestPayload> type() {
        return TYPE;
    }
}
