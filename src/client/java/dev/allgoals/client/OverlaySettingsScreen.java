package dev.allgoals.client;

import dev.allgoals.progress.AllGoalsAttachments;
import dev.allgoals.progress.PlayerGoalProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class OverlaySettingsScreen extends Screen {
    private final Screen parent;
    private final OverlayConfig config;
    private final OverlayRunTimer timer;

    private Button visibilityButton;
    private Button positionButton;
    private Button timerVisibilityButton;
    private Button pinnedVisibilityButton;
    private Button scaleButton;
    private Button iconsPerRowButton;
    private Button pinnedGoalsButton;

    OverlaySettingsScreen(Screen parent, OverlayConfig config, OverlayRunTimer timer) {
        super(Component.literal("HUD Settings"));
        this.parent = parent;
        this.config = config;
        this.timer = timer;
    }

    @Override
    protected void init() {
        int left = width / 2 - 154;
        int right = width / 2 + 4;
        int controlY = Math.max(90, height - 128);

        visibilityButton = addRenderableWidget(Button.builder(visibilityLabel(), button -> {
            config.visible = !config.visible;
            config.save();
            button.setMessage(visibilityLabel());
        }).tooltip(Tooltip.create(Component.literal("Show or hide the complete All Goals HUD.")))
                .bounds(left, controlY, 150, 20).build());

        positionButton = addRenderableWidget(Button.builder(positionLabel(), button -> {
            config.cycleAnchor();
            button.setMessage(positionLabel());
        }).tooltip(Tooltip.create(Component.literal("Choose which screen edge or corner anchors the HUD.")))
                .bounds(right, controlY, 150, 20).build());

        timerVisibilityButton = addRenderableWidget(Button.builder(timerVisibilityLabel(), button -> {
            config.timerEnabled = !config.timerEnabled;
            config.save();
            button.setMessage(timerVisibilityLabel());
        }).tooltip(Tooltip.create(Component.literal("Show or hide the active run timer.")))
                .bounds(left, controlY + 24, 150, 20).build());

        pinnedVisibilityButton = addRenderableWidget(Button.builder(pinnedVisibilityLabel(), button -> {
            config.pinnedGoalsVisible = !config.pinnedGoalsVisible;
            config.save();
            button.setMessage(pinnedVisibilityLabel());
        }).tooltip(Tooltip.create(Component.literal("Show or hide pinned goal icons below the progress bar.")))
                .bounds(right, controlY + 24, 150, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Adjust Colours"), button ->
                        minecraft.setScreen(new ClassicColorScreen(this, config, timer))
        ).tooltip(Tooltip.create(Component.literal("Customize the Classic HUD palette.")))
                .bounds(left, controlY + 48, 150, 20).build());

        scaleButton = addRenderableWidget(Button.builder(scaleLabel(), button -> {
            config.cycleScale();
            button.setMessage(scaleLabel());
        }).tooltip(Tooltip.create(Component.literal("Change the HUD's overall display size.")))
                .bounds(right, controlY + 48, 150, 20).build());

        iconsPerRowButton = addRenderableWidget(Button.builder(iconsPerRowLabel(), button -> {
            config.cycleIconsPerRow();
            button.setMessage(iconsPerRowLabel());
        }).tooltip(Tooltip.create(Component.literal("Choose how many pinned icons appear on each row.")))
                .bounds(left, controlY + 72, 150, 20).build());

        pinnedGoalsButton = addRenderableWidget(Button.builder(pinnedGoalsLabel(), button ->
                minecraft.setScreen(new PinnedGoalsScreen(this, config))
        ).tooltip(Tooltip.create(Component.literal("Search the catalog and choose which goals appear on the HUD.")))
                .bounds(right, controlY + 72, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Restore HUD Defaults"), button -> {
            config.resetLayout();
            refreshLabels();
        }).tooltip(Tooltip.create(Component.literal("Restore the default layout, pins, size, and Classic colours.")))
                .bounds(left, controlY + 96, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(right, controlY + 96, 150, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal("Preview"), width / 2, 26, 0xFFAAAAAA);

        int controlY = Math.max(90, height - 128);
        int previewBottom = Math.max(84, controlY - 8);
        graphics.enableScissor(0, 38, width, previewBottom);
        PlayerGoalProgress progress = currentProgress();
        float availableHeight = Math.max(1, previewBottom - 42);
        float previewScale = Math.min(config.scale(),
                availableHeight / AllGoalsOverlay.contentHeight(config));
        int previewWidth = (int) Math.ceil(AllGoalsOverlay.width() * previewScale);
        AllGoalsOverlay.renderAt(graphics, (width - previewWidth) / 2, 40,
                previewScale, progress, timer.elapsedMillis(), config);
        graphics.disableScissor();
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

    @Override
    public void added() {
        super.added();
        refreshLabels();
    }

    private PlayerGoalProgress currentProgress() {
        if (minecraft == null || minecraft.player == null) return PlayerGoalProgress.empty();
        return minecraft.player.getAttachedOrElse(
                AllGoalsAttachments.PLAYER_PROGRESS, PlayerGoalProgress.empty()
        );
    }

    private void refreshLabels() {
        if (visibilityButton == null) return;
        visibilityButton.setMessage(visibilityLabel());
        positionButton.setMessage(positionLabel());
        timerVisibilityButton.setMessage(timerVisibilityLabel());
        pinnedVisibilityButton.setMessage(pinnedVisibilityLabel());
        scaleButton.setMessage(scaleLabel());
        iconsPerRowButton.setMessage(iconsPerRowLabel());
        pinnedGoalsButton.setMessage(pinnedGoalsLabel());
    }

    private Component visibilityLabel() {
        return Component.literal("HUD: " + (config.visible ? "Shown" : "Hidden"));
    }

    private Component positionLabel() {
        return Component.literal("Position: " + config.anchor.label());
    }

    private Component timerVisibilityLabel() {
        return Component.literal("Timer: " + (config.timerEnabled ? "Shown" : "Hidden"));
    }

    private Component pinnedVisibilityLabel() {
        return Component.literal("Pinned Goals: " + (config.pinnedGoalsVisible ? "Shown" : "Hidden"));
    }

    private Component scaleLabel() {
        return Component.literal("Size: " + config.scaleLabel());
    }

    private Component iconsPerRowLabel() {
        return Component.literal("Icons per Row: " + config.iconsPerRow);
    }

    private Component pinnedGoalsLabel() {
        return Component.literal("Edit Pinned Goals (" + config.pinnedGoalIds.size() + ")");
    }
}
