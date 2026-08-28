package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(CrossbowItem.class)
abstract class CrossbowItemMixin {
    @Inject(method = "performShooting", at = @At("HEAD"))
    private void allGoals$recordFirework(Level level, LivingEntity shooter, InteractionHand hand,
                                         ItemStack crossbow, float speed, float divergence, LivingEntity target,
                                         CallbackInfo callbackInfo) {
        ChargedProjectiles charged = crossbow.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        if (shooter instanceof ServerPlayer player && charged.contains(Items.FIREWORK_ROCKET)) {
            GoalProgressService.complete(player, Set.of("SHOOT_FIREWORK_FROM_CROSSBOW"));
        }
    }
}
