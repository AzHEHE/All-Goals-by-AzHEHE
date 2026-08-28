package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ComposterBlock.class)
abstract class ComposterBlockMixin {
    @Inject(method = "extractProduce", at = @At("RETURN"))
    private static void allGoals$recordComposterUse(Entity source, BlockState state, Level level, BlockPos pos,
                                                     CallbackInfoReturnable<BlockState> callbackInfo) {
        if (source instanceof ServerPlayer player) {
            GoalProgressService.complete(player, Set.of("USE_COMPOSTER"));
        }
    }

    @Inject(method = "addItem", at = @At("HEAD"))
    private static void allGoals$recordCompostedFood(Entity source, BlockState state, LevelAccessor level,
                                                      BlockPos pos, ItemStack stack,
                                                      CallbackInfoReturnable<BlockState> callbackInfo) {
        if (!(source instanceof ServerPlayer player)
                || (!stack.has(DataComponents.FOOD) && !stack.is(Items.CAKE))) return;
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        GoalProgressService.update(player, progress -> {
            progress.observe("composted_foods", itemId);
            int foods = progress.observationCount("composted_foods");
            if (foods >= 3) progress.complete("COMPOST_3_UNIQUE_FOODS");
            if (foods >= 5) progress.complete("COMPOST_5_UNIQUE_FOODS");
            if (foods >= 7) progress.complete("COMPOST_7_UNIQUE_FOODS");
        });
    }
}
