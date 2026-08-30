package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalActionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PumpkinBlock.class)
abstract class PumpkinBlockMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void allGoals$captureCarver(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                        Player player, InteractionHand hand, BlockHitResult hit,
                                        CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (!level.isClientSide() && stack.is(Items.SHEARS) && player instanceof ServerPlayer serverPlayer) {
            GoalActionContext.capturePumpkinCarver(serverPlayer);
        }
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void allGoals$clearCarver(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                      Player player, InteractionHand hand, BlockHitResult hit,
                                      CallbackInfoReturnable<InteractionResult> callbackInfo) {
        GoalActionContext.clearPumpkinCarver();
    }
}
