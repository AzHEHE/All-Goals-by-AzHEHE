package dev.allgoals.mixin;

import dev.allgoals.tracking.CraftingGoalTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
abstract class CraftingResultSlotMixin {
    @Shadow @Final private Player player;
    @Shadow private int removeCount;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void allGoals$recordUniqueCraft(ItemStack crafted, CallbackInfo callbackInfo) {
        if (removeCount >= 0 && !crafted.isEmpty() && player instanceof ServerPlayer serverPlayer
                && (player.containerMenu instanceof CraftingMenu || player.containerMenu instanceof InventoryMenu)) {
            CraftingGoalTracker.onCraft(serverPlayer, crafted);
        }
    }
}
