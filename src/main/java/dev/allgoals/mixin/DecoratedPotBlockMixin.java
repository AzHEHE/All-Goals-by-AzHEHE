package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(DecoratedPotBlock.class)
abstract class DecoratedPotBlockMixin {
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void allGoals$recordFilledPot(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit,
                                           CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || callbackInfo.getReturnValue() != InteractionResult.SUCCESS) return;
        if (level.getBlockEntity(pos) instanceof DecoratedPotBlockEntity pot) {
            ItemStack stored = pot.getTheItem();
            if (!stored.isEmpty() && stored.getCount() >= stored.getMaxStackSize()) {
                GoalProgressService.complete(serverPlayer, Set.of("FILL_DECORATED_POT"));
            }
        }
    }
}
