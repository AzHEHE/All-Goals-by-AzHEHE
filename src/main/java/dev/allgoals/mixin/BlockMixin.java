package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import dev.allgoals.tracking.IceGoalProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
abstract class BlockMixin {
    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void allGoals$blockMined(Level level, BlockPos pos, BlockState state, Player player,
                                     CallbackInfoReturnable<BlockState> callbackInfo) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        GoalProgressService.update(serverPlayer, progress -> {
            Block block = state.getBlock();
            if (block == Blocks.SPAWNER || block == Blocks.TRIAL_SPAWNER) {
                progress.complete("MINE_MOB_SPAWNER");
            }
            IceGoalProgress.record(progress, block);
        });
    }
}
