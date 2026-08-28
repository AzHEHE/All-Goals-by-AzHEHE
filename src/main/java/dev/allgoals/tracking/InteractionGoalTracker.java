package dev.allgoals.tracking;

import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CandleBlock;

import java.util.LinkedHashSet;
import java.util.Set;

public final class InteractionGoalTracker {
    private InteractionGoalTracker() {
    }

    public static void initialize() {
        ItemEvents.USE_ON.register(context -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return null;
            ItemStack stack = context.getItemInHand();
            String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            String block = BuiltInRegistries.BLOCK.getKey(context.getLevel().getBlockState(context.getClickedPos()).getBlock()).getPath();
            Set<String> goals = new LinkedHashSet<>();
            if (item.equals("carved_pumpkin") && block.contains("copper_block")) goals.add("CONSTRUCT_COPPER_GOLEM");
            if ((item.equals("flint_and_steel") || item.equals("fire_charge"))
                    && CandleBlock.canLight(context.getLevel().getBlockState(context.getClickedPos()))) {
                goals.add("LIGHT_CANDLE");
            }
            GoalProgressService.update(player, progress -> {
                goals.forEach(progress::complete);
            });
            return null;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            ItemStack stack = player.getItemInHand(hand);
            String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            String target = entityId(entity);
            Set<String> goals = new LinkedHashSet<>();
            if (item.equals("golden_dandelion") && entity instanceof AgeableMob ageable && ageable.isBaby()) {
                goals.add("USE_GOLDEN_DANDELION");
            }
            if (item.equals("name_tag") && stack.getCustomName() != null) {
                String name = stack.getCustomName().getString();
                if (target.equals("sheep") && name.equals("jeb_")) goals.add("NAME_SHEEP_JEB");
                if (target.equals("ghast") && (name.equals("Dinnerbone") || name.equals("Grumm"))) {
                    goals.add("TURN_GHAST_UPSIDE_DOWN");
                }
                if (target.equals("iron_golem") && (name.equals("Dinnerbone") || name.equals("Grumm"))) {
                    goals.add("TURN_IRON_GOLEM_UPSIDE_DOWN");
                }
            }
            GoalProgressService.complete(serverPlayer, goals);
            return InteractionResult.PASS;
        });
    }

    private static String entityId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
    }
}
