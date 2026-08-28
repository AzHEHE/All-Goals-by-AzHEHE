package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(EndCrystal.class)
abstract class EndCrystalMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void allGoals$recordExplosion(ServerLevel level, DamageSource source, float amount,
                                          CallbackInfoReturnable<Boolean> callbackInfo) {
        if (callbackInfo.getReturnValue() && source.getEntity() instanceof ServerPlayer player) {
            GoalProgressService.complete(player, Set.of("EXPLODE_END_CRYSTAL"));
        }
    }
}
