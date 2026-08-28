package dev.allgoals.tracking;

import dev.allgoals.AllGoals;
import dev.allgoals.party.PartyManager;
import dev.allgoals.notification.NotificationManager;
import dev.allgoals.progress.PlayerGoalProgress;
import dev.allgoals.progress.AudioSettingsManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import dev.allgoals.world.RunModeManager;

import java.util.Collection;
import java.util.function.Consumer;

public final class GoalProgressService {
    private static final String VICTORY_OBSERVATION = "all_goals_victory";
    private static final String VICTORY_VERSION = "v1";

    private GoalProgressService() {
    }

    public static PlayerGoalProgress activeProgress(ServerPlayer player) {
        return PartyManager.activeProgress(player);
    }

    public static void update(ServerPlayer player, Consumer<PlayerGoalProgress.Editor> change) {
        PlayerGoalProgress oldProgress = PartyManager.activeProgress(player);
        PlayerGoalProgress.Editor editor = oldProgress.edit();
        change.accept(editor);
        save(player, oldProgress, editor, true);
    }

    public static void complete(ServerPlayer player, Collection<String> goalIds) {
        if (!goalIds.isEmpty()) update(player, progress -> goalIds.forEach(progress::complete));
    }

    public static PlayerGoalProgress save(ServerPlayer player, PlayerGoalProgress oldProgress,
                                          PlayerGoalProgress.Editor editor) {
        return save(player, oldProgress, editor, true);
    }

    public static PlayerGoalProgress save(ServerPlayer player, PlayerGoalProgress oldProgress,
                                          PlayerGoalProgress.Editor editor, boolean announceGoals) {
        if (!RunModeManager.tracksGoals(player.level().getServer())) return oldProgress;
        boolean celebrate = shouldCelebrate(editor);
        if (celebrate) editor.observe(VICTORY_OBSERVATION, VICTORY_VERSION);

        PlayerGoalProgress updated = editor.build();
        if (updated == oldProgress) return oldProgress;

        PartyManager.saveActiveProgress(player, updated);
        if (announceGoals) announceCompletions(player, editor.newlyCompleted());
        if (celebrate) PartyManager.completionRecipients(player).forEach(VictoryCelebration::play);
        return updated;
    }

    private static boolean shouldCelebrate(PlayerGoalProgress.Editor editor) {
        if (editor.observations(VICTORY_OBSERVATION).contains(VICTORY_VERSION)) return false;
        return editor.completedCount() >= AllGoals.goalCatalog().goalCount()
                && AllGoals.goalCatalog().goals().stream()
                .allMatch(goal -> editor.isComplete(goal.sourceId()));
    }

    public static void announceCompletions(ServerPlayer player, Collection<String> goalIds) {
        for (String goalId : goalIds) {
            String name = AllGoals.goalCatalog().findSource(goalId)
                    .map(goal -> goal.displayName()).orElse(goalId);
            Component message = Component.literal(player.getScoreboardName() + " has completed " + name + ".")
                    .withStyle(ChatFormatting.GREEN);
            for (ServerPlayer recipient : player.level().getServer().getPlayerList().getPlayers()) {
                if (NotificationManager.shouldReceive(recipient)) recipient.sendSystemMessage(message);
                float volume = AudioSettingsManager.get(recipient).completionVolume();
                if (volume <= 0.0F) continue;
                recipient.connection.send(new ClientboundSoundPacket(
                        SoundEvents.NOTE_BLOCK_CHIME,
                        SoundSource.MASTER,
                        recipient.getX(),
                        recipient.getY(),
                        recipient.getZ(),
                        2.0F * volume,
                        1.0F,
                        recipient.getRandom().nextLong()
                ));
            }
        }
    }
}
