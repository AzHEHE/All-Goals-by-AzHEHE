package dev.allgoals.tracking;

import dev.allgoals.progress.PlayerGoalProgress;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class CraftingProgress {
    private static final String CRAFTED_ITEMS = "crafted_items";
    private static final String LEGACY_PREFIX = CRAFTED_ITEMS + ":";
    private static final String MIGRATION = "crafting_results_v3_stable";

    private CraftingProgress() {
    }

    static Result record(PlayerGoalProgress.Editor progress, String itemId) {
        migrate(progress);
        int before = progress.observationCount(CRAFTED_ITEMS);
        int total = progress.observe(CRAFTED_ITEMS, itemId);
        updateMilestones(progress, total);
        return new Result(total, total > before);
    }

    static void migrate(PlayerGoalProgress.Editor progress) {
        if (progress.observations("migrations").contains(MIGRATION)) return;

        List<String> legacyKeys = progress.observationKeys().stream()
                .filter(key -> key.startsWith(LEGACY_PREFIX))
                .toList();
        Set<String> combined = new LinkedHashSet<>(progress.observations(CRAFTED_ITEMS));
        for (String key : legacyKeys) combined.addAll(progress.observations(key));
        for (String key : legacyKeys) progress.clearObservations(key);
        combined.forEach(itemId -> progress.observe(CRAFTED_ITEMS, itemId));

        updateMilestones(progress, combined.size());
        progress.observe("migrations", MIGRATION);
    }

    private static void updateMilestones(PlayerGoalProgress.Editor progress, int total) {
        progress.setCounterAtLeast("unique_crafts_max", total);
        if (total >= 20) progress.complete("CRAFT_20_UNIQUE_ITEMS");
        if (total >= 50) progress.complete("CRAFT_50_UNIQUE_ITEMS");
        if (total >= 100) progress.complete("CRAFT_100_UNIQUE_ITEMS");
    }

    record Result(int total, boolean added) {
    }
}
