package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalActionContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("HEAD"))
    private void allGoals$capturePlacer(BlockPlaceContext context,
                                        CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            GoalActionContext.captureBlockPlacer(player);
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void allGoals$clearPlacer(BlockPlaceContext context,
                                      CallbackInfoReturnable<InteractionResult> callbackInfo) {
        GoalActionContext.clearBlockPlacer();
    }
}
