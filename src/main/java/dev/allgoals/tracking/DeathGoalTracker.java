package dev.allgoals.tracking;

import dev.allgoals.goal.VariantGoalIds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.FallLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import dev.allgoals.world.RunModeManager;

public final class DeathGoalTracker {
    private static final int HARDCORE_ESCAPE_TICKS = 5 * 20;
    private static final Set<String> ARTHROPODS = Set.of(
            "bee", "cave_spider", "spider", "endermite", "silverfish"
    );
    private static final Set<String> UNDEAD = Set.of(
            "drowned", "husk", "phantom", "skeleton", "skeleton_horse", "stray", "wither",
            "wither_skeleton", "zoglin", "zombie", "zombie_horse", "zombie_villager",
            "zombified_piglin", "bogged", "parched", "camel_husk", "zombie_nautilus"
    );
    private static final Set<String> HOSTILE_RIDEABLES = Set.of(
            "skeleton_horse", "zombie_horse", "zombie_nautilus", "camel_husk"
    );
    private static final Set<DyeColor> DRAFTOUT_SHEEP_COLORS = Set.of(
            DyeColor.BLUE, DyeColor.CYAN, DyeColor.GREEN, DyeColor.LIGHT_BLUE, DyeColor.LIME,
            DyeColor.MAGENTA, DyeColor.ORANGE, DyeColor.PURPLE, DyeColor.RED, DyeColor.YELLOW
    );

    private DeathGoalTracker() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                recordDeath(player, source);
            }
            if (entity.getKillCredit() instanceof ServerPlayer killer) {
                recordSpecialKill(killer, entity, source);
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) ->
                recordDealtDamage(source, baseDamageTaken));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damageAmount) -> {
            recordDealtDamage(source, damageAmount);
            if (entity instanceof ServerPlayer player && protectHardcoreGoalDeath(player, source)) {
                return false;
            }
            return true;
        });
    }

    private static void recordSpecialKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        String victimId = entityId(victim);
        Set<String> goals = new LinkedHashSet<>();
        if (victim instanceof Sheep sheep && DRAFTOUT_SHEEP_COLORS.contains(sheep.getColor())) {
            goals.add(VariantGoalIds.goalId(
                    VariantGoalIds.COLORED_SHEEP, sheep.getColor().toString()));
        }
        if (victimId.equals("snow_golem") && victim.level().dimension() == Level.NETHER) {
            goals.add("KILL_SNOW_GOLEM_IN_NETHER");
        }
        if (victimId.equals("breeze") && source.is(DamageTypes.WIND_CHARGE)) {
            goals.add("KILL_BREEZE_USING_WIND_CHARGE");
        }
        if (victimId.equals("ghast")) goals.add("KILL_GHAST");
        if (victimId.equals("parched")) goals.add("KILL_PARCHED");
        GoalProgressService.update(player, progress -> {
            goals.forEach(progress::complete);
            progress.observe("killed_entities", victimId);
            if (victim instanceof Enemy || isHostileRideable(victim)) {
                int unique = progress.observe("killed_hostile_entities", victimId);
                if (unique >= 7) progress.complete("KILL_7_UNIQUE_HOSTILE_MOBS");
                if (unique >= 10) progress.complete("KILL_10_UNIQUE_HOSTILE_MOBS");
                if (unique >= 13) progress.complete("KILL_13_UNIQUE_HOSTILE_MOBS");
                if (unique >= 15) progress.complete("KILL_15_UNIQUE_HOSTILE_MOBS");
            }
            int mobsKilled = progress.addToCounter("mobs_killed", 1);
            if (mobsKilled >= 100) progress.complete("KILL_100_MOBS");
            if (ARTHROPODS.contains(victimId)
                    && progress.addToCounter("arthropods_killed", 1) >= 20) {
                progress.complete("KILL_20_ARTHROPOD_MOBS");
            }
            if (UNDEAD.contains(victimId)
                    && progress.addToCounter("undead_killed", 1) >= 30) {
                progress.complete("KILL_30_UNDEAD_MOBS");
            }
        });
    }

    private static boolean isHostileRideable(Entity entity) {
        return HOSTILE_RIDEABLES.contains(entityId(entity))
                && entity.getPassengers().stream().anyMatch(Enemy.class::isInstance);
    }

    private static void recordDealtDamage(DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer player) || amount <= 0.0F) return;
        int tenths = Math.max(1, Math.round(amount * 10.0F));
        GoalProgressService.update(player, progress -> {
            if (progress.addToCounter("damage_dealt_tenths", tenths) >= 4_000) {
                progress.complete("DEAL_400_DAMAGE");
            }
        });
    }

    private static void recordDeath(ServerPlayer player, DamageSource source) {
        complete(player, matchingDeathGoals(player, source));
    }

    private static Set<String> matchingDeathGoals(ServerPlayer player, DamageSource source) {
        Set<String> goals = new LinkedHashSet<>();
        if (source.is(DamageTypes.DROWN)) goals.add("DIE_BY_DROWNING");
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) goals.add("DIE_TO_VOID");
        if (source.is(DamageTypes.FREEZE)) goals.add("FREEZE_TO_DEATH");
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) goals.add("DIE_BY_MAGIC");
        if (source.is(DamageTypes.BAD_RESPAWN_POINT)) goals.add("DIE_BY_INTENTIONAL_GAME_DESIGN");
        if (source.is(DamageTypes.STING)) goals.add("DIE_BY_BEE_STING");
        if (source.is(DamageTypes.SWEET_BERRY_BUSH)) goals.add("DIE_BY_BERRY_BUSH");
        if (source.is(DamageTypes.CACTUS)) goals.add("DIE_BY_CACTUS");
        if (source.is(DamageTypes.FALLING_ANVIL)) goals.add("DIE_BY_ANVIL");
        if (source.is(DamageTypes.FALLING_STALACTITE)) goals.add("DIE_BY_FALLING_STALACTITE");
        if (source.is(DamageTypes.FIREWORKS)) goals.add("DIE_BY_FIREWORK");
        if (source.is(DamageTypes.TRIDENT)) goals.add("DIE_TO_TRIDENT");
        if (source.is(DamageTypes.FALL) && fellFromVines(player)) {
            goals.add("DIE_BY_FALLING_OFF_VINE");
        }

        String attacker = entityId(source.getEntity());
        String direct = entityId(source.getDirectEntity());
        if (attacker.equals("iron_golem")) goals.add("DIE_BY_IRON_GOLEM");
        if (attacker.equals("polar_bear")) goals.add("DIE_TO_POLAR_BEAR");
        if (attacker.equals("pufferfish")) goals.add("DIE_TO_PUFFERFISH");
        if (attacker.equals("warden")) goals.add("DIE_TO_WARDEN");
        if (direct.equals("tnt_minecart")) goals.add("DIE_BY_TNT_MINECART");
        return goals;
    }

    /**
     * Hardcore worlds cannot normally complete a death goal and continue the
     * run. An unfinished matching goal therefore acts as a one-use, tightly
     * scoped death protection. Once shared/solo progress contains the goal,
     * the same cause is fatal again.
     */
    private static boolean protectHardcoreGoalDeath(ServerPlayer player, DamageSource source) {
        if (!RunModeManager.tracksGoals(player.level().getServer())
                || !player.level().getServer().isHardcore()
                || hasVanillaDeathProtection(player, source)) return false;

        Set<String> eligibleGoals = new LinkedHashSet<>();
        var progress = GoalProgressService.activeProgress(player);
        for (String goalId : matchingDeathGoals(player, source)) {
            if (progress.canCompleteAutomatically(goalId)) eligibleGoals.add(goalId);
        }
        if (eligibleGoals.isEmpty()) return false;

        // Complete first so a second fatal hit can never reuse the protection.
        complete(player, eligibleGoals);
        rescuePlayer(player, source);
        return true;
    }

    private static boolean hasVanillaDeathProtection(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).get(DataComponents.DEATH_PROTECTION) != null) return true;
        }
        return false;
    }

    private static void rescuePlayer(ServerPlayer player, DamageSource source) {
        player.setHealth(1.0F);
        player.invulnerableTime = Math.max(player.invulnerableTime, HARDCORE_ESCAPE_TICKS);
        player.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE,
                HARDCORE_ESCAPE_TICKS,
                4,
                false,
                false,
                false
        ));
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.setTicksFrozen(0);
        player.resetFallDistance();

        // Void damage continues every tick and ignores ordinary protection, so
        // return the player to their bed/anchor (or world spawn) immediately.
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            player.teleport(player.findRespawnPositionAndUseSpawnBlock(
                    false,
                    TeleportTransition.DO_NOTHING
            ));
        }
    }

    private static boolean fellFromVines(ServerPlayer player) {
        FallLocation location = FallLocation.getCurrentFallLocation(player);
        if (location == null) return false;
        if (location == FallLocation.VINES || location == FallLocation.TWISTING_VINES
                || location == FallLocation.WEEPING_VINES) return true;
        if (location != FallLocation.OTHER_CLIMBABLE) return false;
        return player.getLastClimbablePos().map(pos -> {
            var state = player.level().getBlockState(pos);
            return state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT);
        }).orElse(false);
    }

    private static void complete(ServerPlayer player, Set<String> goals) {
        GoalProgressService.complete(player, goals);
    }

    private static String entityId(Entity entity) {
        return entity == null ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
    }
}
