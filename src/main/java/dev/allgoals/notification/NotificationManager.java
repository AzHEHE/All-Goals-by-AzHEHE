package dev.allgoals.notification;

import dev.allgoals.network.AnnouncementSettingsPayload;
import dev.allgoals.progress.AllGoalsAttachments;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class NotificationManager {
    private NotificationManager() {
    }

    public static void initialize() {
        ServerPlayNetworking.registerGlobalReceiver(AnnouncementSettingsPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            player.setAttached(AllGoalsAttachments.NOTIFICATION_PREFERENCE,
                    payload.enabled() ? NotificationPreference.ON : NotificationPreference.OFF);
        });
    }

    public static boolean shouldReceive(ServerPlayer player) {
        return preference(player) != NotificationPreference.OFF;
    }

    public static NotificationPreference preference(ServerPlayer player) {
        NotificationPreference preference = player.getAttachedOrElse(
                AllGoalsAttachments.NOTIFICATION_PREFERENCE, NotificationPreference.ON
        );
        return preference == NotificationPreference.DEFAULT ? NotificationPreference.ON : preference;
    }
}
