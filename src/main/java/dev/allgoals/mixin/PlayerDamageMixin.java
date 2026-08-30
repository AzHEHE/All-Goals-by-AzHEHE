package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerDamageMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void allGoals$damageTaken(ServerLevel level, DamageSource source, float amount,
                                      CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!callbackInfo.getReturnValue() || !((Object) this instanceof ServerPlayer player) || amount <= 0.0F) return;
        int tenths = Math.max(1, Math.round(amount * 10.0F));
        GoalProgressService.update(player, progress -> {
            if (progress.addToCounter("damage_taken_tenths", tenths) >= 2_000) {
                progress.complete("TAKE_200_DAMAGE");
            }
        });
    }
}
