package dev.allgoals.tracking;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

import java.util.Map;
import java.util.Set;

public final class CriteriaGoalTracker {
    private static final Map<String, String> BREED_GOALS = Map.ofEntries(
            Map.entry("armadillo", "BREED_ARMADILLO"), Map.entry("camel", "BREED_CAMEL"),
            Map.entry("chicken", "BREED_CHICKEN"), Map.entry("cow", "BREED_COW"),
            Map.entry("fox", "BREED_FOX"), Map.entry("frog", "BREED_FROGS"),
            Map.entry("goat", "BREED_GOAT"), Map.entry("hoglin", "BREED_HOGLIN"),
            Map.entry("pig", "BREED_PIG"), Map.entry("rabbit", "BREED_RABBIT"),
            Map.entry("sheep", "BREED_SHEEP"), Map.entry("strider", "BREED_STRIDER")
    );
    private static final Map<String, String> TAME_GOALS = Map.of(
            "cat", "TAME_CAT", "horse", "TAME_HORSE", "nautilus", "TAME_NAUTILUS",
            "parrot", "TAME_PARROT", "wolf", "TAME_WOLF"
    );

    private CriteriaGoalTracker() {
    }

    public static void onBreed(ServerPlayer player, Entity parent) {
        String entityId = entityId(parent);
        GoalProgressService.update(player, progress -> {
            progress.observe("bred_animals", entityId);
            String goal = BREED_GOALS.get(entityId);
            if (goal != null) progress.complete(goal);
            int unique = progress.observationCount("bred_animals");
            if (unique >= 4) progress.complete("BREED_4_UNIQUE_ANIMALS");
            if (unique >= 6) progress.complete("BREED_6_UNIQUE_ANIMALS");
            if (unique >= 8) progress.complete("BREED_8_UNIQUE_ANIMALS");
        });
    }

    public static void onTame(ServerPlayer player, Entity animal) {
        String goal = TAME_GOALS.get(entityId(animal));
        if (goal != null) GoalProgressService.complete(player, Set.of(goal));
    }

    public static void onBrew(ServerPlayer player, Holder<Potion> potion) {
        GoalProgressService.update(player, progress -> {
            progress.complete("USE_BREWING_STAND");
            if (isAny(potion, Potions.HEALING, Potions.STRONG_HEALING)) progress.complete("BREW_HEALING_POTION");
            if (isAny(potion, Potions.INVISIBILITY, Potions.LONG_INVISIBILITY)) progress.complete("BREW_INVISIBILITY_POTION");
            if (isAny(potion, Potions.POISON, Potions.LONG_POISON, Potions.STRONG_POISON)) progress.complete("BREW_POISON_POTION");
            if (isAny(potion, Potions.SWIFTNESS, Potions.LONG_SWIFTNESS, Potions.STRONG_SWIFTNESS)) progress.complete("BREW_SWIFTNESS_POTION");
            if (isAny(potion, Potions.WATER_BREATHING, Potions.LONG_WATER_BREATHING)) progress.complete("BREW_WATER_BREATHING_POTION");
            if (isAny(potion, Potions.WEAKNESS, Potions.LONG_WEAKNESS)) progress.complete("BREW_WEAKNESS_POTION");
        });
    }

    @SafeVarargs
    private static boolean isAny(Holder<Potion> value, Holder<Potion>... choices) {
        for (Holder<Potion> choice : choices) if (value.equals(choice)) return true;
        return false;
    }

    private static String entityId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
    }
}
