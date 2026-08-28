package dev.allgoals.client;

import dev.allgoals.AllGoals;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class AllGoalsSettingsScreen extends Screen {
    private final Screen parent;
    private final OverlayConfig config;
    private final OverlayRunTimer timer;
    private int audioHeadingY;
    private int preferencesHeadingY;

    AllGoalsSettingsScreen(Screen parent, OverlayConfig config, OverlayRunTimer timer) {
        super(Component.literal("All Goals Settings"));
        this.parent = parent;
        this.config = config;
        this.timer = timer;
    }

    @Override
    protected void init() {
        int left = width / 2 - 154;
        int right = width / 2 + 4;
        int y = Math.max(50, height / 2 - 70);
        audioHeadingY = y - 13;
        preferencesHeadingY = y + 70;

        addRenderableWidget(new CompletionVolumeSlider(left, y, 308, 20, config));
        addRenderableWidget(Button.builder(craftSoundLabel(), button -> {
            config.uniqueCraftSound = !config.uniqueCraftSound;
            saveAndSyncAudio();
            button.setMessage(craftSoundLabel());
        }).tooltip(Tooltip.create(Component.literal("Play progress sounds when a new unique craft is recorded.")))
                .bounds(left, y + 24, 150, 20).build());
        addRenderableWidget(Button.builder(spyglassSoundLabel(), button -> {
            config.spyglassSound = !config.spyglassSound;
            saveAndSyncAudio();
            button.setMessage(spyglassSoundLabel());
        }).tooltip(Tooltip.create(Component.literal("Play progress sounds when a new mob is observed.")))
                .bounds(right, y + 24, 150, 20).build());
        addRenderableWidget(Button.builder(victorySoundLabel(), button -> {
            config.victorySound = !config.victorySound;
            saveAndSyncAudio();
            button.setMessage(victorySoundLabel());
        }).tooltip(Tooltip.create(Component.literal("Play the celebration sound after every goal is completed.")))
                .bounds(left, y + 48, 308, 20).build());

        addRenderableWidget(Button.builder(notificationLabel(), button -> {
            config.announcementsEnabled = !config.announcementsEnabled;
            config.save();
            ClientSettingsNetworking.syncAnnouncements();
            button.setMessage(notificationLabel());
        }).tooltip(Tooltip.create(Component.literal(
                "Show or hide the green chat message when a goal is completed. This is a personal setting.")))
                .bounds(left, y + 84, 308, 20).build());

        addRenderableWidget(Button.builder(Component.literal("HUD Settings"), button -> {
            saveAndSyncAudio();
            minecraft.setScreen(new OverlaySettingsScreen(this, config, timer));
        }).tooltip(Tooltip.create(Component.literal(
                "Customize the progress display, timer, pinned goals, position, size, and colours.")))
                .bounds(left, y + 112, 308, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left, y + 144, 308, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal("Audio"), width / 2, audioHeadingY, 0xFFA0A0A0);
        graphics.centeredText(font, Component.literal("Player Preferences"),
                width / 2, preferencesHeadingY, 0xFFA0A0A0);
        graphics.text(font, Component.literal(AllGoals.RELEASE_VERSION),
                width - font.width(AllGoals.RELEASE_VERSION) - 5, height - 12, 0x60808080);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft != null && minecraft.level != null) extractTransparentBackground(graphics);
        else extractMenuBackground(graphics);
    }

    @Override
    public void onClose() {
        saveAndSyncAudio();
        minecraft.setScreen(parent);
    }

    private void saveAndSyncAudio() {
        config.save();
        ClientSettingsNetworking.syncAudio();
    }

    private Component craftSoundLabel() {
        return Component.literal("Unique Craft Sound: " + onOff(config.uniqueCraftSound));
    }

    private Component spyglassSoundLabel() {
        return Component.literal("Spyglass Sound: " + onOff(config.spyglassSound));
    }

    private Component victorySoundLabel() {
        return Component.literal("Victory Sound: " + onOff(config.victorySound));
    }

    private Component notificationLabel() {
        return Component.literal("Goal Announcements: " + onOff(config.announcementsEnabled));
    }

    private static String onOff(boolean enabled) {
        return enabled ? "On" : "Off";
    }

    private static final class CompletionVolumeSlider extends AbstractSliderButton {
        private final OverlayConfig config;

        private CompletionVolumeSlider(int x, int y, int width, int height, OverlayConfig config) {
            super(x, y, width, height, Component.empty(), config.goalCompletionVolume);
            this.config = config;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Goal Completion Volume: " + Math.round(value * 100.0) + "%"));
        }

        @Override
        protected void applyValue() {
            config.goalCompletionVolume = (float) value;
        }
    }
}
