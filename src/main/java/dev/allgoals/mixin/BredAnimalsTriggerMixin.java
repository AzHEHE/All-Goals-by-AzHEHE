package dev.allgoals.mixin;

import dev.allgoals.tracking.CriteriaGoalTracker;
import net.minecraft.advancements.criterion.BredAnimalsTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BredAnimalsTrigger.class)
abstract class BredAnimalsTriggerMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void allGoals$recordBreed(ServerPlayer player, Animal parent, Animal partner,
                                      AgeableMob child, CallbackInfo callbackInfo) {
        CriteriaGoalTracker.onBreed(player, parent, child);
    }
}
