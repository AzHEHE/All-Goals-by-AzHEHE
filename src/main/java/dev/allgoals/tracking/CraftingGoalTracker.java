package dev.allgoals.tracking;

import dev.allgoals.progress.AudioSettingsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;

public final class CraftingGoalTracker {
    private static final String CRAFTING_RESULTS_MIGRATION = "crafting_results_v1";

    private CraftingGoalTracker() {
    }

    public static void onCraft(ServerPlayer player, ItemStack crafted) {
        String itemId = BuiltInRegistries.ITEM.getKey(crafted.getItem()).getPath();
        boolean[] added = {false};
        int[] total = {0};
        GoalProgressService.update(player, progress -> {
            if (!progress.observations("migrations").contains(CRAFTING_RESULTS_MIGRATION)) {
                progress.clearObservations("crafted_items");
                progress.uncomplete("CRAFT_20_UNIQUE_ITEMS");
                progress.uncomplete("CRAFT_50_UNIQUE_ITEMS");
                progress.uncomplete("CRAFT_100_UNIQUE_ITEMS");
                progress.uncomplete("CRAFT_ARMOR_TRIM");
                progress.observe("migrations", CRAFTING_RESULTS_MIGRATION);
            }
            int before = progress.observationCount("crafted_items");
            total[0] = progress.observe("crafted_items", itemId);
            added[0] = total[0] > before;
            if (total[0] >= 20) progress.complete("CRAFT_20_UNIQUE_ITEMS");
            if (total[0] >= 50) progress.complete("CRAFT_50_UNIQUE_ITEMS");
            if (total[0] >= 100) progress.complete("CRAFT_100_UNIQUE_ITEMS");
            if (crafted.getItem() instanceof SmithingTemplateItem
                    && !crafted.is(net.minecraft.world.item.Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
                progress.complete("CRAFT_ARMOR_TRIM");
            }
        });

        if (!added[0] || total[0] > 100) return;
        if (AudioSettingsManager.get(player).uniqueCrafts()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, SoundSource.MASTER, 2.0F, 2.0F);
        }
        if (total[0] % 5 == 0) {
            player.sendSystemMessage(Component.literal("You have crafted " + total[0] + " unique items.")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        player.sendOverlayMessage(Component.literal("Unique crafts: " + total[0]));
    }
}
