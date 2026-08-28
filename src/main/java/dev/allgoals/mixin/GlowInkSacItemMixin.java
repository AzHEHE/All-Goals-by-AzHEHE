package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.GlowInkSacItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(GlowInkSacItem.class)
abstract class GlowInkSacItemMixin {
    @Inject(method = "tryApplyToSign", at = @At("RETURN"))
    private void allGoals$recordGlowingSign(Level level, SignBlockEntity sign, boolean front,
                                            ItemStack stack, Player player,
                                            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!Boolean.TRUE.equals(callbackInfo.getReturnValue()) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        String signId = BuiltInRegistries.ITEM.getKey(
                level.getBlockState(sign.getBlockPos()).getBlock().asItem()).getPath();
        Set<String> goals = new LinkedHashSet<>();
        if (signId.equals("warped_sign") || signId.equals("warped_hanging_sign")) {
            goals.add("USE_GLOW_INK_NETHER");
            goals.add("USE_GLOW_INK_WARPED");
        } else if (signId.equals("crimson_sign") || signId.equals("crimson_hanging_sign")) {
            goals.add("USE_GLOW_INK_NETHER");
            goals.add("USE_GLOW_INK_CRIMSON");
        }
        GoalProgressService.complete(serverPlayer, goals);
    }
}
