package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(net.minecraft.world.entity.AgeableMob.class)
abstract class AgeableMobMixin {
    @Inject(method = "setAgeLocked(Lnet/minecraft/world/entity/Mob;Ljava/util/function/Supplier;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)V",
            at = @At("TAIL"))
    private static void allGoals$ageLocked(Mob mob, Supplier<Boolean> locked, Player player,
                                            ItemStack heldItem, Consumer<Mob> setter, CallbackInfo callbackInfo) {
        if (locked.get() && player instanceof ServerPlayer serverPlayer) {
            GoalProgressService.complete(serverPlayer, Set.of("USE_GOLDEN_DANDELION"));
        }
    }
}
