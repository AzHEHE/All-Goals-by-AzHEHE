package dev.allgoals.client;

import dev.allgoals.AllGoals;
import dev.allgoals.goal.GoalDefinition;
import dev.allgoals.goal.GoalIcon;
import dev.allgoals.progress.AllGoalsAttachments;
import dev.allgoals.progress.PlayerGoalProgress;
import dev.allgoals.world.RunMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.List;

final class AllGoalsOverlay {
    private static final int WIDTH = 164;
    private static final int BAR_X = 2;
    private static final int BAR_WIDTH = 160;
    private static final int ICON_STEP = 24;
    private static final int MARGIN = 8;

    private final OverlayConfig config;
    private final OverlayRunTimer timer;
    private PlayerGoalProgress countedProgress;
    private int countedCompleted;
    private Component progressText = Component.empty();
    private long displayedTimerSecond = Long.MIN_VALUE;
    private Component timerText = Component.empty();

    AllGoalsOverlay(OverlayConfig config, OverlayRunTimer timer) {
        this.config = config;
        this.timer = timer;
    }

    void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!config.visible || client.player == null || client.level == null) return;
        if (client.player.getAttachedOrElse(AllGoalsAttachments.RUN_MODE, RunMode.ALL_GOALS) == RunMode.NONE) return;
        if (client.debugEntries.isOverlayVisible()) return;
        if (client.screen instanceof GoalsScreen
                || client.screen instanceof AllGoalsSettingsScreen
                || client.screen instanceof OverlaySettingsScreen
                || client.screen instanceof ClassicColorScreen
                || client.screen instanceof PinnedGoalsScreen) return;

        PlayerGoalProgress progress = client.player.getAttachedOrElse(
                AllGoalsAttachments.PLAYER_PROGRESS, PlayerGoalProgress.empty()
        );
        if (countedProgress != progress) {
            countedProgress = progress;
            countedCompleted = AllGoals.goalCatalog().completedCount(progress);
            progressText = Component.literal(countedCompleted + "/" + AllGoals.goalCatalog().goalCount());
        }
        long elapsedMillis = timer.elapsedMillis();
        long timerSecond = elapsedMillis / 1000L;
        if (config.timerEnabled && displayedTimerSecond != timerSecond) {
            displayedTimerSecond = timerSecond;
            timerText = Component.literal(formatTimer(elapsedMillis));
        }
        int height = contentHeight(config);
        float scale = config.scale();
        int scaledWidth = (int) Math.ceil(WIDTH * scale);
        int scaledHeight = (int) Math.ceil(height * scale);
        int x = anchorX(config.anchor, graphics.guiWidth(), scaledWidth);
        int y = anchorY(config.anchor, graphics.guiHeight(), scaledHeight);
        renderAt(graphics, x, y, scale, progress, config, countedCompleted, timerText, progressText);
    }

    static void renderAt(GuiGraphicsExtractor graphics, int x, int y, float scale,
                         PlayerGoalProgress progress, long elapsedMillis, OverlayConfig config) {
        int completed = AllGoals.goalCatalog().completedCount(progress);
        renderAt(graphics, x, y, scale, progress, config, completed,
                Component.literal(formatTimer(elapsedMillis)),
                Component.literal(completed + "/" + AllGoals.goalCatalog().goalCount()));
    }

    private static void renderAt(GuiGraphicsExtractor graphics, int x, int y, float scale,
                                 PlayerGoalProgress progress, OverlayConfig config, int completed,
                                 Component timerText, Component progressText) {
        Font font = Minecraft.getInstance().font;
        int total = AllGoals.goalCatalog().goalCount();
        int barY = barY(config);

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        if (config.timerEnabled) {
            graphics.centeredText(font, timerText, WIDTH / 2, 0, config.classicTimerColor());
        }
        graphics.centeredText(font, progressText,
                WIDTH / 2, config.timerEnabled ? 11 : 0, 0xFFFFFFFF);
        drawProgressBar(graphics, config, completed, total, barY);
        if (config.pinnedGoalsVisible) drawPinnedGoals(graphics, progress, config, barY + 15);
        graphics.pose().popMatrix();
    }

    static int contentHeight(OverlayConfig config) {
        int barY = barY(config);
        if (!config.pinnedGoalsVisible || config.pinnedGoalIds.isEmpty()) return barY + 10;
        int rows = (config.pinnedGoalIds.size() + config.iconsPerRow - 1) / config.iconsPerRow;
        return barY + 15 + rows * ICON_STEP - 2;
    }

    static int width() {
        return WIDTH;
    }

    private static int barY(OverlayConfig config) {
        return config.timerEnabled ? 23 : 12;
    }

    private static void drawProgressBar(GuiGraphicsExtractor graphics, OverlayConfig config,
                                        int completed, int total, int y) {
        graphics.fill(BAR_X + 1, y + 1, BAR_X + BAR_WIDTH + 1, y + 11, 0x90000000);
        graphics.fill(BAR_X, y, BAR_X + BAR_WIDTH, y + 10, 0xFF17141F);
        graphics.outline(BAR_X, y, BAR_WIDTH, 10, config.classicBorderColor());
        graphics.fill(BAR_X + 2, y + 2, BAR_X + BAR_WIDTH - 2, y + 8, 0xE0383445);
        int fillWidth = total == 0 ? 0 : (int) Math.round(156 * (completed / (double) total));
        if (fillWidth > 0) {
            graphics.fillGradient(BAR_X + 2, y + 2, BAR_X + 2 + fillWidth, y + 8,
                    config.classicBarTopColor(), config.classicBarBottomColor());
            graphics.fill(BAR_X + 2, y + 2, BAR_X + 2 + fillWidth, y + 3, 0x90FFFFFF);
        }
    }

    private static void drawPinnedGoals(GuiGraphicsExtractor graphics, PlayerGoalProgress progress,
                                        OverlayConfig config, int iconStartY) {
        List<GoalDefinition> pinned = config.pinnedGoals();
        int perRow = Math.max(1, config.iconsPerRow);
        long nowMillis = System.currentTimeMillis();
        for (int index = 0; index < pinned.size(); index++) {
            int row = index / perRow;
            int column = index % perRow;
            int rowStart = row * perRow;
            int rowCount = Math.min(perRow, pinned.size() - rowStart);
            int rowWidth = rowCount * ICON_STEP - 2;
            int iconX = (WIDTH - rowWidth) / 2 + column * ICON_STEP;
            int iconY = iconStartY + row * ICON_STEP;
            drawPinnedGoal(graphics, pinned.get(index), progress, iconX, iconY, config, nowMillis);
        }
    }

    private static void drawPinnedGoal(GuiGraphicsExtractor graphics, GoalDefinition goal,
                                       PlayerGoalProgress progress, int x, int y,
                                       OverlayConfig config, long nowMillis) {
        boolean complete = progress.isComplete(goal.sourceId());
        int frameColor = complete ? config.classicCompleteIconColor() : config.classicIncompleteIconColor();
        graphics.fill(x + 1, y + 1, x + 23, y + 23, 0x90000000);
        graphics.fill(x, y, x + 22, y + 22, frameColor);
        graphics.fill(x + 1, y + 1, x + 21, y + 21, 0xE0201E26);

        GoalIcon icon = iconFor(goal, nowMillis);
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon.texture(), x + 3, y + 3,
                0.0F, 0.0F, 16, 16, icon.width(), icon.height(), icon.width(), icon.height());
        if (complete) {
            graphics.fill(x + 1, y + 1, x + 21, y + 21, 0x80000000);
            PixelCheckmark.draw(graphics, x + 6, y + 7, config.classicIncompleteIconColor(), true);
        }
    }

    private static GoalIcon iconFor(GoalDefinition goal, long nowMillis) {
        if (!goal.hasRotatingIcon()) return goal.icons().getFirst();
        long period = Math.max(250L, goal.rotationPeriodMillis());
        int frame = (int) ((nowMillis / period) % goal.icons().size());
        return goal.icons().get(frame);
    }

    private static int anchorX(OverlayConfig.OverlayAnchor anchor, int screenWidth, int overlayWidth) {
        return switch (anchor) {
            case TOP_CENTER, MIDDLE_CENTER, BOTTOM_CENTER -> (screenWidth - overlayWidth) / 2;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> screenWidth - overlayWidth - MARGIN;
            default -> MARGIN;
        };
    }

    private static int anchorY(OverlayConfig.OverlayAnchor anchor, int screenHeight, int overlayHeight) {
        return switch (anchor) {
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT -> (screenHeight - overlayHeight) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - overlayHeight - MARGIN;
            default -> MARGIN;
        };
    }

    static String formatTimer(long elapsedMillis) {
        long totalSeconds = Math.max(0L, elapsedMillis) / 1000L;
        long seconds = totalSeconds % 60L;
        long minutes = totalSeconds / 60L % 60L;
        long hours = totalSeconds / 3600L;
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

}
