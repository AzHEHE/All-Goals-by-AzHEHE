package dev.allgoals.tracking;

import dev.allgoals.progress.PlayerGoalProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IceGoalProgress {
    public static final String OBSERVATION_KEY = "mined_ice_items";
    public static final List<String> VALID_TYPES = List.of(
            "ice", "packed_ice", "blue_ice", "frosted_ice"
    );
    private static final int TARGET = 3;

    private IceGoalProgress() {
    }

    public static void record(PlayerGoalProgress.Editor progress, Block block) {
        String blockId = BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (!VALID_TYPES.contains(blockId)) return;

        Set<String> recorded = canonicalTypes(progress.observations(OBSERVATION_KEY));
        recorded.add(blockId);
        replaceIfNeeded(progress, recorded);
        if (recorded.size() >= TARGET) progress.complete("MINE_3_TYPES_OF_ICE");
    }

    public static Set<String> canonicalTypes(Iterable<String> values) {
        Set<String> canonical = new LinkedHashSet<>();
        for (String value : values) {
            int separator = value.indexOf(':');
            if (separator >= 0 && !value.substring(0, separator).equals("minecraft")) continue;
            String path = value.substring(separator + 1);
            if (VALID_TYPES.contains(path)) canonical.add(path);
        }
        return canonical;
    }

    private static void replaceIfNeeded(PlayerGoalProgress.Editor progress, Set<String> recorded) {
        if (progress.observations(OBSERVATION_KEY).equals(recorded)) return;
        progress.clearObservations(OBSERVATION_KEY);
        recorded.forEach(type -> progress.observe(OBSERVATION_KEY, type));
    }
}
