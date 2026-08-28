package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Allay.class)
abstract class AllayMixin {
    @Unique private int allGoals$amethystBefore;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void allGoals$beforeInteract(Player player, InteractionHand hand,
                                         CallbackInfoReturnable<InteractionResult> callbackInfo) {
        allGoals$amethystBefore = player.getItemInHand(hand).is(Items.AMETHYST_SHARD)
                ? player.getItemInHand(hand).getCount() : -1;
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void allGoals$afterInteract(Player player, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (allGoals$amethystBefore > player.getItemInHand(hand).getCount() && player instanceof ServerPlayer serverPlayer) {
            GoalProgressService.complete(serverPlayer, Set.of("DUPLICATE_ALLAY"));
        }
        allGoals$amethystBefore = -1;
    }
}
