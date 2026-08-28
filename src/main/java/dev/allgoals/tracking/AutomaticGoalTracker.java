package dev.allgoals.tracking;

import dev.allgoals.progress.PlayerGoalProgress;
import dev.allgoals.goal.VariantGoalIds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import dev.allgoals.world.RunModeManager;

/**
 * Server-authoritative polling tracker. Vanilla statistics provide event history;
 * inventory/equipment/world state cover goals that are true right now.
 */
public final class AutomaticGoalTracker {
    private static final int HISTORY_CHECK_INTERVAL_TICKS = 4;
    private static final int INVENTORY_FALLBACK_INTERVAL_TICKS = 4;

    private static final Map<String, String> DIRECT_OBTAIN_ITEMS = Map.ofEntries(
            entry("OBTAIN_ACTIVATOR_RAIL", "activator_rail"),
            entry("OBTAIN_ANCIENT_DEBRIS", "ancient_debris"),
            entry("OBTAIN_AMETHYST_BLOCK", "amethyst_block"),
            entry("OBTAIN_BELL", "bell"),
            entry("OBTAIN_RESIN_BLOCK", "resin_block"),
            entry("OBTAIN_BONE_BLOCK", "bone_block"),
            entry("OBTAIN_BOOKSHELF", "bookshelf"),
            entry("OBTAIN_ENCHANT_BOTTLE", "experience_bottle"),
            entry("OBTAIN_BRICK_WALL", "brick_wall"),
            entry("OBTAIN_TROPICAL_FISH_BUCKET", "tropical_fish_bucket"),
            entry("OBTAIN_CLOCK", "clock"),
            entry("OBTAIN_COBWEB", "cobweb"),
            entry("OBTAIN_COPPER_CHEST", "copper_chest"),
            entry("OBTAIN_DAYLIGHT_DETECTOR", "daylight_detector"),
            entry("OBTAIN_DEAD_BUSH", "dead_bush"),
            entry("OBTAIN_DETECTOR_RAIL", "detector_rail"),
            entry("OBTAIN_DISPENSER", "dispenser"),
            entry("OBTAIN_DRIED_KELP_BLOCK", "dried_kelp_block"),
            entry("OBTAIN_EMERALD_BLOCK", "emerald_block"),
            entry("OBTAIN_ENDER_CHEST", "ender_chest"),
            entry("OBTAIN_END_ROD", "end_rod"),
            entry("OBTAIN_DRAGON_EGG", "dragon_egg"),
            entry("OBTAIN_FLOWERING_AZALEA", "flowering_azalea"),
            entry("OBTAIN_GILDED_BLACKSTONE", "gilded_blackstone"),
            entry("OBTAIN_HEART_OF_THE_SEA", "heart_of_the_sea"),
            entry("OBTAIN_LODESTONE", "lodestone"),
            entry("OBTAIN_MOSSY_STONE_BRICK_WALL", "mossy_stone_brick_wall"),
            entry("OBTAIN_MUD_BRICK_WALL", "mud_brick_wall"),
            entry("OBTAIN_NETHERITE_SCRAP", "netherite_scrap"),
            entry("OBTAIN_PISTON", "piston"),
            entry("OBTAIN_POWDER_SNOW_BUCKET", "powder_snow_bucket"),
            entry("OBTAIN_POWERED_RAIL", "powered_rail"),
            entry("OBTAIN_RED_NETHER_BRICK_STAIRS", "red_nether_brick_stairs"),
            entry("OBTAIN_REDSTONE_COMPARATOR", "comparator"),
            entry("OBTAIN_REDSTONE_LAMP", "redstone_lamp"),
            entry("OBTAIN_REDSTONE_REPEATER", "repeater"),
            entry("OBTAIN_RESIN_BRICK_WALL", "resin_brick_wall"),
            entry("OBTAIN_SCAFFOLDING", "scaffolding"),
            entry("OBTAIN_SMOOTH_BASALT", "smooth_basalt"),
            entry("OBTAIN_SMOOTH_QUARTZ_STAIRS", "smooth_quartz_stairs"),
            entry("OBTAIN_SOUL_LANTERN", "soul_lantern"),
            entry("OBTAIN_SPONGE", "sponge"),
            entry("OBTAIN_TNT", "tnt"),
            entry("OBTAIN_TINTED_GLASS", "tinted_glass"),
            entry("OBTAIN_WITHER_SKELETON_SKULL", "wither_skeleton_skull"),
            entry("OBTAIN_WRITTEN_BOOK", "written_book")
    );

    private static final Map<String, String> SIMPLE_KILLS = Map.ofEntries(
            entry("KILL_BAT", "bat"), entry("KILL_BOGGED", "bogged"),
            entry("KILL_ELDER_GUARDIAN", "elder_guardian"), entry("KILL_GUARDIAN", "guardian"),
            entry("KILL_HUSK", "husk"), entry("KILL_SILVERFISH", "silverfish"),
            entry("KILL_GHAST", "ghast"), entry("KILL_PARCHED", "parched"),
            entry("KILL_SNOW_GOLEM", "snow_golem"), entry("KILL_STRAY", "stray"),
            entry("KILL_WARDEN", "warden"), entry("KILL_WITCH", "witch"),
            entry("KILL_ZOGLIN", "zoglin"), entry("KILL_ZOMBIE_VILLAGER", "zombie_villager")
    );

    private static final Map<String, String> SIMPLE_MINES = Map.ofEntries(
            entry("MINE_MOB_SPAWNER", "spawner"), entry("MINE_CRAFTER", "crafter"),
            entry("MINE_TURTLE_EGG", "turtle_egg")
    );

    private static final Map<String, String> SIMPLE_EFFECTS = Map.ofEntries(
            entry("GET_ABSORPTION_STATUS_EFFECT", "absorption"),
            entry("GET_BAD_OMEN_STATUS_EFFECT", "bad_omen"),
            entry("GET_GLOWING_STATUS_EFFECT", "glowing"),
            entry("GET_JUMP_BOOST_STATUS_EFFECT", "jump_boost"),
            entry("GET_MINING_FATIGUE_STATUS_EFFECT", "mining_fatigue"),
            entry("GET_NAUSEA_STATUS_EFFECT", "nausea"),
            entry("GET_POISON_STATUS_EFFECT", "poison"),
            entry("GET_WEAKNESS_STATUS_EFFECT", "weakness")
    );

    private static final Set<String> SAPLING_ITEMS = Set.of(
            "oak_sapling", "acacia_sapling", "birch_sapling", "cherry_sapling", "dark_oak_sapling",
            "jungle_sapling", "spruce_sapling", "mangrove_propagule", "pale_oak_sapling",
            "azalea", "flowering_azalea"
    );
    private static final Set<String> SEED_ITEMS = Set.of(
            "wheat_seeds", "beetroot_seeds", "melon_seeds", "pumpkin_seeds", "torchflower_seeds", "nether_wart"
    );
    private static final Set<String> BUCKET_ITEMS = Set.of(
            "bucket", "water_bucket", "cod_bucket", "salmon_bucket", "lava_bucket", "milk_bucket",
            "tropical_fish_bucket", "pufferfish_bucket", "axolotl_bucket", "powder_snow_bucket", "tadpole_bucket"
    );
    private static final Set<String> FLOWER_ITEMS = Set.of(
            "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip",
            "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "torchflower",
            "wither_rose", "sunflower", "lilac", "rose_bush", "peony", "closed_eyeblossom", "open_eyeblossom",
            "cactus_flower", "chorus_flower", "golden_dandelion", "pitcher_plant", "cherry_leaves",
            "flowering_azalea", "flowering_azalea_leaves", "mangrove_propagule", "pink_petals",
            "spore_blossom", "wildflowers"
    );
    private static final Set<String> HORSE_ARMOR_ITEMS = Set.of(
            "iron_horse_armor", "leather_horse_armor", "diamond_horse_armor", "golden_horse_armor",
            "copper_horse_armor", "netherite_horse_armor"
    );
    private static final Set<String> WORKSTATION_ITEMS = Set.of(
            "blast_furnace", "smoker", "cartography_table", "brewing_stand", "barrel", "composter",
            "fletching_table", "cauldron", "lectern", "stonecutter", "loom", "smithing_table", "grindstone"
    );
    private static final Set<String> ARMOR_MATERIALS = Set.of(
            "leather", "copper", "golden", "chainmail", "iron", "diamond", "netherite"
    );
    private static final Map<Integer, String> DYE_COLOR_RGB = Map.ofEntries(
            Map.entry(16383998, "white"), Map.entry(16351261, "orange"), Map.entry(13061821, "magenta"),
            Map.entry(3847130, "light_blue"), Map.entry(16701501, "yellow"), Map.entry(8439583, "lime"),
            Map.entry(15961002, "pink"), Map.entry(4673362, "gray"), Map.entry(10329495, "light_gray"),
            Map.entry(1481884, "cyan"), Map.entry(8991416, "purple"), Map.entry(3949738, "blue"),
            Map.entry(8606770, "brown"), Map.entry(6192150, "green"), Map.entry(11546150, "red"),
            Map.entry(1908001, "black")
    );
    private static final Set<String> BREAKABLE_ARMOR_MATERIALS = Set.of(
            "leather", "copper", "golden", "chainmail", "iron", "diamond"
    );
    private static final Set<String> MISC_TOOLS = Set.of(
            "fishing_rod", "flint_and_steel", "shears", "brush", "carrot_on_a_stick", "warped_fungus_on_a_stick"
    );
    private static final Map<String, String> TOOL_MATERIALS = Map.of(
            "WOODEN", "wooden", "STONE", "stone", "IRON", "iron",
            "GOLDEN", "golden", "DIAMOND", "diamond", "COPPER", "copper"
    );
    private static final String TOOL_SPEAR_MIGRATION = "tool_sets_include_spear_v1";
    private static final String ADVANCEMENT_COUNT_MIGRATION = "advancement_count_v2";
    private static final String WORKSTATION_ACTION_MIGRATION = "workstation_actions_v1";
    private static final String CRAFTING_RESULTS_MIGRATION = "crafting_results_v1";
    private static final String CAULDRON_STATS_MIGRATION = "cauldron_stats_v1";
    private static final Set<String> SPYGLASS_ADVANCEMENTS = Set.of(
            "adventure/spyglass_at_parrot", "adventure/spyglass_at_ghast", "adventure/spyglass_at_dragon"
    );
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    );
    private static final List<String> DYE_COLORS = List.of(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    );
    private static final Set<String> NETHER_BIOMES = Set.of(
            "nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas"
    );
    private static final Set<String> CAVE_BIOMES = Set.of("lush_caves", "dripstone_caves", "deep_dark");
    private static final Map<UUID, CachedInventory> INVENTORY_CACHE = new HashMap<>();

    private static long ticks;

    private AutomaticGoalTracker() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(AutomaticGoalTracker::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                INVENTORY_CACHE.remove(handler.getPlayer().getUUID()));
    }

    private static void onServerTick(MinecraftServer server) {
        if (!RunModeManager.tracksGoals(server)) return;
        ticks++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            checkFastPlayer(player);
            if (isHistoryCheckTick(player)) checkSlowPlayer(player);
        }
    }

    private static boolean isHistoryCheckTick(ServerPlayer player) {
        // Keep history-backed goals responsive without scanning every player on
        // the same server tick. At normal TPS the maximum delay is 0.2 seconds.
        return Math.floorMod(ticks + player.getUUID().hashCode(), HISTORY_CHECK_INTERVAL_TICKS) == 0;
    }

    private static void checkFastPlayer(ServerPlayer player) {
        PlayerGoalProgress oldProgress = GoalProgressService.activeProgress(player);
        PlayerGoalProgress.Editor progress = oldProgress.edit();
        CachedInventory cachedInventory = cachedInventory(player);
        boolean checkedInventory = cachedInventory.checkedProgress != oldProgress;
        if (checkedInventory) {
            applyInventorySnapshot(progress, cachedInventory.snapshot);
            checkDirectObtainGoals(progress, cachedInventory.snapshot.counts());
            checkInventoryCollections(progress, cachedInventory.snapshot.counts());
            checkUniqueInventory(player, progress);
        }
        checkCurrentState(player, progress);
        checkCauldronStatistics(player, progress);
        PlayerGoalProgress saved = saveProgress(player, oldProgress, progress);
        if (checkedInventory) cachedInventory.checkedProgress = saved;
    }

    private static void checkSlowPlayer(ServerPlayer player) {
        PlayerGoalProgress oldProgress = GoalProgressService.activeProgress(player);
        PlayerGoalProgress.Editor progress = oldProgress.edit();
        repairOldCraftingProgress(progress);
        ConsumptionGoalTracker.repairLegacyStatProgress(progress);
        checkVanillaStatistics(player, progress);
        checkAdvancements(player, progress);
        saveProgress(player, oldProgress, progress);
    }

    private static PlayerGoalProgress saveProgress(ServerPlayer player, PlayerGoalProgress oldProgress,
                                                   PlayerGoalProgress.Editor progress) {
        return GoalProgressService.save(player, oldProgress, progress);
    }

    private static CachedInventory cachedInventory(ServerPlayer player) {
        UUID playerId = player.getUUID();
        int revision = player.getInventory().getTimesChanged();
        CachedInventory cached = INVENTORY_CACHE.get(playerId);
        if (cached != null && cached.revision == revision
                && ticks - cached.scannedAtTick < INVENTORY_FALLBACK_INTERVAL_TICKS) {
            return cached;
        }

        InventorySnapshot snapshot = readInventory(player);
        PlayerGoalProgress checkedProgress = cached != null && cached.snapshot.equals(snapshot)
                ? cached.checkedProgress : null;
        CachedInventory refreshed = new CachedInventory(revision, ticks, snapshot, checkedProgress);
        INVENTORY_CACHE.put(playerId, refreshed);
        return refreshed;
    }

    private static InventorySnapshot readInventory(ServerPlayer player) {
        Map<String, Integer> counts = new HashMap<>();
        Set<String> observedItems = new HashSet<>();
        boolean decoratedShield = false;
        boolean copperChest = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            counts.merge(itemId, stack.getCount(), Integer::sum);
            observedItems.add(itemId);
            if (itemId.equals("shield") && stack.get(DataComponents.BASE_COLOR) != null) {
                decoratedShield = true;
            }
            if (stack.is(ItemTags.COPPER_CHESTS)) copperChest = true;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                counts.merge(itemId, stack.getCount(), Integer::sum);
                observedItems.add(itemId);
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(offhand.getItem()).getPath();
            observedItems.add(itemId);
            if (itemId.equals("shield") && offhand.get(DataComponents.BASE_COLOR) != null) {
                decoratedShield = true;
            }
            if (offhand.is(ItemTags.COPPER_CHESTS)) copperChest = true;
        }
        return new InventorySnapshot(Map.copyOf(counts), Set.copyOf(observedItems), decoratedShield, copperChest);
    }

    private static void applyInventorySnapshot(PlayerGoalProgress.Editor progress, InventorySnapshot snapshot) {
        Set<String> obtained = progress.observations("obtained_items");
        if (!obtained.containsAll(snapshot.observedItems())) {
            snapshot.observedItems().forEach(itemId -> progress.observe("obtained_items", itemId));
        }
        if (snapshot.decoratedShield()) progress.complete("PUT_BANNER_ON_SHIELD");
        if (snapshot.copperChest()) progress.complete("OBTAIN_COPPER_CHEST");
    }

    private static void checkDirectObtainGoals(PlayerGoalProgress.Editor progress, Map<String, Integer> inventory) {
        DIRECT_OBTAIN_ITEMS.forEach((goal, item) -> {
            if (inventory.getOrDefault(item, 0) > 0 || progress.observations("obtained_items").contains(item)) {
                progress.complete(goal);
            }
        });
        if (inventory.values().stream().anyMatch(count -> count >= 64)) progress.complete("OBTAIN_STACK_OF_64");
        if (inventory.getOrDefault("arrow", 0) >= 32 || inventory.getOrDefault("spectral_arrow", 0) >= 32) {
            progress.complete("OBTAIN_32_ARROWS");
        }
        if (inventory.getOrDefault("arrow", 0) >= 64) progress.complete("OBTAIN_64_ARROWS");
        if (inventory.getOrDefault("coarse_dirt", 0) >= 64) progress.complete("OBTAIN_64_COARSE_DIRT");
        if (inventory.containsKey("suspicious_sand") || inventory.containsKey("suspicious_gravel")) progress.complete("OBTAIN_SUSPICIOUS_BLOCK");
        if (inventory.keySet().stream().anyMatch(id -> id.endsWith("_pottery_sherd"))) progress.complete("OBTAIN_POTTERY_SHERD");
    }

    private static void checkInventoryCollections(PlayerGoalProgress.Editor progress, Map<String, Integer> inventory) {
        Set<String> seen = inventory.keySet();
        repairOldToolCompletions(progress);
        completeIfAll(progress, seen, "OBTAIN_ALL_FURNACES", "furnace", "blast_furnace", "smoker");
        completeIfAll(progress, seen, "OBTAIN_ALL_MUSHROOMS",
                "red_mushroom", "brown_mushroom", "crimson_fungus", "warped_fungus");
        completeIfAll(progress, seen, "OBTAIN_ALL_PUMPKINS", "pumpkin", "carved_pumpkin", "jack_o_lantern");
        completeIfAll(progress, seen, "OBTAIN_ALL_RAW_ORE_BLOCKS", "raw_iron_block", "raw_gold_block", "raw_copper_block");
        completeIfAll(progress, seen, "OBTAIN_ALL_TORCHES", "torch", "soul_torch", "redstone_torch", "copper_torch");
        completeIfAll(progress, seen, "OBTAIN_ALL_MINECARTS", "minecart", "chest_minecart", "furnace_minecart",
                "hopper_minecart", "tnt_minecart");
        completeAtLeast(progress, seen, "OBTAIN_3_HORSE_ARMORS", 3, HORSE_ARMOR_ITEMS::contains);

        completeAtLeast(progress, seen, "OBTAIN_4_UNIQUE_SAPLINGS", 4, SAPLING_ITEMS::contains);
        completeAtLeast(progress, seen, "OBTAIN_4_UNIQUE_SEEDS", 4, SEED_ITEMS::contains);
        completeAtLeast(progress, seen, "OBTAIN_6_UNIQUE_BUCKETS", 6, BUCKET_ITEMS::contains);
        completeAtLeast(progress, seen, "OBTAIN_6_UNIQUE_FLOWERS", 6, FLOWER_ITEMS::contains);
        completeAtLeast(progress, seen, "OBTAIN_3_BANNER_PATTERNS", 3, id -> id.endsWith("_banner_pattern"));
        completeAtLeast(progress, seen, "OBTAIN_2_ARMOR_TRIMS", 2, AutomaticGoalTracker::isArmorTrimTemplate);
        completeAtLeast(progress, seen, "OBTAIN_7_UNIQUE_WORKSTATIONS", 7, WORKSTATION_ITEMS::contains);

        checkToolSet(progress, seen, "WOODEN", "wooden");
        checkToolSet(progress, seen, "STONE", "stone");
        checkToolSet(progress, seen, "IRON", "iron");
        checkToolSet(progress, seen, "GOLDEN", "golden");
        checkToolSet(progress, seen, "DIAMOND", "diamond");
        checkToolSet(progress, seen, "COPPER", "copper");

        for (String color : DYE_COLORS) {
            if (inventory.getOrDefault(color + "_wool", 0) >= 64) {
                progress.complete(VariantGoalIds.goalId(VariantGoalIds.COLORED_WOOL, color));
            }
            if (inventory.getOrDefault(color + "_concrete", 0) >= 64) {
                progress.complete(VariantGoalIds.goalId(VariantGoalIds.COLORED_CONCRETE, color));
            }
            if (seen.contains(color + "_glazed_terracotta")) {
                progress.complete(VariantGoalIds.goalId(VariantGoalIds.GLAZED_TERRACOTTA, color));
            }
        }
    }

    private static void checkVanillaStatistics(ServerPlayer player, PlayerGoalProgress.Editor progress) {
        repairOldWorkstationCompletions(progress);
        int mobKills = player.getStats().getValue(Stats.CUSTOM, Stats.MOB_KILLS);
        int damageDealtTenths = player.getStats().getValue(Stats.CUSTOM, Stats.DAMAGE_DEALT);
        int damageTaken = player.getStats().getValue(Stats.CUSTOM, Stats.DAMAGE_TAKEN) / 10;
        int sprintCm = player.getStats().getValue(Stats.CUSTOM, Stats.SPRINT_ONE_CM);
        progress.setCounterAtLeast("mobs_killed", mobKills);
        progress.setCounterAtLeast("damage_dealt_tenths", damageDealtTenths);
        progress.setCounterAtLeast("damage_taken", damageTaken);
        progress.setCounterAtLeast("sprint_cm", sprintCm);
        if (mobKills >= 100) progress.complete("KILL_100_MOBS");
        if (progress.counter("damage_dealt_tenths") >= 4_000) progress.complete("DEAL_400_DAMAGE");
        if (damageTaken >= 200) progress.complete("TAKE_200_DAMAGE");
        if (sprintCm >= 100_000) progress.complete("SPRINT_1_KM");
        if (player.getStats().getValue(Stats.CUSTOM, Stats.EAT_CAKE_SLICE) > 0) progress.complete("EAT_CAKE");
        if (player.getStats().getValue(Stats.CUSTOM, Stats.TUNE_NOTEBLOCK) > 0) progress.complete("TUNE_NOTEBLOCK");
        if (player.getStats().getValue(Stats.CUSTOM, Stats.PLAY_RECORD) > 0) progress.complete("USE_JUKEBOX");
        if (player.getStats().getValue(Stats.CUSTOM, Stats.POT_FLOWER) > 0) progress.complete("PUT_FLOWER_IN_POT");
        if (player.getStats().getValue(Stats.CUSTOM, Stats.ENCHANT_ITEM) > 0) progress.complete("USE_ENCHANTING_TABLE");
        if (player.getStats().getValue(Stats.CUSTOM, Stats.TRADED_WITH_VILLAGER) > 0) progress.complete("GET_WHAT_A_DEAL_ADVANCEMENT");

        SIMPLE_KILLS.forEach((goal, entityId) -> {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(minecraft(entityId));
            if (type != null && player.getStats().getValue(Stats.ENTITY_KILLED, type) > 0) progress.complete(goal);
        });
        int arthropodsKilled = 0;
        int undeadKilled = 0;
        Set<String> raidMobsKilled = new HashSet<>();
        Set<String> arthropods = Set.of("bee", "cave_spider", "spider", "endermite", "silverfish");
        Set<String> undead = Set.of("drowned", "husk", "phantom", "skeleton", "skeleton_horse", "stray", "wither",
                "wither_skeleton", "zoglin", "zombie", "zombie_horse", "zombie_villager", "zombified_piglin",
                "bogged", "parched", "camel_husk", "zombie_nautilus");
        Set<String> raidMobs = Set.of("pillager", "vindicator", "ravager", "witch", "vex", "evoker");
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            int kills = player.getStats().getValue(Stats.ENTITY_KILLED, type);
            if (kills > 0) {
                String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
                if (type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
                    progress.observe("killed_hostile_entities", entityId);
                }
                progress.observe("killed_entities", entityId);
                if (arthropods.contains(entityId)) arthropodsKilled += kills;
                if (undead.contains(entityId)) undeadKilled += kills;
                if (raidMobs.contains(entityId)) raidMobsKilled.add(entityId);
            }
        }
        int uniqueKilled = progress.observationCount("killed_hostile_entities");
        if (uniqueKilled >= 7) progress.complete("KILL_7_UNIQUE_HOSTILE_MOBS");
        if (uniqueKilled >= 10) progress.complete("KILL_10_UNIQUE_HOSTILE_MOBS");
        if (uniqueKilled >= 13) progress.complete("KILL_13_UNIQUE_HOSTILE_MOBS");
        if (uniqueKilled >= 15) progress.complete("KILL_15_UNIQUE_HOSTILE_MOBS");
        progress.setCounterAtLeast("arthropods_killed", arthropodsKilled);
        progress.setCounterAtLeast("undead_killed", undeadKilled);
        if (arthropodsKilled >= 20) progress.complete("KILL_20_ARTHROPOD_MOBS");
        if (undeadKilled >= 30) progress.complete("KILL_30_UNDEAD_MOBS");
        if (raidMobsKilled.containsAll(raidMobs)) progress.complete("KILL_ALL_RAID_MOBS");

        SIMPLE_MINES.forEach((goal, blockId) -> {
            Block block = BuiltInRegistries.BLOCK.getValue(minecraft(blockId));
            if (block != null && player.getStats().getValue(Stats.BLOCK_MINED, block) > 0) progress.complete(goal);
        });
        int iceTypes = 0;
        for (String id : List.of("ice", "packed_ice", "blue_ice")) {
            Block block = BuiltInRegistries.BLOCK.getValue(minecraft(id));
            if (block != null && player.getStats().getValue(Stats.BLOCK_MINED, block) > 0) iceTypes++;
        }
        if (iceTypes >= 3) progress.complete("MINE_3_TYPES_OF_ICE");
        if (wasEitherBlockMined(player, "diamond_ore", "deepslate_diamond_ore")) progress.complete("MINE_DIAMOND_ORE");
        if (wasEitherBlockMined(player, "emerald_ore", "deepslate_emerald_ore")) progress.complete("MINE_EMERALD_ORE");

        boolean brokeTool = false;
        boolean brokeArmor = false;
        boolean usedHangingSign = false;
        for (Item item : BuiltInRegistries.ITEM) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (player.getStats().getValue(Stats.ITEM_BROKEN, item) > 0) {
                brokeTool |= looksLikeTool(itemId);
                brokeArmor |= looksLikeArmor(itemId);
            }
            if (player.getStats().getValue(Stats.ITEM_USED, item) > 0 && itemId.endsWith("_hanging_sign")) {
                usedHangingSign = true;
            }
        }
        if (brokeTool) progress.complete("BREAK_ANY_TOOL");
        if (brokeArmor) progress.complete("BREAK_ANY_ARMOR");
        if (usedHangingSign) progress.complete("PLACE_HANGING_SIGN");
        completeIfItemUsed(player, progress, "painting", "PLACE_PAINTING");
        completeIfItemUsed(player, progress, "goat_horn", "TOOT_GOAT_HORN");
        completeIfItemUsed(player, progress, "carrot_on_a_stick", "RIDE_PIG");
    }

    private static void checkCauldronStatistics(ServerPlayer player, PlayerGoalProgress.Editor progress) {
        int cauldronUses = player.getStats().getValue(Stats.CUSTOM, Stats.CLEAN_ARMOR)
                + player.getStats().getValue(Stats.CUSTOM, Stats.CLEAN_BANNER)
                + player.getStats().getValue(Stats.CUSTOM, Stats.CLEAN_SHULKER_BOX);
        boolean repairCauldron = !progress.observations("migrations").contains(CAULDRON_STATS_MIGRATION);
        setThresholdGoal(progress, "USE_CAULDRON", cauldronUses > 0, repairCauldron);
        progress.observe("migrations", CAULDRON_STATS_MIGRATION);
    }

    private static void checkAdvancements(ServerPlayer player, PlayerGoalProgress.Editor progress) {
        Set<String> completedIds = new HashSet<>();
        for (AdvancementHolder advancement : player.level().getServer().getAdvancements().getAllAdvancements()) {
            // Datapacks may contain internal advancement nodes with no display or no
            // criteria. Empty requirements report themselves as complete immediately,
            // so only real, earnable advancement entries belong in this total. Root
            // advancements still count when they have a display and criteria.
            if (advancement.value().display().isPresent()
                    && !advancement.value().criteria().isEmpty()
                    && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                progress.observe("advancements_obtained", advancement.id().toString());
                completedIds.add(advancement.id().getPath());
            }
        }
        int completed = progress.observationCount("advancements_obtained");
        progress.setCounterAtLeast("advancements", completed);
        boolean repairOldCount = !progress.observations("migrations").contains(ADVANCEMENT_COUNT_MIGRATION);
        setThresholdGoal(progress, "GET_10_ADVANCEMENTS", completed >= 10, repairOldCount);
        setThresholdGoal(progress, "GET_20_ADVANCEMENTS", completed >= 20, repairOldCount);
        setThresholdGoal(progress, "GET_30_ADVANCEMENTS", completed >= 30, repairOldCount);
        progress.observe("migrations", ADVANCEMENT_COUNT_MIGRATION);
        completeForAdvancement(progress, completedIds, "GET_EYE_SPY_ADVANCEMENT", "story/follow_ender_eye");
        completeForAdvancement(progress, completedIds,
                "GET_THE_CITY_AT_THE_END_OF_THE_GAME_ADVANCEMENT", "end/find_end_city");
        completeForAdvancement(progress, completedIds,
                "GET_THOSE_WERE_THE_DAYS_ADVANCEMENT", "nether/find_bastion");
        completeForAdvancement(progress, completedIds,
                "GET_A_TERRIBLE_FORTRESS_ADVANCEMENT", "nether/find_fortress");
        completeForAdvancement(progress, completedIds, "GET_NOT_QUITE_NINE_LIVES_ADVANCEMENT", "nether/charge_respawn_anchor");
        completeForAdvancement(progress, completedIds, "GET_BULLSEYE_ADVANCEMENT", "adventure/bullseye");
        completeForAdvancement(progress, completedIds, "GET_MOB_KABOB_ADVANCEMENT", "adventure/spear_many_mobs");
        completeForAdvancement(progress, completedIds, "GET_OH_SHINY_ADVANCEMENT", "nether/distract_piglin");
        completeForAdvancement(progress, completedIds, "GET_SNIPER_DUEL_ADVANCEMENT", "adventure/sniper_duel");
        completeForAdvancement(progress, completedIds, "GET_STAY_HYDRATED_ADVANCEMENT", "husbandry/place_dried_ghast_in_water");
        completeForAdvancement(progress, completedIds, "GET_THIS_BOAT_HAS_LEGS_ADVANCEMENT", "nether/ride_strider");
        completeForAdvancement(progress, completedIds, "GET_WAX_OFF_ADVANCEMENT", "husbandry/wax_off");
        completeForAdvancement(progress, completedIds, "GET_WAX_ON_ADVANCEMENT", "husbandry/wax_on");
        completeForAdvancement(progress, completedIds, "GET_HIRED_HELP_ADVANCEMENT", "adventure/summon_iron_golem");
        completeForAdvancement(progress, completedIds, "GET_WHAT_A_DEAL_ADVANCEMENT", "adventure/trade");
        completeForAdvancement(progress, completedIds, "USE_BREWING_STAND", "nether/brew_potion");
        if (completedIds.stream().anyMatch(SPYGLASS_ADVANCEMENTS::contains)) {
            progress.complete("GET_ANY_SPYGLASS_ADVANCEMENT");
        }
    }

    private static void setThresholdGoal(PlayerGoalProgress.Editor progress, String goalId,
                                         boolean complete, boolean repairOldCount) {
        if (complete) {
            progress.complete(goalId);
        } else if (repairOldCount) {
            // Repairs worlds affected by the old empty-requirement counting bug.
            progress.uncomplete(goalId);
        }
    }

    private static void checkCurrentState(ServerPlayer player, PlayerGoalProgress.Editor progress) {
        if (player.level().dimension() == Level.NETHER) progress.complete("ENTER_NETHER");
        if (player.level().dimension() == Level.END) progress.complete("ENTER_END");
        if (player.experienceLevel >= 10) progress.complete("REACH_EXP_LEVEL_10");
        if (player.experienceLevel >= 20) progress.complete("REACH_EXP_LEVEL_20");
        if (player.getFoodData().getFoodLevel() == 0) progress.complete("EMPTY_HUNGER_BAR");
        if (player.getBlockY() <= player.level().getMinY()) progress.complete("REACH_BEDROCK");
        if (player.getBlockY() >= player.level().getMaxY() - 1) progress.complete("REACH_HEIGHT_LIMIT");
        if (player.level().dimension() == Level.NETHER && player.getBlockY() >= 128) progress.complete("REACH_NETHER_ROOF");
        int effects = player.getActiveEffects().size();
        if (effects >= 3) progress.complete("GET_3_STATUS_EFFECTS_AT_ONCE");
        if (effects >= 4) progress.complete("GET_4_STATUS_EFFECTS_AT_ONCE");
        if (effects >= 6) progress.complete("GET_6_STATUS_EFFECTS_AT_ONCE");
        Set<String> effectIds = new HashSet<>();
        for (Holder<MobEffect> holder : player.getActiveEffectsMap().keySet()) {
            effectIds.add(BuiltInRegistries.MOB_EFFECT.getKey(holder.value()).getPath());
        }
        SIMPLE_EFFECTS.forEach((goal, effect) -> {
            if (effectIds.contains(effect)) progress.complete(goal);
        });

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AbstractMinecart) progress.complete("RIDE_MINECART");
        if (vehicle != null && BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).getPath().equals("pig")) progress.complete("RIDE_PIG");
        if (vehicle != null && BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).getPath().equals("horse")) progress.complete("RIDE_HORSE");
        if (player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD))) {
            progress.complete("HAVE_YOUR_SHIELD_DISABLED");
        }

        if (!progress.isComplete("LEASH_4_UNIQUE_MOBS")
                || !progress.isComplete("LEASH_6_UNIQUE_MOBS")
                || !progress.isComplete("LEASH_8_UNIQUE_MOBS")
                || !progress.isComplete("LEASH_IRON_GOLEM")) {
            Set<String> leashedTypes = new HashSet<>();
            for (Leashable leashable : Leashable.leashableLeashedTo(player)) {
                if (leashable instanceof Entity entity) {
                    leashedTypes.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
                }
            }
            leashedTypes.forEach(id -> progress.observe("leashed_entities", id));
            int leashedNow = leashedTypes.size();
            if (leashedNow >= 4) progress.complete("LEASH_4_UNIQUE_MOBS");
            if (leashedNow >= 6) progress.complete("LEASH_6_UNIQUE_MOBS");
            if (leashedNow >= 8) progress.complete("LEASH_8_UNIQUE_MOBS");
            if (leashedTypes.contains("iron_golem")) progress.complete("LEASH_IRON_GOLEM");
        }

        if (!progress.isComplete("ENRAGE_ZOMBIFIED_PIGLIN")) {
            boolean angryPiglin = !player.level().getEntitiesOfClass(
                    ZombifiedPiglin.class,
                    player.getBoundingBox().inflate(24),
                    piglin -> piglin.isAngryAt(player, player.level())
            ).isEmpty();
            if (angryPiglin) progress.complete("ENRAGE_ZOMBIFIED_PIGLIN");
        }

        checkArmor(player, progress);

        if (!progress.isComplete("VISIT_ALL_NETHER_BIOMES") || !progress.isComplete("VISIT_ALL_CAVE_BIOMES")) {
            player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(key -> {
            String biome = key.identifier().getPath();
            progress.observe("visited_biomes", biome);
            Set<String> visited = progress.observations("visited_biomes");
            if (visited.containsAll(NETHER_BIOMES)) {
                progress.complete("VISIT_ALL_NETHER_BIOMES");
            }
            if (visited.containsAll(CAVE_BIOMES)) {
                progress.complete("VISIT_ALL_CAVE_BIOMES");
            }
            });
        }
    }

    private static void checkArmor(ServerPlayer player, PlayerGoalProgress.Editor progress) {
        List<ItemStack> armor = List.of(
                player.getItemBySlot(EquipmentSlot.HEAD), player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS), player.getItemBySlot(EquipmentSlot.FEET)
        );
        List<String> ids = armor.stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
                .toList();
        if (ids.stream().anyMatch(id -> id.startsWith("chainmail_"))) progress.complete("WEAR_CHAIN_ARMOR_PIECE");
        for (ItemStack stack : armor) {
            if (stack.isEmpty()) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (!itemId.startsWith("leather_")) continue;
            var dyedColor = stack.get(DataComponents.DYED_COLOR);
            if (dyedColor == null) continue;
            String color = DYE_COLOR_RGB.get(dyedColor.rgb());
            if (color != null) {
                progress.complete(VariantGoalIds.goalId(
                        VariantGoalIds.DYED_LEATHER, itemId + "&" + color));
            }
        }
        if (armor.size() == 4 && armor.stream().noneMatch(ItemStack::isEmpty)) {
            for (String material : List.of("iron", "golden", "diamond", "copper")) {
                if (ids.stream().allMatch(id -> id.startsWith(material + "_"))) {
                    progress.complete("WEAR_" + material.toUpperCase() + "_ARMOR");
                }
            }
            if (ids.stream().allMatch(id -> id.startsWith("leather_"))) progress.complete("WEAR_LEATHER_ARMOR");
            if (armor.stream().allMatch(ItemStack::isEnchanted)) progress.complete("WEAR_FULL_ENCHANTED_ARMOR");
            Set<String> materials = new HashSet<>();
            boolean validArmorMaterials = true;
            for (String id : ids) {
                String material = armorMaterial(id);
                if (material == null) {
                    validArmorMaterials = false;
                    break;
                }
                materials.add(material);
            }
            if (validArmorMaterials && materials.size() == 4) progress.complete("WEAR_UNIQUE_ARMOR");
            if (ids.stream().allMatch(id -> id.startsWith("leather_"))) {
                Set<Integer> colors = new HashSet<>();
                boolean allDyed = true;
                for (ItemStack stack : armor) {
                    var color = stack.get(DataComponents.DYED_COLOR);
                    if (color == null || color.rgb() == net.minecraft.world.item.component.DyedItemColor.LEATHER_COLOR) {
                        allDyed = false;
                        break;
                    }
                    colors.add(color.rgb());
                }
                if (allDyed && colors.size() == 4) progress.complete("WEAR_UNIQUE_COLORED_LEATHER_ARMOR");
            }
        }
        if (!armor.getFirst().isEmpty() && BuiltInRegistries.ITEM.getKey(armor.getFirst().getItem()).getPath().equals("carved_pumpkin")) {
            String playerCounter = "carved_pumpkin_ticks:" + player.getUUID();
            int wornTicks = progress.addToCounter(playerCounter, 1);
            progress.setCounterAtLeast("carved_pumpkin_ticks", wornTicks);
            if (wornTicks >= 20 * 60 * 5) {
                progress.complete("WEAR_CARVED_PUMPKIN_FOR_5_MINUTES");
            }
        }
    }

    private static boolean wasEitherBlockMined(ServerPlayer player, String first, String second) {
        Block a = BuiltInRegistries.BLOCK.getValue(minecraft(first));
        Block b = BuiltInRegistries.BLOCK.getValue(minecraft(second));
        return (a != null && player.getStats().getValue(Stats.BLOCK_MINED, a) > 0)
                || (b != null && player.getStats().getValue(Stats.BLOCK_MINED, b) > 0);
    }

    private static void checkToolSet(PlayerGoalProgress.Editor progress, Set<String> seen, String goalMaterial, String itemMaterial) {
        completeIfAll(progress, seen, "OBTAIN_" + goalMaterial + "_TOOLS",
                itemMaterial + "_sword", itemMaterial + "_pickaxe", itemMaterial + "_axe",
                itemMaterial + "_shovel", itemMaterial + "_hoe", itemMaterial + "_spear");
    }

    private static void repairOldToolCompletions(PlayerGoalProgress.Editor progress) {
        if (progress.observations("migrations").contains(TOOL_SPEAR_MIGRATION)) return;
        Set<String> obtained = progress.observations("obtained_items");
        TOOL_MATERIALS.forEach((goalMaterial, itemMaterial) -> {
            String goalId = "OBTAIN_" + goalMaterial + "_TOOLS";
            if (progress.isComplete(goalId) && !obtained.contains(itemMaterial + "_spear")) {
                progress.uncomplete(goalId);
            }
        });
        progress.observe("migrations", TOOL_SPEAR_MIGRATION);
    }

    private static void repairOldWorkstationCompletions(PlayerGoalProgress.Editor progress) {
        if (progress.observations("migrations").contains(WORKSTATION_ACTION_MIGRATION)) return;
        for (String goalId : Set.of("USE_ANVIL", "USE_BREWING_STAND", "USE_GRINDSTONE",
                "USE_LOOM", "USE_SMITHING_TABLE", "USE_STONECUTTER")) {
            progress.uncomplete(goalId);
        }
        progress.observe("migrations", WORKSTATION_ACTION_MIGRATION);
    }

    private static void repairOldCraftingProgress(PlayerGoalProgress.Editor progress) {
        if (progress.observations("migrations").contains(CRAFTING_RESULTS_MIGRATION)) return;
        progress.clearObservations("crafted_items");
        progress.uncomplete("CRAFT_20_UNIQUE_ITEMS");
        progress.uncomplete("CRAFT_50_UNIQUE_ITEMS");
        progress.uncomplete("CRAFT_100_UNIQUE_ITEMS");
        progress.uncomplete("CRAFT_ARMOR_TRIM");
        progress.observe("migrations", CRAFTING_RESULTS_MIGRATION);
    }

    private static void checkUniqueInventory(ServerPlayer player, PlayerGoalProgress.Editor progress) {
        Set<Item> unique = new HashSet<>();
        int occupied = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !unique.add(stack.getItem())) return;
            occupied++;
        }
        if (occupied == 36) progress.complete("FILL_INVENTORY_UNIQUE_ITEMS");
    }

    private static String armorMaterial(String itemId) {
        for (String material : ARMOR_MATERIALS) {
            if (itemId.startsWith(material + "_")) return material;
        }
        return null;
    }

    private static void completeIfAll(PlayerGoalProgress.Editor progress, Set<String> values, String goal, String... required) {
        if (Set.of(required).stream().allMatch(values::contains)) progress.complete(goal);
    }

    private static void completeAtLeast(PlayerGoalProgress.Editor progress, Set<String> values, String goal,
                                        int required, java.util.function.Predicate<String> predicate) {
        if (values.stream().filter(predicate).count() >= required) progress.complete(goal);
    }

    private static boolean looksLikeTool(String id) {
        if (MISC_TOOLS.contains(id)) return true;
        return TOOL_MATERIALS.values().stream().anyMatch(material -> id.startsWith(material + "_"))
                && (id.endsWith("_sword") || id.endsWith("_pickaxe") || id.endsWith("_axe")
                || id.endsWith("_shovel") || id.endsWith("_hoe") || id.endsWith("_spear"));
    }

    private static boolean looksLikeArmor(String id) {
        return BREAKABLE_ARMOR_MATERIALS.stream().anyMatch(material -> id.startsWith(material + "_"))
                && (id.endsWith("_helmet") || id.endsWith("_chestplate")
                || id.endsWith("_leggings") || id.endsWith("_boots"));
    }

    private static boolean isArmorTrimTemplate(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(minecraft(id));
        return item instanceof SmithingTemplateItem
                && !item.getDefaultInstance().is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    }

    private static void completeIfItemUsed(ServerPlayer player, PlayerGoalProgress.Editor progress,
                                           String itemId, String goalId) {
        Item item = BuiltInRegistries.ITEM.getValue(minecraft(itemId));
        if (item != null && player.getStats().getValue(Stats.ITEM_USED, item) > 0) progress.complete(goalId);
    }

    private static void completeForAdvancement(PlayerGoalProgress.Editor progress, Set<String> completed,
                                               String goalId, String advancementId) {
        if (completed.contains(advancementId)) progress.complete(goalId);
    }

    private static Identifier minecraft(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private static Map.Entry<String, String> entry(String key, String value) {
        return Map.entry(key, value);
    }

    private record InventorySnapshot(
            Map<String, Integer> counts,
            Set<String> observedItems,
            boolean decoratedShield,
            boolean copperChest
    ) {
    }

    private static final class CachedInventory {
        private final int revision;
        private final long scannedAtTick;
        private final InventorySnapshot snapshot;
        private PlayerGoalProgress checkedProgress;

        private CachedInventory(int revision, long scannedAtTick,
                                InventorySnapshot snapshot, PlayerGoalProgress checkedProgress) {
            this.revision = revision;
            this.scannedAtTick = scannedAtTick;
            this.snapshot = snapshot;
            this.checkedProgress = checkedProgress;
        }
    }
}
