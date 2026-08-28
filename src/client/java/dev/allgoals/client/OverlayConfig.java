package dev.allgoals.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.allgoals.AllGoals;
import dev.allgoals.goal.GoalDefinition;
import dev.allgoals.progress.PlayerAudioSettings;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class OverlayConfig {
    static final int DEFAULT_CLASSIC_BORDER_RGB = 0xE2B85C;
    static final int DEFAULT_CLASSIC_BAR_RGB = 0x5BE7C4;
    static final int DEFAULT_CLASSIC_ICON_RGB = 0x9A8F79;
    static final int DEFAULT_CLASSIC_TIMER_RGB = 0xFFD966;

    static final List<String> DEFAULT_PINS = List.of(
            "KILL_WARDEN",
            "OBTAIN_WITHER_SKELETON_SKULL",
            "OBTAIN_RESIN_BRICK_WALL",
            "BREED_FROGS"
    );
    static final float[] SCALES = {0.5625F, 0.75F, 0.9375F};
    private static final int MAX_SAVED_TIMERS = 256;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("all_goals_overlay.json");
    private static final Path TEMP_CONFIG_PATH = CONFIG_PATH.resolveSibling(
            CONFIG_PATH.getFileName() + ".tmp");

    boolean visible = true;
    boolean timerEnabled = true;
    boolean pinnedGoalsVisible = true;
    OverlayAnchor anchor = OverlayAnchor.TOP_LEFT;
    int scaleIndex = 1;
    int iconsPerRow = 4;
    int classicBorderRgb = DEFAULT_CLASSIC_BORDER_RGB;
    int classicBarRgb = DEFAULT_CLASSIC_BAR_RGB;
    int classicIconRgb = DEFAULT_CLASSIC_ICON_RGB;
    int classicTimerRgb = DEFAULT_CLASSIC_TIMER_RGB;
    float goalCompletionVolume = 1.0F;
    boolean uniqueCraftSound = true;
    boolean spyglassSound = true;
    boolean victorySound = true;
    boolean announcementsEnabled = true;
    List<String> pinnedGoalIds = new ArrayList<>(DEFAULT_PINS);
    Map<String, Long> timerMillis = new LinkedHashMap<>();
    private int hudScaleCalibration = 1;
    private transient List<GoalDefinition> resolvedPinnedGoals;

    static OverlayConfig load() {
        if (!Files.exists(CONFIG_PATH)) return new OverlayConfig();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            boolean legacyScale = !json.has("hudScaleCalibration");
            boolean missingAnnouncements = !json.has("announcementsEnabled");
            OverlayConfig config = GSON.fromJson(json, OverlayConfig.class);
            if (config == null) config = new OverlayConfig();
            if (legacyScale) config.scaleIndex = Math.min(SCALES.length - 1, config.scaleIndex + 1);
            if (missingAnnouncements) config.announcementsEnabled = true;
            config.normalize();
            return config;
        } catch (Exception exception) {
            AllGoals.LOGGER.warn("Could not read All Goals overlay settings; using defaults", exception);
            return new OverlayConfig();
        }
    }

    void save() {
        normalize();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(TEMP_CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(TEMP_CONFIG_PATH, CONFIG_PATH,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(TEMP_CONFIG_PATH, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            AllGoals.LOGGER.warn("Could not save All Goals overlay settings", exception);
        }
    }

    float scale() {
        return SCALES[scaleIndex];
    }

    String scaleLabel() {
        return switch (scaleIndex) {
            case 0 -> "75%";
            case 2 -> "125%";
            default -> "100%";
        };
    }

    void cycleScale() {
        scaleIndex = (scaleIndex + 1) % SCALES.length;
        save();
    }

    void cycleAnchor() {
        OverlayAnchor[] anchors = OverlayAnchor.values();
        anchor = anchors[(anchor.ordinal() + 1) % anchors.length];
        save();
    }

    void cycleIconsPerRow() {
        iconsPerRow = iconsPerRow >= 8 ? 1 : iconsPerRow + 1;
        save();
    }

    boolean isPinned(String goalId) {
        return pinnedGoalIds.contains(goalId);
    }

    List<GoalDefinition> pinnedGoals() {
        if (resolvedPinnedGoals == null) {
            resolvedPinnedGoals = pinnedGoalIds.stream()
                    .map(AllGoals.goalCatalog()::findSource)
                    .flatMap(java.util.Optional::stream)
                    .toList();
        }
        return resolvedPinnedGoals;
    }

    void togglePinned(String goalId) {
        if (pinnedGoalIds.remove(goalId)) {
            resolvedPinnedGoals = null;
            save();
            return;
        }
        pinnedGoalIds.add(goalId);
        resolvedPinnedGoals = null;
        save();
    }

    void resetLayout() {
        visible = true;
        timerEnabled = true;
        pinnedGoalsVisible = true;
        anchor = OverlayAnchor.TOP_LEFT;
        scaleIndex = 1;
        iconsPerRow = 4;
        pinnedGoalIds = new ArrayList<>(DEFAULT_PINS);
        resolvedPinnedGoals = null;
        save();
    }

    void resetClassicColors() {
        classicBorderRgb = DEFAULT_CLASSIC_BORDER_RGB;
        classicBarRgb = DEFAULT_CLASSIC_BAR_RGB;
        classicIconRgb = DEFAULT_CLASSIC_ICON_RGB;
        classicTimerRgb = DEFAULT_CLASSIC_TIMER_RGB;
        save();
    }

    int classicBorderColor() {
        return opaque(classicBorderRgb);
    }

    int classicBarTopColor() {
        return opaque(classicBarRgb);
    }

    int classicBarBottomColor() {
        if (classicBarRgb == DEFAULT_CLASSIC_BAR_RGB) return 0xFF2BAF79;
        return shade(classicBarRgb, 0.68F);
    }

    int classicIncompleteIconColor() {
        return opaque(classicIconRgb);
    }

    int classicCompleteIconColor() {
        if (classicIconRgb == DEFAULT_CLASSIC_ICON_RGB) return 0xFF36784E;
        return shade(classicIconRgb, 0.62F);
    }

    int classicTimerColor() {
        return opaque(classicTimerRgb);
    }

    PlayerAudioSettings audioSettings() {
        return new PlayerAudioSettings(
                goalCompletionVolume, uniqueCraftSound, spyglassSound, victorySound
        );
    }

    long timerFor(String runKey) {
        return Math.max(0L, timerMillis.getOrDefault(runKey, 0L));
    }

    void setTimer(String runKey, long milliseconds) {
        if (runKey == null) return;
        timerMillis.remove(runKey);
        timerMillis.put(runKey, Math.max(0L, milliseconds));
        trimOldTimers();
    }

    private void normalize() {
        if (anchor == null) anchor = OverlayAnchor.TOP_LEFT;
        scaleIndex = Math.max(0, Math.min(SCALES.length - 1, scaleIndex));
        iconsPerRow = Math.max(1, Math.min(8, iconsPerRow));
        classicBorderRgb &= 0xFFFFFF;
        classicBarRgb &= 0xFFFFFF;
        classicIconRgb &= 0xFFFFFF;
        classicTimerRgb &= 0xFFFFFF;
        goalCompletionVolume = Float.isFinite(goalCompletionVolume)
                ? Math.clamp(goalCompletionVolume, 0.0F, 1.0F)
                : 1.0F;
        hudScaleCalibration = 1;
        if (pinnedGoalIds == null) pinnedGoalIds = new ArrayList<>(DEFAULT_PINS);
        LinkedHashSet<String> validPins = new LinkedHashSet<>();
        for (String goalId : pinnedGoalIds) {
            if (goalId != null && AllGoals.goalCatalog().findSource(goalId).isPresent()) validPins.add(goalId);
        }
        List<String> normalizedPins = new ArrayList<>(validPins);
        if (!normalizedPins.equals(pinnedGoalIds)) resolvedPinnedGoals = null;
        pinnedGoalIds = normalizedPins;
        if (timerMillis == null) timerMillis = new LinkedHashMap<>();
        timerMillis.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        timerMillis.replaceAll((key, value) -> Math.max(0L, value));
        trimOldTimers();
    }

    private void trimOldTimers() {
        while (timerMillis.size() > MAX_SAVED_TIMERS) {
            var iterator = timerMillis.keySet().iterator();
            if (!iterator.hasNext()) return;
            iterator.next();
            iterator.remove();
        }
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    private static int shade(int rgb, float factor) {
        int red = Math.clamp(Math.round(((rgb >> 16) & 0xFF) * factor), 0, 255);
        int green = Math.clamp(Math.round(((rgb >> 8) & 0xFF) * factor), 0, 255);
        int blue = Math.clamp(Math.round((rgb & 0xFF) * factor), 0, 255);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    enum OverlayAnchor {
        TOP_LEFT("Top Left"),
        TOP_CENTER("Top Center"),
        TOP_RIGHT("Top Right"),
        MIDDLE_LEFT("Middle Left"),
        MIDDLE_CENTER("Middle Center"),
        MIDDLE_RIGHT("Middle Right"),
        BOTTOM_LEFT("Bottom Left"),
        BOTTOM_CENTER("Bottom Center"),
        BOTTOM_RIGHT("Bottom Right");

        private final String label;

        OverlayAnchor(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

}
