package dev.allgoals.mixin;

import dev.allgoals.tracking.SpyglassGoalTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerSpyglassMixin {
    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void allGoals$recordSpyglassTarget(ItemStack useItem, CallbackInfo callbackInfo) {
        if (useItem.is(Items.SPYGLASS)) SpyglassGoalTracker.onSpyglassTick((ServerPlayer) (Object) this);
    }
}
