package dev.allgoals.progress;

import net.minecraft.server.level.ServerPlayer;

public final class AudioSettingsManager {
    private AudioSettingsManager() {
    }

    public static PlayerAudioSettings get(ServerPlayer player) {
        return player.getAttachedOrElse(AllGoalsAttachments.AUDIO_SETTINGS, PlayerAudioSettings.defaults());
    }
}
