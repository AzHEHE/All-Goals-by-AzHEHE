package dev.allgoals.client;

import dev.allgoals.network.LeaderboardDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.List;

final class LeaderboardScreen extends Screen {
    private static final int ROW_HEIGHT = 36;
    private static final int PANEL_WIDTH = 236;
    private static final int REFRESH_INTERVAL_TICKS = 5 * 20;
    private static final Identifier SOCIAL_BACKGROUND =
            Identifier.withDefaultNamespace("social_interactions/background");
    private final Screen parent;
    private LeaderboardDataPayload snapshot;
    private Filter filter = Filter.BOTH;
    private Button bothButton;
    private Button partiesButton;
    private Button individualsButton;
    private List<LeaderboardDataPayload.Entry> visibleEntries = List.of();
    private int scroll;
    private int refreshTicks;

    LeaderboardScreen(Screen parent, LeaderboardDataPayload snapshot) {
        super(Component.literal("All Goals World Leaderboard"));
        this.parent = parent;
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int rowLeft = center - 110;
        int tabWidth = 73;
        bothButton = addRenderableWidget(Button.builder(Component.literal("Both"), button -> setFilter(Filter.BOTH))
                .bounds(rowLeft, 45, tabWidth, 20).build());
        partiesButton = addRenderableWidget(Button.builder(Component.literal("Parties"), button -> setFilter(Filter.PARTIES))
                .bounds(rowLeft + 74, 45, tabWidth, 20).build());
        individualsButton = addRenderableWidget(Button.builder(Component.literal("Individuals"), button -> setFilter(Filter.INDIVIDUALS))
                .bounds(rowLeft + 148, 45, tabWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center - 100, height - 26, 200, 20).build());
        refreshVisibleEntries();
        refreshButtons();
    }

    void update(LeaderboardDataPayload updated) {
        snapshot = updated;
        refreshVisibleEntries();
        clampScroll();
    }

    @Override
    public void tick() {
        super.tick();
        if (++refreshTicks >= REFRESH_INTERVAL_TICKS) {
            refreshTicks = 0;
            LeaderboardClient.refresh();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);

        int panelX = width / 2 - PANEL_WIDTH / 2;
        int top = 88;
        int bottom = 72 + windowHeight();

        if (visibleEntries.isEmpty()) {
            graphics.centeredText(font, Component.literal(emptyMessage()), width / 2,
                    top + (bottom - top) / 2 - 4, 0xFFAAAAAA);
            return;
        }

        graphics.enableScissor(panelX + 8, top, panelX + PANEL_WIDTH - 8, bottom);
        for (int index = 0; index < visibleEntries.size(); index++) {
            int rowY = top + 4 + index * ROW_HEIGHT - scroll;
            if (rowY + ROW_HEIGHT < top || rowY >= bottom) continue;
            renderEntry(graphics, visibleEntries.get(index), index + 1, panelX + 8, rowY,
                    PANEL_WIDTH - 16, mouseX, mouseY);
        }
        graphics.disableScissor();
    }

    private void renderEntry(GuiGraphicsExtractor graphics, LeaderboardDataPayload.Entry entry,
                             int rank, int x, int y, int rowWidth, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + rowWidth
                && mouseY >= y && mouseY < y + ROW_HEIGHT;
        if (hovered) graphics.fill(x, y, x + rowWidth, y + ROW_HEIGHT, 0x20FFFFFF);
        graphics.text(font, Integer.toString(rank), x + 7, y + 14, 0xFFAAAAAA);

        PlayerFaceExtractor.extractRenderState(graphics, skin(entry), x + 26, y + 6, 24);

        String progress = entry.completed() + " / " + snapshot.totalGoals();
        int progressX = x + rowWidth - font.width(progress) - 9;
        int nameWidth = Math.max(24, progressX - (x + 58) - 8);
        String displayName = font.plainSubstrByWidth(entry.name(), nameWidth);
        Component name = Component.literal(displayName).withStyle(
                entry.party() ? ChatFormatting.GOLD : ChatFormatting.WHITE
        );
        graphics.text(font, name, x + 58, y + 7, 0xFFFFFFFF);
        String detail = entry.party()
                ? "Party  •  " + entry.onlineMembers() + "/" + entry.totalMembers() + " online"
                : "Individual";
        int barWidth = Math.min(92, Math.max(44, rowWidth / 4));
        int barX = x + rowWidth - barWidth - 9;
        int detailWidth = Math.max(24, barX - (x + 58) - 5);
        graphics.text(font, font.plainSubstrByWidth(detail, detailWidth), x + 58, y + 21, 0xFF909090);

        graphics.text(font, progress, progressX, y + 7,
                entry.party() ? 0xFFFFD36A : 0xFFE0E0E0);
        int barY = y + 22;
        graphics.fill(barX, barY, barX + barWidth, barY + 5, 0xFF080808);
        int filled = snapshot.totalGoals() <= 0 ? 0
                : Math.min(barWidth, Math.round(barWidth * entry.completed() / (float) snapshot.totalGoals()));
        if (filled > 0) {
            graphics.fill(barX + 1, barY + 1, barX + filled, barY + 4,
                    entry.party() ? 0xFFFFAA00 : 0xFF18A8BD);
        }
    }

    private PlayerSkin skin(LeaderboardDataPayload.Entry entry) {
        if (minecraft != null && minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(entry.headOwner());
            if (info != null) return info.getSkin();
        }
        return DefaultPlayerSkin.get(entry.headOwner());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (super.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
        scroll -= (int) Math.round(vertical * ROW_HEIGHT);
        clampScroll();
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft != null && minecraft.level != null) extractTransparentBackground(graphics);
        else extractMenuBackground(graphics);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SOCIAL_BACKGROUND,
                width / 2 - PANEL_WIDTH / 2, 64, PANEL_WIDTH, windowHeight() + 16);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void setFilter(Filter next) {
        filter = next;
        scroll = 0;
        refreshVisibleEntries();
        refreshButtons();
    }

    private void refreshButtons() {
        bothButton.active = filter != Filter.BOTH;
        partiesButton.active = filter != Filter.PARTIES;
        individualsButton.active = filter != Filter.INDIVIDUALS;
    }

    private void refreshVisibleEntries() {
        visibleEntries = snapshot.entries().stream().filter(entry -> switch (filter) {
            case BOTH -> true;
            case PARTIES -> entry.party();
            case INDIVIDUALS -> !entry.party();
        }).toList();
    }

    private void clampScroll() {
        int viewportHeight = Math.max(0, windowHeight() - 16);
        int maximum = Math.max(0, visibleEntries.size() * ROW_HEIGHT + 8 - viewportHeight);
        scroll = Math.max(0, Math.min(scroll, maximum));
    }

    private String emptyMessage() {
        return switch (filter) {
            case BOTH -> "No players are currently connected.";
            case PARTIES -> "No parties are currently online.";
            case INDIVIDUALS -> "No individual players are currently online.";
        };
    }

    private enum Filter {
        BOTH,
        PARTIES,
        INDIVIDUALS
    }

    private int windowHeight() {
        return Math.max(52, height - 144);
    }
}
