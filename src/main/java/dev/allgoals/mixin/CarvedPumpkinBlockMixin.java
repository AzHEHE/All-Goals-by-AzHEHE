package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalActionContext;
import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(CarvedPumpkinBlock.class)
abstract class CarvedPumpkinBlockMixin {
    @Inject(method = "trySpawnGolem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;spawn(Lnet/minecraft/world/level/block/WeatheringCopper$WeatherState;)V"))
    private void allGoals$copperGolemSpawned(Level level, BlockPos topPos, CallbackInfo callbackInfo) {
        if (level.isClientSide()) return;
        ServerPlayer player = GoalActionContext.pumpkinCarver();
        if (player == null) player = GoalActionContext.blockPlacer();
        if (player != null) GoalProgressService.complete(player, Set.of("CONSTRUCT_COPPER_GOLEM"));
    }
}
