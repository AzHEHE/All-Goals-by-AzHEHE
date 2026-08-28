package dev.allgoals.client;

import dev.allgoals.AllGoals;
import dev.allgoals.goal.GoalDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class PinnedGoalsScreen extends Screen {
    private final Screen parent;
    private final OverlayConfig config;
    private final List<Button> goalButtons = new ArrayList<>();

    private EditBox searchBox;
    private Button previousButton;
    private Button nextButton;
    private String query = "";
    private int page;
    private int pageCount = 1;
    private boolean refreshNeeded;

    PinnedGoalsScreen(Screen parent, OverlayConfig config) {
        super(Component.literal("Choose Pinned Goals"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int searchWidth = Math.min(300, width - 40);
        searchBox = addRenderableWidget(new EditBox(
                font, width / 2 - searchWidth / 2, 32, searchWidth, 20,
                Component.literal("Search goals")
        ));
        searchBox.setHint(Component.literal("Search goals..."));
        searchBox.setValue(query);
        searchBox.setResponder(value -> {
            query = value;
            page = 0;
            refreshNeeded = true;
        });

        int footerY = height - 50;
        previousButton = addRenderableWidget(Button.builder(Component.literal("< Previous"), button -> {
            if (page > 0) {
                page--;
                refreshGoalButtons();
            }
        }).bounds(width / 2 - 154, footerY, 100, 20).build());

        nextButton = addRenderableWidget(Button.builder(Component.literal("Next >"), button -> {
            if (page + 1 < pageCount) {
                page++;
                refreshGoalButtons();
            }
        }).bounds(width / 2 + 54, footerY, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20).build());

        refreshGoalButtons();
        setInitialFocus(searchBox);
    }

    @Override
    public void tick() {
        super.tick();
        if (refreshNeeded) {
            refreshNeeded = false;
            refreshGoalButtons();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal(
                "Page " + (page + 1) + "/" + pageCount + "  •  "
                        + config.pinnedGoalIds.size() + " pinned"
        ), width / 2, height - 63, 0xFFAAAAAA);
        if (goalButtons.isEmpty()) {
            graphics.centeredText(font, Component.literal("No goals match your search."),
                    width / 2, height / 2, 0xFFAAAAAA);
        }
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

    private void refreshGoalButtons() {
        for (Button button : goalButtons) removeWidget(button);
        goalButtons.clear();

        List<GoalDefinition> goals = filteredGoals();
        int rows = Math.max(1, (height - 108) / 22);
        int pageSize = rows * 2;
        pageCount = Math.max(1, (goals.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, pageCount - 1));

        int first = page * pageSize;
        int last = Math.min(goals.size(), first + pageSize);
        int columnWidth = Math.min(202, (width - 28) / 2);
        int leftX = width / 2 - columnWidth - 4;
        int rightX = width / 2 + 4;
        for (int index = first; index < last; index++) {
            GoalDefinition goal = goals.get(index);
            int localIndex = index - first;
            int column = localIndex / rows;
            int row = localIndex % rows;
            int x = column == 0 ? leftX : rightX;
            Button button = Button.builder(goalLabel(goal), pressed -> {
                config.togglePinned(goal.sourceId());
                pressed.setMessage(goalLabel(goal));
            }).tooltip(Tooltip.create(Component.literal(goal.displayName())
                    .append(Component.literal("\n" + goal.sourceId()).withStyle(ChatFormatting.DARK_GRAY))))
                    .bounds(x, 58 + row * 22, columnWidth, 20).build();
            goalButtons.add(addRenderableWidget(button));
        }

        previousButton.active = page > 0;
        nextButton.active = page + 1 < pageCount;
    }

    private List<GoalDefinition> filteredGoals() {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return AllGoals.goalCatalog().goals().stream()
                .filter(goal -> normalized.isEmpty()
                        || goal.displayName().toLowerCase(Locale.ROOT).contains(normalized)
                        || goal.sourceId().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted(Comparator.comparing(GoalDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Component goalLabel(GoalDefinition goal) {
        if (!config.isPinned(goal.sourceId())) return Component.literal(goal.displayName());
        return Component.literal("Pinned: " + goal.displayName()).withStyle(ChatFormatting.GOLD);
    }
}
