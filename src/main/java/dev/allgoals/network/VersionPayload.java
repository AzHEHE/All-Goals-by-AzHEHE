package dev.allgoals.network;

import dev.allgoals.AllGoals;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VersionPayload(String version) implements CustomPacketPayload {
    public static final Type<VersionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "version")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, VersionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.version, 64),
            buffer -> new VersionPayload(buffer.readUtf(64))
    );

    @Override
    public Type<VersionPayload> type() {
        return TYPE;
    }
}
