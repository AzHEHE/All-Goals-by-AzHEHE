package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
abstract class BrewingStandPotionSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void allGoals$recordLingeringPotion(Player player, ItemStack stack, CallbackInfo callbackInfo) {
        if (player instanceof ServerPlayer serverPlayer && stack.is(Items.LINGERING_POTION)) {
            GoalProgressService.complete(serverPlayer, Set.of("BREW_LINGERING_POTION"));
        }
    }
}
