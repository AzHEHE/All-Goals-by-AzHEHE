package dev.allgoals.mixin;

import dev.allgoals.tracking.GoalProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ItemFrame.class)
abstract class ItemFrameMixin {
    @Inject(method = "interact", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void allGoals$recordNestedItemFrame(Player player, InteractionHand hand, Vec3 location,
                                                 CallbackInfoReturnable<InteractionResult> callbackInfo) {
        ItemFrame frame = (ItemFrame) (Object) this;
        if (player instanceof ServerPlayer serverPlayer && !(frame instanceof GlowItemFrame)
                && player.getItemInHand(hand).is(Items.ITEM_FRAME)) {
            GoalProgressService.complete(serverPlayer, Set.of("ITEM_FRAME_IN_ITEM_FRAME"));
        }
    }
}
