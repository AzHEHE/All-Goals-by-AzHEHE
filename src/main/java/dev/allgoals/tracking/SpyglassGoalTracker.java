package dev.allgoals.tracking;

import dev.allgoals.progress.AudioSettingsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class SpyglassGoalTracker {
    private SpyglassGoalTracker() {
    }

    public static void onSpyglassTick(ServerPlayer player) {
        Vec3 from = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 to = from.add(view.x * 100.0, view.y * 100.0, view.z * 100.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(), player, from, to, new AABB(from, to).inflate(1.0),
                entity -> !entity.isSpectator(), 0.0F
        );
        if (hit == null || !(hit.getEntity() instanceof Mob mob) || !player.hasLineOfSight(mob)) return;

        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
        boolean[] added = {false};
        int[] total = {0};
        GoalProgressService.update(player, progress -> {
            int before = progress.observationCount("spied_entities");
            total[0] = progress.observe("spied_entities", entityId);
            added[0] = total[0] > before;
            if (total[0] >= 10) progress.complete("SPY_10_UNIQUE_MOBS");
            if (total[0] >= 15) progress.complete("SPY_15_UNIQUE_MOBS");
            if (total[0] >= 20) progress.complete("SPY_20_UNIQUE_MOBS");
            if (total[0] >= 25) progress.complete("SPY_25_UNIQUE_MOBS");
            if (entityId.equals("iron_golem")) progress.complete("SPY_IRON_GOLEM");
            if (entityId.equals("piglin_brute")) progress.complete("SPY_PIGLIN_BRUTE");
            if (entityId.equals("enderman")) progress.complete("SPY_ENDERMAN");
        });

        if (!added[0] || total[0] > 25) return;
        if (AudioSettingsManager.get(player).spyglass()) {
            player.level().playSound((Entity) null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.MASTER, 2.0F, 1.0F);
        }
        if (total[0] % 5 == 0) {
            player.sendSystemMessage(Component.literal("You have spied on " + total[0] + " unique mobs.")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        player.sendOverlayMessage(Component.literal("Mobs spied on: " + total[0]));
    }
}
