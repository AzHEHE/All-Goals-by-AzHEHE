package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ArmorStand.class)
abstract class ArmorStandMixin {
    @Inject(method = "interact", at = @At("RETURN"))
    private void allGoals$recordFilledArmorStand(Player player, InteractionHand hand, Vec3 location,
                                                  CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || callbackInfo.getReturnValue() != InteractionResult.SUCCESS_SERVER) return;
        ArmorStand stand = (ArmorStand) (Object) this;
        if (!stand.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && !stand.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && !stand.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                && !stand.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
            GoalProgressService.complete(serverPlayer, Set.of("FILL_ARMOR_STAND"));
        }
    }
}
