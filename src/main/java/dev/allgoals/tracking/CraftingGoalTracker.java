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
    private CraftingGoalTracker() {
    }

    public static void onCraft(ServerPlayer player, ItemStack crafted) {
        String itemId = BuiltInRegistries.ITEM.getKey(crafted.getItem()).getPath();
        boolean[] added = {false};
        int[] total = {0};
        GoalProgressService.update(player, progress -> {
            CraftingProgress.Result result = CraftingProgress.record(progress, itemId);
            total[0] = result.total();
            added[0] = result.added();
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
