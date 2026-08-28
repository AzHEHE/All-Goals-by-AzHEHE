package dev.allgoals;

import dev.allgoals.command.AllGoalsCommands;
import dev.allgoals.goal.GoalCatalog;
import dev.allgoals.network.LeaderboardNetworking;
import dev.allgoals.network.SettingsNetworking;
import dev.allgoals.notification.NotificationManager;
import dev.allgoals.party.PartyManager;
import dev.allgoals.progress.AllGoalsAttachments;
import dev.allgoals.tracking.AutomaticGoalTracker;
import dev.allgoals.tracking.DeathGoalTracker;
import dev.allgoals.tracking.InteractionGoalTracker;
import dev.allgoals.world.RunModeManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AllGoals implements ModInitializer {
    public static final String MOD_ID = "all_goals";
    public static final String RELEASE_VERSION = "V1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static GoalCatalog goalCatalog;

    @Override
    public void onInitialize() {
        goalCatalog = GoalCatalog.loadBundled();
        AllGoalsAttachments.initialize();
        LeaderboardNetworking.initialize();
        SettingsNetworking.initialize();
        NotificationManager.initialize();
        RunModeManager.initialize();
        PartyManager.initialize();
        AutomaticGoalTracker.initialize();
        DeathGoalTracker.initialize();
        InteractionGoalTracker.initialize();
        AllGoalsCommands.initialize();
        LOGGER.info(
                "All Goals initialized with {} goals, {} icons, and {} rotating icon groups",
                goalCatalog.goalCount(),
                goalCatalog.iconCount(),
                goalCatalog.rotatingGoalCount()
        );
    }

    public static GoalCatalog goalCatalog() {
        if (goalCatalog == null) {
            throw new IllegalStateException("All Goals has not initialized yet");
        }
        return goalCatalog;
    }

    public static String modVersion() {
        return VersionHolder.VERSION;
    }

    private static final class VersionHolder {
        private static final String VERSION = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .orElseThrow(() -> new IllegalStateException("All Goals mod metadata is unavailable"))
                .getMetadata()
                .getVersion()
                .getFriendlyString();

        private VersionHolder() {
        }
    }
}
