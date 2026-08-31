package dev.allgoals.tracking;

import dev.allgoals.progress.PlayerGoalProgress;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CraftingProgressSelfTest {
    private CraftingProgressSelfTest() {
    }

    public static void main(String[] args) {
        PlayerGoalProgress original = new PlayerGoalProgress(
                Set.of("CRAFT_20_UNIQUE_ITEMS"),
                Map.of("unique_crafts_max", 20),
                Map.of(
                        "crafted_items", List.of("torch"),
                        "crafted_items:first-player", List.of("stick", "planks"),
                        "crafted_items:second-player", List.of("planks", "crafting_table")
                )
        );

        PlayerGoalProgress.Editor migration = original.edit();
        CraftingProgress.migrate(migration);
        PlayerGoalProgress migrated = migration.build();
        expect(migrated.observations("crafted_items").size() == 4,
                "all legacy craft histories must be merged without duplicates");
        expect(migrated.observations().keySet().stream()
                        .noneMatch(key -> key.startsWith("crafted_items:")),
                "runtime UUID craft keys must be removed after migration");
        expect(migrated.isComplete("CRAFT_20_UNIQUE_ITEMS"),
                "migration must never revoke completed craft goals");
        expect(migrated.counter("unique_crafts_max") == 20,
                "migration must preserve the highest recorded craft count");

        PlayerGoalProgress.Editor duplicate = migrated.edit();
        CraftingProgress.Result duplicateResult = CraftingProgress.record(duplicate, "stick");
        expect(!duplicateResult.added() && duplicateResult.total() == 4,
                "a craft already recorded before restart must not count again");

        PlayerGoalProgress.Editor newCraft = migrated.edit();
        CraftingProgress.Result newResult = CraftingProgress.record(newCraft, "chest");
        expect(newResult.added() && newResult.total() == 5,
                "a new craft must continue from the migrated total");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
