package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(CampfireBlockEntity.class)
abstract class CampfireBlockEntityMixin {
    @Inject(method = "placeFood", at = @At("RETURN"))
    private void allGoals$recordFilledCampfire(ServerLevel level, LivingEntity source, ItemStack stack,
                                                CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!(source instanceof ServerPlayer player) || !callbackInfo.getReturnValueZ()) return;
        CampfireBlockEntity campfire = (CampfireBlockEntity) (Object) this;
        if (campfire.getItems().stream().noneMatch(ItemStack::isEmpty)) {
            GoalProgressService.complete(player, Set.of("FILL_CAMPFIRE"));
        }
    }
}
