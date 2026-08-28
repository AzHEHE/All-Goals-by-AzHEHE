package dev.allgoals.goal;

import net.minecraft.resources.Identifier;

public record SimpleGoal(
        Identifier id,
        String displayName) implements Goal {
}
