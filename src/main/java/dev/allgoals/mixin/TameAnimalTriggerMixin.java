package dev.allgoals.mixin;

import dev.allgoals.tracking.CriteriaGoalTracker;
import net.minecraft.advancements.criterion.TameAnimalTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TameAnimalTrigger.class)
abstract class TameAnimalTriggerMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void allGoals$recordTame(ServerPlayer player, Animal animal, CallbackInfo callbackInfo) {
        CriteriaGoalTracker.onTame(player, animal);
    }
}
