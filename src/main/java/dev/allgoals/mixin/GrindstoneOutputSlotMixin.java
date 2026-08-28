package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
abstract class GrindstoneOutputSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void allGoals$recordGrindstoneOutput(Player player, ItemStack stack, CallbackInfo callbackInfo) {
        if (player instanceof ServerPlayer serverPlayer
                && player.containerMenu instanceof GrindstoneMenu grindstone
                && (Object) this == grindstone.slots.get(2)) {
            GoalProgressService.complete(serverPlayer, Set.of("USE_GRINDSTONE"));
        }
    }
}
