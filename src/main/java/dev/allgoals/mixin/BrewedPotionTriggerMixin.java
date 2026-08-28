package dev.allgoals.mixin;

import dev.allgoals.tracking.CriteriaGoalTracker;
import net.minecraft.advancements.criterion.BrewedPotionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewedPotionTrigger.class)
abstract class BrewedPotionTriggerMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void allGoals$recordBrew(ServerPlayer player, Holder<Potion> potion, CallbackInfo callbackInfo) {
        CriteriaGoalTracker.onBrew(player, potion);
    }
}
