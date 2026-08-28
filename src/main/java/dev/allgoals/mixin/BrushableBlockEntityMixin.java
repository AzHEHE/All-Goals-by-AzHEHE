package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(BrushableBlockEntity.class)
abstract class BrushableBlockEntityMixin {
    @Inject(method = "brushingCompleted", at = @At("HEAD"))
    private void allGoals$recordFinishedBrushing(ServerLevel level, LivingEntity user,
                                                  ItemStack brush, CallbackInfo callbackInfo) {
        if (user instanceof ServerPlayer player) {
            GoalProgressService.complete(player, Set.of("USE_BRUSH_ON_SUSPICIOUS_BLOCK"));
        }
    }
}
