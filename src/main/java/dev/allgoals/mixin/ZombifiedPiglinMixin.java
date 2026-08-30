package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ZombifiedPiglin.class)
abstract class ZombifiedPiglinMixin {
    @Inject(method = "setPersistentAngerTarget", at = @At("HEAD"))
    private void allGoals$angerTarget(@Nullable EntityReference<LivingEntity> target, CallbackInfo callbackInfo) {
        if (target == null) return;
        ZombifiedPiglin piglin = (ZombifiedPiglin) (Object) this;
        ServerPlayer player = piglin.level().getServer().getPlayerList().getPlayer(target.getUUID());
        if (player != null) GoalProgressService.complete(player, Set.of("ENRAGE_ZOMBIFIED_PIGLIN"));
    }
}
