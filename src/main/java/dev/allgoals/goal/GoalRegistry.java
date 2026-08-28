package dev.allgoals.goal;

import dev.allgoals.AllGoals;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GoalRegistry {
    private static final Map<Identifier, Goal> GOALS = new LinkedHashMap<>();

    public static final Goal EAT_PUMPKIN_PIE = register(
            new SimpleGoal(
                    Identifier.fromNamespaceAndPath(
                            AllGoals.MOD_ID,
                            "eat_pumpkin_pie"
                    ),
                    "Eat Pumpkin Pie"
            )
    );

    private GoalRegistry() {
    }

    private static Goal register(Goal goal) {
        Goal existing = GOALS.putIfAbsent(goal.id(), goal);

        if (existing != null) {
            throw new IllegalArgumentException(
                    "Duplicate goal ID: " + goal.id()
            );
        }

        return goal;
    }

    public static List<Goal> values() {
        return List.copyOf(GOALS.values());
    }

    public static void initialize() {
        AllGoals.LOGGER.info(
                "Registered {} tracking goal(s)",
                GOALS.size()
        );
    }
}