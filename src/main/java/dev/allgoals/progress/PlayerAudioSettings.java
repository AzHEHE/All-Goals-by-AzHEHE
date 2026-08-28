package dev.allgoals.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PlayerAudioSettings(
        float completionVolume,
        boolean uniqueCrafts,
        boolean spyglass,
        boolean victory
) {
    public static final Codec<PlayerAudioSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("completion_volume", 1.0F).forGetter(PlayerAudioSettings::completionVolume),
            Codec.BOOL.optionalFieldOf("unique_crafts", true).forGetter(PlayerAudioSettings::uniqueCrafts),
            Codec.BOOL.optionalFieldOf("spyglass", true).forGetter(PlayerAudioSettings::spyglass),
            Codec.BOOL.optionalFieldOf("victory", true).forGetter(PlayerAudioSettings::victory)
    ).apply(instance, PlayerAudioSettings::new));

    public PlayerAudioSettings {
        completionVolume = Float.isFinite(completionVolume)
                ? Math.clamp(completionVolume, 0.0F, 1.0F)
                : 1.0F;
    }

    public static PlayerAudioSettings defaults() {
        return new PlayerAudioSettings(1.0F, true, true, true);
    }
}
