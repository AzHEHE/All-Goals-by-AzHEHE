package dev.allgoals.tracking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.Map;
import java.util.Set;

public final class ConsumptionGoalTracker {
    private static final String FOOD_CONSUMPTION_MIGRATION = "food_consumption_events_v1";
    private static final Set<String> LEGACY_FOOD_GOALS = Set.of(
            "EAT_GLOW_BERRY", "EAT_POISONOUS_POTATO", "EAT_BEETROOT_SOUP", "EAT_COOKIE",
            "EAT_CHORUS_FRUIT", "EAT_PUMPKIN_PIE", "EAT_RABBIT_STEW", "EAT_SUSPICIOUS_STEW", "EAT_ALL_SOUPS",
            "EAT_5_UNIQUE_FOOD", "EAT_10_UNIQUE_FOOD", "EAT_15_UNIQUE_FOOD",
            "EAT_20_UNIQUE_FOOD", "EAT_25_UNIQUE_FOOD", "DRINK_HONEY_BOTTLE"
    );
    private static final Map<String, String> SINGLE_FOOD_GOALS = Map.ofEntries(
            Map.entry("glow_berries", "EAT_GLOW_BERRY"),
            Map.entry("poisonous_potato", "EAT_POISONOUS_POTATO"),
            Map.entry("beetroot_soup", "EAT_BEETROOT_SOUP"),
            Map.entry("cookie", "EAT_COOKIE"),
            Map.entry("chorus_fruit", "EAT_CHORUS_FRUIT"),
            Map.entry("pumpkin_pie", "EAT_PUMPKIN_PIE"),
            Map.entry("rabbit_stew", "EAT_RABBIT_STEW"),
            Map.entry("suspicious_stew", "EAT_SUSPICIOUS_STEW")
    );
    private ConsumptionGoalTracker() {
    }

    public static void onConsume(ServerPlayer player, ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        int effectsBefore = player.getActiveEffects().size();
        GoalProgressService.update(player, progress -> {
            repairLegacyStatProgress(progress);
            // Draftout treats every item with vanilla food properties as a unique
            // food. Reading the component keeps this correct when Minecraft adds
            // new foods and avoids counting non-food drinks such as honey bottles.
            if (stack.has(DataComponents.FOOD)) {
                progress.observe("eaten_foods", itemId);
                String directGoal = SINGLE_FOOD_GOALS.get(itemId);
                if (directGoal != null) progress.complete(directGoal);
                int foods = progress.observationCount("eaten_foods");
                if (foods >= 5) progress.complete("EAT_5_UNIQUE_FOOD");
                if (foods >= 10) progress.complete("EAT_10_UNIQUE_FOOD");
                if (foods >= 15) progress.complete("EAT_15_UNIQUE_FOOD");
                if (foods >= 20) progress.complete("EAT_20_UNIQUE_FOOD");
                if (foods >= 25) progress.complete("EAT_25_UNIQUE_FOOD");
                if (progress.observations("eaten_foods").containsAll(
                        Set.of("suspicious_stew", "mushroom_stew", "rabbit_stew", "beetroot_soup"))) {
                    progress.complete("EAT_ALL_SOUPS");
                }
            }
            if (stack.is(Items.HONEY_BOTTLE)) progress.complete("DRINK_HONEY_BOTTLE");
            if (stack.is(Items.MILK_BUCKET) && effectsBefore > 0) {
                progress.complete("REMOVE_STATUS_EFFECT_USING_MILK");
            }
            PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
            if (potion != null && potion.is(Potions.WATER)) progress.complete("DRINK_WATER_BOTTLE");
        });
    }

    static void repairLegacyStatProgress(dev.allgoals.progress.PlayerGoalProgress.Editor progress) {
        if (progress.observations("migrations").contains(FOOD_CONSUMPTION_MIGRATION)) return;
        progress.clearObservations("eaten_foods");
        LEGACY_FOOD_GOALS.forEach(progress::uncomplete);
        progress.observe("migrations", FOOD_CONSUMPTION_MIGRATION);
    }
}
