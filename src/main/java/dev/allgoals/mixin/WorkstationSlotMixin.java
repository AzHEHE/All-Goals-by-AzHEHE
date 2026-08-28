package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(Slot.class)
abstract class WorkstationSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void allGoals$recordWorkstationOutput(Player player, ItemStack stack, CallbackInfo callbackInfo) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (player.containerMenu instanceof StonecutterMenu stonecutter && (Object) this == stonecutter.slots.get(1)) {
            GoalProgressService.complete(serverPlayer, Set.of("USE_STONECUTTER"));
        } else if (player.containerMenu instanceof LoomMenu loom && (Object) this == loom.getResultSlot()) {
            GoalProgressService.complete(serverPlayer, Set.of("USE_LOOM"));
        }
    }
}
