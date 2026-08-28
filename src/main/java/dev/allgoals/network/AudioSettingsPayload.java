package dev.allgoals.network;

import dev.allgoals.AllGoals;
import dev.allgoals.progress.PlayerAudioSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AudioSettingsPayload(PlayerAudioSettings settings) implements CustomPacketPayload {
    public static final Type<AudioSettingsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "audio_settings")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AudioSettingsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeFloat(payload.settings.completionVolume());
                buffer.writeBoolean(payload.settings.uniqueCrafts());
                buffer.writeBoolean(payload.settings.spyglass());
                buffer.writeBoolean(payload.settings.victory());
            },
            buffer -> new AudioSettingsPayload(new PlayerAudioSettings(
                    buffer.readFloat(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()
            ))
    );

    @Override
    public Type<AudioSettingsPayload> type() {
        return TYPE;
    }
}
