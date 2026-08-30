package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(AbstractCandleBlock.class)
abstract class AbstractCandleBlockMixin {
    @Inject(method = "onProjectileHit", at = @At("TAIL"))
    private void allGoals$projectileLitCandle(Level level, BlockState oldState, BlockHitResult hit,
                                              Projectile projectile, CallbackInfo callbackInfo) {
        if (!level.isClientSide() && !oldState.getValue(AbstractCandleBlock.LIT)
                && level.getBlockState(hit.getBlockPos()).getValue(AbstractCandleBlock.LIT)
                && projectile.getOwner() instanceof ServerPlayer player) {
            GoalProgressService.complete(player, Set.of("LIGHT_CANDLE"));
        }
    }
}
