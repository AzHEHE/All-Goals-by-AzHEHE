package dev.allgoals.progress;

import dev.allgoals.AllGoals;
import dev.allgoals.notification.NotificationPreference;
import dev.allgoals.party.PartyStatus;
import dev.allgoals.world.RunMode;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

public final class AllGoalsAttachments {
    public static final AttachmentType<PlayerGoalProgress> PLAYER_PROGRESS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "player_progress"),
            builder -> builder
                    .initializer(PlayerGoalProgress::empty)
                    .persistent(PlayerGoalProgress.CODEC)
                    .copyOnDeath()
                    .syncWith(
                            ByteBufCodecs.fromCodecWithRegistries(PlayerGoalProgress.CODEC),
                            AttachmentSyncPredicate.targetOnly()
                    )
    );

    public static final AttachmentType<PartyStatus> PARTY_STATUS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "party_status"),
            builder -> builder
                    .initializer(PartyStatus::solo)
                    .syncWith(
                            ByteBufCodecs.fromCodecWithRegistries(PartyStatus.CODEC),
                            AttachmentSyncPredicate.targetOnly()
                    )
    );

    public static final AttachmentType<NotificationPreference> NOTIFICATION_PREFERENCE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "notification_preference"),
                    builder -> builder
                            .initializer(() -> NotificationPreference.ON)
                            .persistent(NotificationPreference.CODEC)
                            .copyOnDeath()
            );

    public static final AttachmentType<PlayerAudioSettings> AUDIO_SETTINGS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "audio_settings"),
            builder -> builder
                    .initializer(PlayerAudioSettings::defaults)
                    .persistent(PlayerAudioSettings.CODEC)
                    .copyOnDeath()
    );

    public static final AttachmentType<RunMode> RUN_MODE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "run_mode"),
            builder -> builder
                    .initializer(() -> RunMode.ALL_GOALS)
                    .syncWith(
                            ByteBufCodecs.fromCodecWithRegistries(RunMode.CODEC),
                            AttachmentSyncPredicate.targetOnly()
                    )
    );

    public static final AttachmentType<String> RUN_ID = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, "run_id"),
            builder -> builder
                    .initializer(() -> "")
                    .syncWith(
                            ByteBufCodecs.STRING_UTF8,
                            AttachmentSyncPredicate.targetOnly()
                    )
    );

    private AllGoalsAttachments() {
    }

    public static void initialize() {
        // Loading this class performs registration before players can join.
    }
}
