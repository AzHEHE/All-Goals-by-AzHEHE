package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Allay.class)
abstract class AllayMixin {
    @Inject(method = "mobInteract", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/allay/Allay;duplicateAllay()V"))
    private void allGoals$duplicate(Player player, InteractionHand hand,
                                    CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (player instanceof ServerPlayer serverPlayer) {
            GoalProgressService.complete(serverPlayer, Set.of("DUPLICATE_ALLAY"));
        }
    }
}
