package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
    @Inject(method = "onTake", at = @At("TAIL"))
    private void allGoals$recordAnvilOutput(Player player, ItemStack stack, CallbackInfo callbackInfo) {
        if (player instanceof ServerPlayer serverPlayer) {
            GoalProgressService.complete(serverPlayer, Set.of("USE_ANVIL"));
        }
    }
}
