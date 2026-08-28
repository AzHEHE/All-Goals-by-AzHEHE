package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(EnderMan.class)
abstract class EnderManMixin {
    @Inject(method = "setTarget", at = @At("RETURN"))
    private void allGoals$recordEndermanAnger(LivingEntity target, CallbackInfo callbackInfo) {
        if (target instanceof ServerPlayer player) {
            GoalProgressService.complete(player, Set.of("ENRAGE_ENDERMAN"));
        }
    }
}
