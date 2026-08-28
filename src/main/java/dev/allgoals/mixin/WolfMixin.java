package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Wolf.class)
abstract class WolfMixin {
    @Inject(method = "mobInteract", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/wolf/Wolf;setBodyArmorItem(Lnet/minecraft/world/item/ItemStack;)V",
            ordinal = 0))
    private void allGoals$recordWolfArmor(Player player, InteractionHand hand,
                                           CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (player instanceof ServerPlayer serverPlayer) {
            GoalProgressService.complete(serverPlayer, Set.of("PUT_WOLF_ARMOR_ON_WOLF"));
        }
    }
}
