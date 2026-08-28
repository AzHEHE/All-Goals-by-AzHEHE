package dev.allgoals.network;

import dev.allgoals.AllGoals;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AnnouncementSettingsPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<AnnouncementSettingsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "announcement_settings")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AnnouncementSettingsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeBoolean(payload.enabled),
                    buffer -> new AnnouncementSettingsPayload(buffer.readBoolean())
            );

    @Override
    public Type<AnnouncementSettingsPayload> type() {
        return TYPE;
    }
}
