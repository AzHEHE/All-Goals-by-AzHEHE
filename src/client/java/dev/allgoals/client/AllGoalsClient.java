package dev.allgoals.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.allgoals.progress.AllGoalsAttachments;
import dev.allgoals.world.RunMode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import static dev.allgoals.AllGoals.MOD_ID;

public final class AllGoalsClient implements ClientModInitializer {
    private static final Identifier OVERLAY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "progress_overlay");
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "controls")
    );
    private static final KeyMapping OPEN_GOALS = new KeyMapping(
            "key.all_goals.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );
    private static final KeyMapping OPEN_LEADERBOARD = new KeyMapping(
            "key.all_goals.leaderboard",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY
    );

    @Override
    public void onInitializeClient() {
        LeaderboardClient.initialize();
        OverlayConfig overlayConfig = OverlayConfig.load();
        ClientSettingsNetworking.initialize(overlayConfig);
        OverlayRunTimer runTimer = new OverlayRunTimer(overlayConfig);
        AllGoalsOverlay overlay = new AllGoalsOverlay(overlayConfig, runTimer);

        KeyMappingHelper.registerKeyMapping(OPEN_GOALS);
        KeyMappingHelper.registerKeyMapping(OPEN_LEADERBOARD);
        HudElementRegistry.addFirst(OVERLAY_ID, (graphics, deltaTracker) -> overlay.render(graphics));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> runTimer.stop());
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                addSettingsShortcut(client, screen, overlayConfig, runTimer);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            runTimer.tick(client);
            while (OPEN_GOALS.consumeClick()) {
                if (client.player == null) continue;
                if (client.player.getAttachedOrElse(AllGoalsAttachments.RUN_MODE, RunMode.ALL_GOALS)
                        == RunMode.NONE) {
                    client.gui.getChat().addClientSystemMessage(Component.literal(
                            "This world is not using All Goals mode."));
                    continue;
                }
                if (client.screen instanceof GoalsScreen) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new GoalsScreen());
                }
            }
            while (OPEN_LEADERBOARD.consumeClick()) {
                if (client.player == null) continue;
                if (client.screen instanceof LeaderboardScreen) {
                    client.setScreen(null);
                } else {
                    LeaderboardClient.open(client.screen);
                }
            }
        });
    }

    private static void addSettingsShortcut(Minecraft client, Screen screen,
                                            OverlayConfig config, OverlayRunTimer timer) {
        String anchorLabel = Component.translatable(
                screen instanceof TitleScreen ? "menu.singleplayer" : "menu.returnToGame"
        ).getString();
        int x = 6;
        int y = 6;
        for (AbstractWidget widget : Screens.getWidgets(screen)) {
            if (!widget.getMessage().getString().equals(anchorLabel)) continue;
            x = widget.getX() - AllGoalsLogoButton.SIZE - 4;
            y = widget.getY() + (widget.getHeight() - AllGoalsLogoButton.SIZE) / 2;
            break;
        }
        Screens.getWidgets(screen).add(new AllGoalsLogoButton(x, y, button ->
                client.setScreen(new AllGoalsSettingsScreen(screen, config, timer))));
    }

    static boolean isOpenKey(KeyEvent event) {
        return OPEN_GOALS.matches(event);
    }
}
