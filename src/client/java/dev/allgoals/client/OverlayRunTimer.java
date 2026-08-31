package dev.allgoals.client;

import dev.allgoals.AllGoals;
import dev.allgoals.party.PartyStatus;
import dev.allgoals.progress.AllGoalsAttachments;
import dev.allgoals.progress.PlayerGoalProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

final class OverlayRunTimer {
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final int SAVE_INTERVAL_TICKS = 20 * 20;

    private final OverlayConfig config;
    private String runKey;
    private long elapsedNanos;
    private long lastTickNanos;
    private boolean wasRunning;
    private int ticksSinceSave;
    private boolean partyMode;
    private PartyStatus partyStatus = PartyStatus.solo();
    private long partyStatusReceivedNanos;
    private ClientLevel runIdentity;
    private String legacyRunKey;
    private boolean legacyMigrationChecked;
    private PlayerGoalProgress completionCheckedProgress;
    private boolean allGoalsComplete;

    OverlayRunTimer(OverlayConfig config) {
        this.config = config;
    }

    void tick(Minecraft client) {
        long now = System.nanoTime();
        ClientLevel nextRunIdentity = client.player == null ? null : client.level;
        String nextRunKey = runKey(client);
        if (runIdentity != nextRunIdentity) {
            runIdentity = nextRunIdentity;
            legacyRunKey = legacyRunKey(client);
            switchRun(nextRunKey);
        } else if (!Objects.equals(runKey, nextRunKey)) {
            switchRun(nextRunKey);
        }
        migrateLegacyTimer(client);

        PartyStatus nextPartyStatus = partyStatus(client);
        if (nextPartyStatus.inParty()) {
            if (!partyMode && wasRunning) persist();
            partyMode = true;
            wasRunning = false;
            lastTickNanos = now;
            if (!nextPartyStatus.equals(partyStatus)) {
                partyStatus = nextPartyStatus;
                partyStatusReceivedNanos = now;
            }
            return;
        }

        if (partyMode) {
            elapsedNanos = Math.max(elapsedNanos, partyElapsedMillis(now) * NANOS_PER_MILLI);
            partyMode = false;
            partyStatus = PartyStatus.solo();
            partyStatusReceivedNanos = 0L;
            persist();
        }

        boolean running = runKey != null
                && client.player != null
                && client.level != null
                && (client.screen == null || !client.screen.isPauseScreen())
                && !allGoalsComplete(client);

        if (running && wasRunning && lastTickNanos != 0L) {
            elapsedNanos += Math.max(0L, now - lastTickNanos);
        }
        lastTickNanos = now;

        if (wasRunning && !running) persist();
        wasRunning = running;

        if (running && ++ticksSinceSave >= SAVE_INTERVAL_TICKS) persist();
    }

    long elapsedMillis() {
        if (partyMode) return partyElapsedMillis(System.nanoTime());
        return elapsedNanos / NANOS_PER_MILLI;
    }

    void stop() {
        persist();
    }

    private void switchRun(String nextRunKey) {
        persist();
        runKey = nextRunKey;
        elapsedNanos = runKey == null ? 0L : config.timerFor(runKey) * NANOS_PER_MILLI;
        lastTickNanos = 0L;
        wasRunning = false;
        completionCheckedProgress = null;
        allGoalsComplete = false;
        legacyMigrationChecked = false;
    }

    private void migrateLegacyTimer(Minecraft client) {
        if (legacyMigrationChecked || runKey == null) return;
        legacyMigrationChecked = true;
        if (legacyRunKey == null || config.hasTimer(runKey) || !hasRecordedProgress(client)) return;

        long previousTimer = config.timerFor(legacyRunKey);
        if (previousTimer <= 0L) return;
        elapsedNanos = previousTimer * NANOS_PER_MILLI;
        persist();
    }

    private static boolean hasRecordedProgress(Minecraft client) {
        if (client.player == null) return false;
        PlayerGoalProgress progress = client.player.getAttachedOrElse(
                AllGoalsAttachments.PLAYER_PROGRESS, PlayerGoalProgress.empty()
        );
        return !progress.completed().isEmpty()
                || !progress.counters().isEmpty()
                || !progress.observations().isEmpty();
    }

    private void persist() {
        ticksSinceSave = 0;
        if (runKey == null) return;
        config.setTimer(runKey, elapsedMillis());
        config.save();
    }

    private boolean allGoalsComplete(Minecraft client) {
        PlayerGoalProgress progress = client.player.getAttachedOrElse(
                AllGoalsAttachments.PLAYER_PROGRESS, PlayerGoalProgress.empty()
        );
        if (completionCheckedProgress != progress) {
            completionCheckedProgress = progress;
            allGoalsComplete = AllGoals.goalCatalog().allComplete(progress);
        }
        return allGoalsComplete;
    }

    private long partyElapsedMillis(long nowNanos) {
        long interpolated = partyStatus.elapsedMillis();
        if (partyStatus.timerRunning() && partyStatusReceivedNanos != 0L) {
            interpolated += Math.max(0L, nowNanos - partyStatusReceivedNanos) / NANOS_PER_MILLI;
        }
        return interpolated;
    }

    private static PartyStatus partyStatus(Minecraft client) {
        if (client.player == null) return PartyStatus.solo();
        return client.player.getAttachedOrElse(AllGoalsAttachments.PARTY_STATUS, PartyStatus.solo());
    }

    private static String runKey(Minecraft client) {
        if (client.player == null || client.level == null) return null;
        String worldRunId = client.player.getAttachedOrElse(AllGoalsAttachments.RUN_ID, "");
        return worldRunId.isBlank() ? null : "world:" + worldRunId;
    }

    private static String legacyRunKey(Minecraft client) {
        if (client.player == null || client.level == null) return null;
        if (client.getSingleplayerServer() != null) {
            Path root = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath().normalize();
            Path folder = root.getFileName();
            return "singleplayer:" + (folder == null ? root : folder);
        }
        if (client.getCurrentServer() != null) {
            return "server:" + client.getCurrentServer().ip.toLowerCase(Locale.ROOT);
        }
        return "local:" + client.level.dimension().identifier();
    }
}
