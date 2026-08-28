package dev.allgoals.client;

import dev.allgoals.AllGoals;
import dev.allgoals.goal.GoalDefinition;
import dev.allgoals.progress.PlayerGoalProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

final class ClassicColorScreen extends Screen {
    private final Screen parent;
    private final OverlayConfig config;
    private final OverlayRunTimer timer;

    private ColorTarget selected = ColorTarget.BORDER;
    private Button borderButton;
    private Button barButton;
    private Button iconsButton;
    private Button timerButton;
    private RgbSlider redSlider;
    private RgbSlider greenSlider;
    private RgbSlider blueSlider;
    private int controlsY;

    ClassicColorScreen(Screen parent, OverlayConfig config, OverlayRunTimer timer) {
        super(Component.literal("Adjust Classic Colours"));
        this.parent = parent;
        this.config = config;
        this.timer = timer;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int selectorLeft = center - 150;
        controlsY = Math.max(106, height - 126);

        borderButton = addRenderableWidget(Button.builder(Component.empty(), button -> select(ColorTarget.BORDER))
                .bounds(selectorLeft, controlsY, 72, 20).build());
        barButton = addRenderableWidget(Button.builder(Component.empty(), button -> select(ColorTarget.BAR))
                .bounds(selectorLeft + 76, controlsY, 72, 20).build());
        iconsButton = addRenderableWidget(Button.builder(Component.empty(), button -> select(ColorTarget.ICONS))
                .bounds(selectorLeft + 152, controlsY, 72, 20).build());
        timerButton = addRenderableWidget(Button.builder(Component.empty(), button -> select(ColorTarget.TIMER))
                .bounds(selectorLeft + 228, controlsY, 72, 20).build());

        redSlider = addRenderableWidget(new RgbSlider(center - 110, controlsY + 24, 220,
                "Red", channel(selected.rgb(config), 16), value -> setChannel(16, value)));
        greenSlider = addRenderableWidget(new RgbSlider(center - 110, controlsY + 48, 220,
                "Green", channel(selected.rgb(config), 8), value -> setChannel(8, value)));
        blueSlider = addRenderableWidget(new RgbSlider(center - 110, controlsY + 72, 220,
                "Blue", channel(selected.rgb(config), 0), value -> setChannel(0, value)));

        addRenderableWidget(Button.builder(Component.literal("Reset Colours"), button -> {
            config.resetClassicColors();
            refreshControls();
        }).bounds(center - 110, controlsY + 96, 106, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center + 4, controlsY + 96, 106, 20).build());

        refreshControls();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal("Live Preview"), width / 2, 24, 0xFFAAAAAA);

        graphics.enableScissor(0, 34, width, Math.max(35, controlsY - 10));
        AllGoalsOverlay.renderAt(graphics, width / 2 - 82, 36, 1.0F,
                previewProgress(), timer.elapsedMillis(), config);
        graphics.disableScissor();

        int rgb = selected.rgb(config);
        String hex = String.format(Locale.ROOT, "%06X", rgb);
        graphics.centeredText(font, Component.literal(selected.label + "  #" + hex),
                width / 2, controlsY - 12, 0xFFFFFFFF);

        int swatchLeft = width / 2 + 116;
        graphics.fill(swatchLeft, controlsY + 24, swatchLeft + 32, controlsY + 92, 0xFF111111);
        graphics.outline(swatchLeft, controlsY + 24, 32, 68, 0xFFFFFFFF);
        graphics.fill(swatchLeft + 2, controlsY + 26, swatchLeft + 30, controlsY + 90,
                0xFF000000 | rgb);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft != null && minecraft.level != null) extractTransparentBackground(graphics);
        else extractMenuBackground(graphics);
    }

    @Override
    public void onClose() {
        config.save();
        minecraft.setScreen(parent);
    }

    private PlayerGoalProgress previewProgress() {
        int targetCompleted = AllGoals.goalCatalog().goalCount() / 2;
        Set<String> completed = new LinkedHashSet<>();
        Set<String> pinned = new LinkedHashSet<>(config.pinnedGoalIds);

        for (int index = 0; index < config.pinnedGoalIds.size(); index++) {
            String goalId = config.pinnedGoalIds.get(index);
            if (index % 2 == 0 && AllGoals.goalCatalog().findSource(goalId).isPresent()) {
                completed.add(goalId);
            }
        }
        for (GoalDefinition goal : AllGoals.goalCatalog().goals()) {
            if (completed.size() >= targetCompleted) break;
            if (!pinned.contains(goal.sourceId())) completed.add(goal.sourceId());
        }
        return new PlayerGoalProgress(completed, Map.of(), Map.of());
    }

    private void select(ColorTarget target) {
        selected = target;
        refreshControls();
    }

    private void refreshControls() {
        if (borderButton == null) return;
        borderButton.setMessage(selectorLabel(ColorTarget.BORDER));
        barButton.setMessage(selectorLabel(ColorTarget.BAR));
        iconsButton.setMessage(selectorLabel(ColorTarget.ICONS));
        timerButton.setMessage(selectorLabel(ColorTarget.TIMER));
        int rgb = selected.rgb(config);
        redSlider.setChannelValue(channel(rgb, 16));
        greenSlider.setChannelValue(channel(rgb, 8));
        blueSlider.setChannelValue(channel(rgb, 0));
    }

    private Component selectorLabel(ColorTarget target) {
        return Component.literal((selected == target ? "> " : "") + target.shortLabel
                + (selected == target ? " <" : ""));
    }

    private void setChannel(int shift, int value) {
        int mask = 0xFF << shift;
        int rgb = selected.rgb(config);
        selected.setRgb(config, rgb & ~mask | Math.clamp(value, 0, 255) << shift);
    }

    private static int channel(int rgb, int shift) {
        return rgb >> shift & 0xFF;
    }

    private enum ColorTarget {
        BORDER("Border", "Border") {
            @Override int rgb(OverlayConfig config) { return config.classicBorderRgb; }
            @Override void setRgb(OverlayConfig config, int rgb) { config.classicBorderRgb = rgb; }
        },
        BAR("Progress Bar", "Bar") {
            @Override int rgb(OverlayConfig config) { return config.classicBarRgb; }
            @Override void setRgb(OverlayConfig config, int rgb) { config.classicBarRgb = rgb; }
        },
        ICONS("Icon Frames", "Icons") {
            @Override int rgb(OverlayConfig config) { return config.classicIconRgb; }
            @Override void setRgb(OverlayConfig config, int rgb) { config.classicIconRgb = rgb; }
        },
        TIMER("Timer Text", "Timer") {
            @Override int rgb(OverlayConfig config) { return config.classicTimerRgb; }
            @Override void setRgb(OverlayConfig config, int rgb) { config.classicTimerRgb = rgb; }
        };

        private final String label;
        private final String shortLabel;

        ColorTarget(String label, String shortLabel) {
            this.label = label;
            this.shortLabel = shortLabel;
        }

        abstract int rgb(OverlayConfig config);
        abstract void setRgb(OverlayConfig config, int rgb);
    }

    private static final class RgbSlider extends AbstractSliderButton {
        private final String channelName;
        private final IntConsumer onChange;

        private RgbSlider(int x, int y, int width, String channelName,
                          int initialValue, IntConsumer onChange) {
            super(x, y, width, 20, Component.empty(), initialValue / 255.0D);
            this.channelName = channelName;
            this.onChange = onChange;
            updateMessage();
        }

        void setChannelValue(int channelValue) {
            value = Math.clamp(channelValue, 0, 255) / 255.0D;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(channelName + ": " + channelValue()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(channelValue());
        }

        private int channelValue() {
            return Math.clamp((int) Math.round(value * 255.0D), 0, 255);
        }
    }
}
