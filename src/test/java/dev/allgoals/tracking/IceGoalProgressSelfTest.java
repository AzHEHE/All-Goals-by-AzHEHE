package dev.allgoals.tracking;

import java.util.List;
import java.util.Set;

public final class IceGoalProgressSelfTest {
    private IceGoalProgressSelfTest() {
    }

    public static void main(String[] args) {
        Set<String> recorded = IceGoalProgress.canonicalTypes(List.of(
                "minecraft:ice",
                "ice",
                "minecraft:packed_ice",
                "blue_ice",
                "minecraft:air",
                "example:ice"
        ));

        expect(recorded.equals(Set.of("ice", "packed_ice", "blue_ice")),
                "ice progress must normalize legacy IDs without double-counting them");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
