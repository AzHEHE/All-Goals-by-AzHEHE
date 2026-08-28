package dev.allgoals.tracking;

import dev.allgoals.progress.AudioSettingsManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

import java.util.List;

final class VictoryCelebration {
    private static final double[][] ROCKET_OFFSETS = {
            {0.0, 0.0}, {-1.5, -1.0}, {1.5, -1.0}, {-1.5, 1.0}, {1.5, 1.0}
    };

    private VictoryCelebration() {
    }

    static void play(ServerPlayer player) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("All Goals completed").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        ));

        boolean audioEnabled = AudioSettingsManager.get(player).victory();
        if (audioEnabled) {
            level.playSound(null, x, y, z,
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.5F, 1.0F);
            level.playSound(null, x, y, z,
                    SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.2F, 0.8F);
            level.playSound(null, x, y + 3.0, z,
                    SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.MASTER, 1.4F, 1.0F);
        }

        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                x, y + 1.0, z, 120, 0.9, 1.1, 0.9, 0.2);
        level.sendParticles(ParticleTypes.END_ROD,
                x, y + 1.2, z, 80, 1.2, 1.4, 1.2, 0.08);
        level.sendParticles(ParticleTypes.FIREWORK,
                x, y + 2.0, z, 100, 1.5, 1.5, 1.5, 0.16);

        if (audioEnabled) {
            launchHarmlessRockets(level, x, y, z);
            showFireworkBurst(level, x, y + 3.0, z,
                    FireworkExplosion.Shape.STAR,
                    new int[]{0xFFD700, 0xFFFFFF, 0x55FFFF},
                    new int[]{0xFFAA00, 0x55FF55});
            showFireworkBurst(level, x - 2.0, y + 2.5, z + 0.5,
                    FireworkExplosion.Shape.LARGE_BALL,
                    new int[]{0xFF55FF, 0x5555FF, 0xFFFFFF},
                    new int[]{0x55FFFF});
            showFireworkBurst(level, x + 2.0, y + 2.5, z + 0.5,
                    FireworkExplosion.Shape.BURST,
                    new int[]{0x55FF55, 0xFFFF55, 0xFF5555},
                    new int[]{0xFFFFFF});
        }
    }

    private static void launchHarmlessRockets(ServerLevel level, double x, double y, double z) {
        ItemStack rocketStack = new ItemStack(Items.FIREWORK_ROCKET);
        rocketStack.set(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
        for (double[] offset : ROCKET_OFFSETS) {
            level.addFreshEntity(new FireworkRocketEntity(
                    level, x + offset[0], y + 0.25, z + offset[1], rocketStack.copy()
            ));
        }
    }

    private static void showFireworkBurst(ServerLevel level, double x, double y, double z,
                                          FireworkExplosion.Shape shape, int[] colors, int[] fadeColors) {
        FireworkExplosion explosion = new FireworkExplosion(
                shape, new IntArrayList(colors), new IntArrayList(fadeColors), true, true
        );
        ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
        firework.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));

        FireworkRocketEntity visual = new FireworkRocketEntity(level, x, y, z, firework);
        if (level.addFreshEntity(visual)) {
            level.broadcastEntityEvent(visual, (byte) 17);
            visual.discard();
        }
    }
}
