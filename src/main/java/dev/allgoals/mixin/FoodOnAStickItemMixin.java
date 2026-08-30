package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FoodOnAStickItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(FoodOnAStickItem.class)
abstract class FoodOnAStickItemMixin {
    @Inject(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtAndConvertOnBreak(ILnet/minecraft/world/level/ItemLike;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private void allGoals$successfulBoost(Level level, Player player, InteractionHand hand,
                                          CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (player instanceof ServerPlayer serverPlayer && player.getItemInHand(hand).is(Items.CARROT_ON_A_STICK)) {
            GoalProgressService.complete(serverPlayer, Set.of("RIDE_PIG"));
        }
    }
}
