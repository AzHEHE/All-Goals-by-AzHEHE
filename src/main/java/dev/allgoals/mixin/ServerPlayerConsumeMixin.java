package dev.allgoals.mixin;

import dev.allgoals.tracking.ConsumptionGoalTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerConsumeMixin {
    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void allGoals$recordConsumedItem(CallbackInfo callbackInfo) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ItemStack consumed = player.getUseItem().copy();
        if (!consumed.isEmpty()) ConsumptionGoalTracker.onConsume(player, consumed);
    }
}
