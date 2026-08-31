package dev.allgoals.client;

import dev.allgoals.AllGoals;
import dev.allgoals.goal.GoalDefinition;
import dev.allgoals.goal.GoalIcon;
import dev.allgoals.goal.VariantGoalIds;
import dev.allgoals.progress.AllGoalsAttachments;
import dev.allgoals.progress.PlayerGoalProgress;
import dev.allgoals.tracking.IceGoalProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Advancement-style All Goals board. A visual node may group several colour
 * variants, but every variant remains an independently completed goal.
 */
public final class GoalsScreen extends Screen {
    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 140;
    private static final int INSIDE_X = 9;
    private static final int INSIDE_Y = 18;
    private static final int INSIDE_WIDTH = 234;
    private static final int INSIDE_HEIGHT = 113;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_SPACING = 4;
    private static final int TOP_TAB_COUNT = 8;
    private static final int NODE_SIZE = 26;
    private static final int GRID_COLUMNS = 6;
    private static final int NODE_X_SPACING = 42;
    private static final int NODE_Y_SPACING = 30;
    private static final int TITLE_LINE_HEIGHT = 10;
    private static final int STATUS_LINE_HEIGHT = 11;
    private static final double SCROLL_SPEED = 16.0;
    private static final Component AUTHOR_SIGNATURE = Component.literal("-AzHEHE");

    private static final List<InformationSection> INFORMATION_SECTIONS = List.of(
            new InformationSection("About All Goals", List.of(
                    "All Goals is based on Draftout, a Minecraft bingo and lockout mod. The aim of All Goals is to complete every available Draftout goal in a single Minecraft world, with the mod automatically tracking your progress throughout the run."
            )),
            new InformationSection("Credits and Attribution", List.of(
                    "Full credit to 7rowl and Marin, the original creators of Draftout. The goal concepts, icons, and other Draftout assets used by All Goals are sourced from Draftout and are not my original work. All credit for this material belongs to the Draftout creators and team.",
                    "All Goals is an unofficial, standalone project. It is not an official release by, or a replacement for, the Draftout team.",
                    "Official Draftout website: https://draftoutmc.com/"
            )),
            new InformationSection("Included Goals", List.of(
                    "All Goals includes goals from the current Draftout goal pool, as well as goals that have been removed since Draftout's public beta release.",
                    "This mod will aim to remain up to date with new goals introduced by the Draftout team. As a result, the available goals may be added, removed, or changed between versions of All Goals."
            )),
            new InformationSection("Run Version Requirements", List.of(
                    "To keep runs clear and comparable, every recorded run should state which version of All Goals was used. If possible, players should also state the version in which the run was completed.",
                    "This release is All Goals " + AllGoals.RELEASE_VERSION + "."
            ))
    );

    private static final Identifier WINDOW_TEXTURE = minecraft("textures/gui/advancements/window.png");
    private static final Identifier TITLE_BOX = minecraft("advancements/title_box");
    private static final Identifier BOX_OBTAINED = minecraft("advancements/box_obtained");
    private static final Identifier BOX_UNOBTAINED = minecraft("advancements/box_unobtained");
    private static final Identifier TASK_OBTAINED = minecraft("advancements/task_frame_obtained");
    private static final Identifier TASK_UNOBTAINED = minecraft("advancements/task_frame_unobtained");
    private static final Identifier CHALLENGE_OBTAINED = minecraft("advancements/challenge_frame_obtained");
    private static final Identifier CHALLENGE_UNOBTAINED = minecraft("advancements/challenge_frame_unobtained");

    private static final List<String> MILESTONE_IDS = List.of(
            "OBTAIN_WITHER_SKELETON_SKULL", "KILL_WARDEN", "OBTAIN_SPONGE",
            "GET_THE_CITY_AT_THE_END_OF_THE_GAME_ADVANCEMENT", "REACH_HEIGHT_LIMIT",
            "OBTAIN_RESIN_BRICK_WALL", "OBTAIN_RESIN_BLOCK", "VISIT_ALL_NETHER_BIOMES",
            "SPY_25_UNIQUE_MOBS", "EAT_25_UNIQUE_FOOD", "BREED_8_UNIQUE_ANIMALS",
            "KILL_15_UNIQUE_HOSTILE_MOBS", "COMPOST_7_UNIQUE_FOODS", "CRAFT_100_UNIQUE_ITEMS",
            "EAT_ALL_SOUPS", "KILL_30_UNDEAD_MOBS", "KILL_20_ARTHROPOD_MOBS",
            "KILL_ALL_RAID_MOBS", "VISIT_ALL_CAVE_BIOMES", "TURN_GHAST_UPSIDE_DOWN"
    );

    private final Map<GoalCategory, List<GoalNode>> layouts = new EnumMap<>(GoalCategory.class);
    private final Map<GoalCategory, ViewState> views = new EnumMap<>(GoalCategory.class);
    private int cachedInformationContentHeight = -1;
    private GoalCategory selected = GoalCategory.MILESTONES;
    private boolean scrolling;

    public GoalsScreen() {
        super(Component.literal("All Goals"));
        List<GoalDefinition> allGoals = AllGoals.goalCatalog().goals();
        for (GoalCategory category : GoalCategory.values()) {
            List<GoalNode> layout = switch (category) {
                case MILESTONES -> createMilestoneLayout(allGoals);
                case DYES -> createDyeLayout(allGoals);
                case BREEDS -> createBreedLayout(goalsForCategory(allGoals, category));
                case INFO -> List.of();
                default -> createGridLayout(goalsForCategory(allGoals, category), category);
            };
            layouts.put(category, layout);
            views.put(category, new ViewState());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int windowX = (width - WINDOW_WIDTH) / 2;
        int windowY = (height - WINDOW_HEIGHT) / 2;
        PlayerGoalProgress progress = currentProgress();
        List<GoalNode> nodes = layouts.get(selected);
        ViewState view = views.get(selected);
        if (selected == GoalCategory.INFO) prepareInformationView(view);
        else prepareView(view, nodes);

        graphics.nextStratum();
        drawInside(graphics, windowX + INSIDE_X, windowY + INSIDE_Y, nodes, view, progress);
        if (selected == GoalCategory.INFO) {
            drawInformation(graphics, windowX + INSIDE_X, windowY + INSIDE_Y, view);
        }
        graphics.nextStratum();
        graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, windowX, windowY,
                0.0F, 0.0F, WINDOW_WIDTH, WINDOW_HEIGHT, 256, 256);
        drawTabs(graphics, windowX, windowY, mouseX, mouseY);

        int completed = nodes.stream().mapToInt(node -> node.completedCount(progress)).sum();
        int total = nodes.stream().mapToInt(GoalNode::goalCount).sum();
        int completedAll = AllGoals.goalCatalog().completedCount(progress);
        Component heading = selected == GoalCategory.INFO
                ? Component.literal("Information")
                : Component.literal(selected.label + "  " + completed + "/" + total);
        Component allCounter = Component.literal("All " + completedAll + "/" + AllGoals.goalCatalog().goalCount());
        graphics.text(font, heading, windowX + 8, windowY + 6, 0xFF404040, false);
        graphics.text(font, allCounter, windowX + WINDOW_WIDTH - 8 - font.width(allCounter),
                windowY + 6, 0xFF404040, false);
        Component version = Component.literal(AllGoals.RELEASE_VERSION);
        graphics.text(font, version, windowX + WINDOW_WIDTH - 11 - font.width(version),
                windowY + WINDOW_HEIGHT - 18, 0x99606060, false);

        GoalNode hovered = selected == GoalCategory.INFO
                ? null : hoveredNode(nodes, view, windowX, windowY, mouseX, mouseY);
        if (hovered != null) {
            graphics.nextStratum();
            drawAdvancementTooltip(graphics, hovered, view, progress, windowX, windowY);
        }
    }

    private void drawInside(GuiGraphicsExtractor graphics, int x, int y, List<GoalNode> nodes,
                            ViewState view, PlayerGoalProgress progress) {
        graphics.enableScissor(x, y, x + INSIDE_WIDTH, y + INSIDE_HEIGHT);
        int scrollX = (int) Math.floor(view.x);
        int scrollY = (int) Math.floor(view.y);
        int tileOffsetX = scrollX % 16;
        int tileOffsetY = scrollY % 16;
        for (int column = -1; column <= 15; column++) {
            for (int row = -1; row <= 8; row++) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, selected.background,
                        x + tileOffsetX + column * 16, y + tileOffsetY + row * 16,
                        0.0F, 0.0F, 16, 16, 16, 16);
            }
        }
        drawLayoutSeparators(graphics, x, y, scrollX, scrollY);
        for (GoalNode node : nodes) {
            if (!nodeVisible(node, scrollX, scrollY)) continue;
            int nodeX = x + scrollX + node.x();
            int nodeY = y + scrollY + node.y();
            boolean complete = node.isComplete(progress);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frameFor(complete),
                    nodeX + 3, nodeY, NODE_SIZE, NODE_SIZE);
            drawGoalIcon(graphics, iconFor(node), nodeX + 8, nodeY + 5, 16);
            if (!node.isGroup()) drawGoalAmount(graphics, node.primary(), nodeX + 8, nodeY + 5);
        }
        graphics.disableScissor();
    }

    private void drawInformation(GuiGraphicsExtractor graphics, int x, int y, ViewState view) {
        graphics.enableScissor(x, y, x + INSIDE_WIDTH, y + INSIDE_HEIGHT);
        int contentY = y + (int) Math.floor(view.y) + 7;
        for (InformationSection section : INFORMATION_SECTIONS) {
            graphics.text(font, Component.literal(section.heading()), x + 8, contentY, 0xFFFFAA00, false);
            contentY += TITLE_LINE_HEIGHT + 5;
            for (String paragraph : section.paragraphs()) {
                contentY = drawInformationParagraph(graphics, paragraph, x + 8, contentY, 0xFFE0E0E0);
            }
            contentY += 3;
        }
        graphics.text(font, AUTHOR_SIGNATURE,
                x + INSIDE_WIDTH - 8 - font.width(AUTHOR_SIGNATURE), contentY, 0xFFAAAAAA, false);
        graphics.disableScissor();
    }

    private int drawInformationParagraph(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), INSIDE_WIDTH - 16);
        for (FormattedCharSequence line : lines) {
            graphics.text(font, line, x, y, color);
            y += STATUS_LINE_HEIGHT;
        }
        return y + 5;
    }

    private void drawLayoutSeparators(GuiGraphicsExtractor graphics, int x, int y, int scrollX, int scrollY) {
        if (selected == GoalCategory.MILESTONES) {
            int lineY = y + scrollY + 72;
            graphics.fill(x + scrollX + 5, lineY, x + scrollX + 197, lineY + 1, 0xFF202020);
            graphics.fill(x + scrollX + 5, lineY + 1, x + scrollX + 197, lineY + 2, 0x70FFFFFF);
        }
    }

    private static boolean nodeVisible(GoalNode node, int scrollX, int scrollY) {
        int x = scrollX + node.x() + 3;
        int y = scrollY + node.y();
        return x + NODE_SIZE >= 0 && x <= INSIDE_WIDTH && y + NODE_SIZE >= 0 && y <= INSIDE_HEIGHT;
    }

    private Identifier frameFor(boolean complete) {
        if (selected == GoalCategory.MILESTONES || selected == GoalCategory.CHALLENGES) {
            return complete ? CHALLENGE_OBTAINED : CHALLENGE_UNOBTAINED;
        }
        return complete ? TASK_OBTAINED : TASK_UNOBTAINED;
    }

    private void drawTabs(GuiGraphicsExtractor graphics, int windowX, int windowY, int mouseX, int mouseY) {
        GoalCategory[] categories = GoalCategory.values();
        for (int index = 0; index < categories.length; index++) {
            GoalCategory category = categories[index];
            TabPosition tab = tabPosition(index, windowX, windowY);
            boolean isSelected = category == selected;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                    tabSprite(index, categories.length, isSelected), tab.x(), tab.y(), TAB_WIDTH, TAB_HEIGHT);
            if (category == GoalCategory.INFO) {
                Component informationGlyph = Component.literal("i").withStyle(ChatFormatting.BOLD);
                float scale = 1.75F;
                float glyphX = tab.x() + (TAB_WIDTH - font.width(informationGlyph) * scale) / 2.0F;
                float glyphY = tab.y() + (tab.below() ? 6.0F : 8.0F);
                graphics.pose().pushMatrix();
                graphics.pose().translate(glyphX, glyphY);
                graphics.pose().scale(scale, scale);
                graphics.text(font, informationGlyph, 0, 0, 0xFFAAAAAA, true);
                graphics.pose().popMatrix();
            } else {
                graphics.item(category.icon, tab.x() + 6, tab.y() + (tab.below() ? 7 : 9));
            }
            if (isOver(mouseX, mouseY, tab.x(), tab.y(), TAB_WIDTH, TAB_HEIGHT)) {
                graphics.setTooltipForNextFrame(font, Component.literal(category.label), mouseX, mouseY);
            }
        }
    }

    private static TabPosition tabPosition(int index, int windowX, int windowY) {
        if (GoalCategory.values()[index] == GoalCategory.INFO) {
            return new TabPosition(windowX + WINDOW_WIDTH - TAB_WIDTH,
                    windowY + WINDOW_HEIGHT - 4, true);
        }
        if (index < TOP_TAB_COUNT) {
            return new TabPosition(windowX + index * (TAB_WIDTH + TAB_SPACING), windowY - TAB_HEIGHT + 4, false);
        }
        return new TabPosition(windowX + (index - TOP_TAB_COUNT) * (TAB_WIDTH + TAB_SPACING),
                windowY + WINDOW_HEIGHT - 4, true);
    }

    private static Identifier tabSprite(int index, int total, boolean selected) {
        boolean below = index >= TOP_TAB_COUNT;
        int rowIndex = below ? index - TOP_TAB_COUNT : index;
        int rowTotal = below ? total - TOP_TAB_COUNT : Math.min(total, TOP_TAB_COUNT);
        String position = rowIndex == 0 ? "left" : rowIndex == rowTotal - 1 ? "right" : "middle";
        return minecraft("advancements/tab_" + (below ? "below_" : "above_")
                + position + (selected ? "_selected" : ""));
    }

    private void drawAdvancementTooltip(GuiGraphicsExtractor graphics, GoalNode node, ViewState view,
                                        PlayerGoalProgress progress, int windowX, int windowY) {
        boolean complete = node.isComplete(progress);
        GoalDefinition primary = node.primary();
        String detail = node.isGroup()
                ? "Completed variants: " + node.completedCount(progress) + "/" + node.goalCount()
                : progressDetail(primary, progress);
        String status = complete ? "Completed" : "Not completed";
        if (!detail.isEmpty()) status += "  •  " + detail;
        List<ItemStack> trackedItems = node.isGroup() ? List.of() : trackedItemIcons(primary, progress);
        List<ChecklistLine> checklist = checklistFor(primary, progress);

        int variantGridWidth = node.isGroup() ? Math.min(7, node.goalCount()) * 18 + 10 : 0;
        int itemGridWidth = Math.min(7, trackedItems.size()) * 18 + (trackedItems.isEmpty() ? 0 : 10);
        int cardWidth = Math.max(Math.max(120, Math.max(itemGridWidth, variantGridWidth)),
                Math.min(200, font.width(node.title()) + 40));
        List<FormattedCharSequence> trackedEntityLines = trackedEntityLines(primary, progress, cardWidth - 10);
        List<FormattedCharSequence> titleLines = font.split(Component.literal(node.title()), cardWidth - 40);
        List<FormattedCharSequence> statusLines = font.split(Component.literal(status), cardWidth - 10);
        int titleHeight = Math.max(28, titleLines.size() * TITLE_LINE_HEIGHT + 10);
        int itemGridHeight = trackedItems.isEmpty() ? 0 : ((trackedItems.size() + 6) / 7) * 18 + 6;
        int variantGridHeight = node.isGroup() ? ((node.goalCount() + 6) / 7) * 18 + 6 : 0;
        int checklistHeight = checklist.size() * STATUS_LINE_HEIGHT;
        int trackedTextHeight = trackedEntityLines.size() * STATUS_LINE_HEIGHT;
        int cardHeight = titleHeight + statusLines.size() * STATUS_LINE_HEIGHT + 12
                + itemGridHeight + variantGridHeight + checklistHeight + trackedTextHeight;

        int nodeX = windowX + INSIDE_X + (int) Math.floor(view.x) + node.x();
        int nodeY = windowY + INSIDE_Y + (int) Math.floor(view.y) + node.y();
        int cardX = nodeX + 31;
        if (cardX + cardWidth > width - 4) cardX = nodeX - cardWidth + 1;
        int cardY = Math.max(4, Math.min(height - cardHeight - 4, nodeY - (cardHeight - NODE_SIZE) / 2));

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                complete ? BOX_OBTAINED : BOX_UNOBTAINED, cardX, cardY, cardWidth, cardHeight);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX, cardX, cardY, cardWidth, titleHeight);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frameFor(complete),
                cardX + 3, cardY + Math.max(0, (titleHeight - NODE_SIZE) / 2), NODE_SIZE, NODE_SIZE);
        drawGoalIcon(graphics, iconFor(node), cardX + 8,
                cardY + Math.max(0, (titleHeight - NODE_SIZE) / 2) + 5, 16);
        if (!node.isGroup()) {
            drawGoalAmount(graphics, primary, cardX + 8,
                    cardY + Math.max(0, (titleHeight - NODE_SIZE) / 2) + 5);
        }

        int titleY = cardY + Math.max(5, (titleHeight - titleLines.size() * TITLE_LINE_HEIGHT) / 2);
        for (int line = 0; line < titleLines.size(); line++) {
            graphics.text(font, titleLines.get(line), cardX + 34,
                    titleY + line * TITLE_LINE_HEIGHT, 0xFFFFFFFF);
        }
        int contentY = cardY + titleHeight + 5;
        for (int line = 0; line < statusLines.size(); line++) {
            graphics.text(font, statusLines.get(line), cardX + 5,
                    contentY + line * STATUS_LINE_HEIGHT, complete ? 0xFF00FF00 : 0xFFAAAAAA);
        }
        contentY += statusLines.size() * STATUS_LINE_HEIGHT + 3;
        for (ChecklistLine line : checklist) {
            if (line.complete()) {
                PixelCheckmark.draw(graphics, cardX + 6, contentY + 1, 0xFF55FF55, false);
            } else {
                graphics.outline(cardX + 7, contentY + 1, 8, 8, 0xFF777777);
            }
            graphics.text(font, Component.literal(line.label()), cardX + 19, contentY,
                    line.complete() ? 0xFF55FF55 : 0xFFAAAAAA, false);
            contentY += STATUS_LINE_HEIGHT;
        }
        for (FormattedCharSequence line : trackedEntityLines) {
            graphics.text(font, line, cardX + 6, contentY, 0xFFDDDDDD);
            contentY += STATUS_LINE_HEIGHT;
        }
        if (!trackedItems.isEmpty()) {
            drawItemGrid(graphics, trackedItems, cardX + 3, contentY);
        } else if (node.isGroup()) {
            drawVariantGrid(graphics, node, progress, cardX + 3, contentY);
        }
    }

    private static void drawItemGrid(GuiGraphicsExtractor graphics, List<ItemStack> items, int x, int y) {
        for (int index = 0; index < items.size(); index++) {
            int itemX = x + 2 + index % 7 * 18;
            int itemY = y + 2 + index / 7 * 18;
            graphics.fill(itemX, itemY, itemX + 16, itemY + 16, Integer.MIN_VALUE);
            graphics.outline(itemX, itemY, 16, 16, 0x40FFFFFF);
            graphics.item(items.get(index), itemX, itemY);
        }
    }

    private void drawVariantGrid(GuiGraphicsExtractor graphics, GoalNode node,
                                 PlayerGoalProgress progress, int x, int y) {
        for (int index = 0; index < node.goals().size(); index++) {
            GoalDefinition goal = node.goals().get(index);
            int iconX = x + 2 + index % 7 * 18;
            int iconY = y + 2 + index / 7 * 18;
            graphics.fill(iconX, iconY, iconX + 16, iconY + 16, Integer.MIN_VALUE);
            graphics.outline(iconX, iconY, 16, 16, 0x40FFFFFF);
            drawGoalIcon(graphics, iconFor(goal), iconX, iconY, 16);
            if (progress.isComplete(goal.sourceId())) {
                graphics.fill(iconX, iconY, iconX + 16, iconY + 16, 0xA0000000);
                PixelCheckmark.draw(graphics, iconX + 3, iconY + 4, 0xFF55FF55, true);
            }
        }
    }

    private static List<ItemStack> trackedItemIcons(GoalDefinition goal, PlayerGoalProgress progress) {
        String key;
        if (goal.sourceId().matches("EAT_(5|10|15|20|25)_UNIQUE_FOOD")) {
            key = "eaten_foods";
        } else if (goal.sourceId().matches("COMPOST_(3|5|7)_UNIQUE_FOODS")) {
            key = "composted_foods";
        } else {
            return List.of();
        }
        return progress.observations(key).stream()
                .map(id -> BuiltInRegistries.ITEM.getValue(minecraft(id)))
                .filter(Objects::nonNull).map(ItemStack::new).toList();
    }

    private static List<ChecklistLine> checklistFor(GoalDefinition goal, PlayerGoalProgress progress) {
        List<String> visited = progress.observations("visited_biomes");
        List<String> eaten = progress.observations("eaten_foods");
        Set<String> minedIce = IceGoalProgress.canonicalTypes(
                progress.observations(IceGoalProgress.OBSERVATION_KEY));
        return switch (goal.sourceId()) {
            case "EAT_ALL_SOUPS" -> List.of(
                    new ChecklistLine("Beetroot Soup", eaten.contains("beetroot_soup")),
                    new ChecklistLine("Mushroom Stew", eaten.contains("mushroom_stew")),
                    new ChecklistLine("Suspicious Stew", eaten.contains("suspicious_stew")),
                    new ChecklistLine("Rabbit Stew", eaten.contains("rabbit_stew"))
            );
            case "VISIT_ALL_CAVE_BIOMES" -> List.of(
                    new ChecklistLine("Lush Caves", visited.contains("lush_caves")),
                    new ChecklistLine("Dripstone Caves", visited.contains("dripstone_caves")),
                    new ChecklistLine("Deep Dark", visited.contains("deep_dark"))
            );
            case "VISIT_ALL_NETHER_BIOMES" -> List.of(
                    new ChecklistLine("Nether Wastes", visited.contains("nether_wastes")),
                    new ChecklistLine("Crimson Forest", visited.contains("crimson_forest")),
                    new ChecklistLine("Warped Forest", visited.contains("warped_forest")),
                    new ChecklistLine("Soul Sand Valley", visited.contains("soul_sand_valley")),
                    new ChecklistLine("Basalt Deltas", visited.contains("basalt_deltas"))
            );
            case "MINE_3_TYPES_OF_ICE" -> List.of(
                    new ChecklistLine("Ice", minedIce.contains("ice")),
                    new ChecklistLine("Packed Ice", minedIce.contains("packed_ice")),
                    new ChecklistLine("Blue Ice", minedIce.contains("blue_ice")),
                    new ChecklistLine("Frosted Ice", minedIce.contains("frosted_ice"))
            );
            default -> List.of();
        };
    }

    private List<FormattedCharSequence> trackedEntityLines(GoalDefinition goal,
                                                           PlayerGoalProgress progress, int width) {
        String key;
        if (goal.sourceId().matches("KILL_(7|10|13|15)_UNIQUE_HOSTILE_MOBS")) {
            key = "killed_hostile_entities";
        } else if (goal.sourceId().equals("KILL_ALL_RAID_MOBS")) {
            key = "killed_raid_mobs";
        } else if (goal.sourceId().matches("SPY_(10|15|20|25)_UNIQUE_MOBS")) {
            key = "spied_entities";
        } else if (goal.sourceId().matches("BREED_(4|6|8)_UNIQUE_ANIMALS")) {
            key = "bred_animals";
        } else {
            return List.of();
        }
        if (progress.observations(key).isEmpty()) return List.of();
        String names = progress.observations(key).stream()
                .map(id -> BuiltInRegistries.ENTITY_TYPE.getValue(minecraft(id)))
                .filter(Objects::nonNull)
                .map(type -> type.getDescription().getString())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return font.split(Component.literal("Recorded: " + names), width);
    }

    private void drawGoalIcon(GuiGraphicsExtractor graphics, GoalIcon icon, int x, int y, int size) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon.texture(), x, y, 0.0F, 0.0F,
                size, size, icon.width(), icon.height(), icon.width(), icon.height());
    }

    private void drawGoalAmount(GuiGraphicsExtractor graphics, GoalDefinition goal, int x, int y) {
        String amount = switch (goal.sourceId()) {
            case "MINE_3_TYPES_OF_ICE" -> "3";
            case "KILL_ALL_RAID_MOBS" -> "6";
            case "SPY_10_UNIQUE_MOBS" -> "10";
            case "SPY_20_UNIQUE_MOBS", "KILL_20_ARTHROPOD_MOBS" -> "20";
            case "SPY_25_UNIQUE_MOBS" -> "25";
            case "KILL_30_UNDEAD_MOBS" -> "30";
            default -> "";
        };
        if (!amount.isEmpty()) {
            graphics.text(font, Component.literal(amount), x + 16 - font.width(amount), y + 8,
                    0xFFFFFFFF, true);
        }
    }

    private GoalNode hoveredNode(List<GoalNode> nodes, ViewState view,
                                 int windowX, int windowY, int mouseX, int mouseY) {
        int insideX = windowX + INSIDE_X;
        int insideY = windowY + INSIDE_Y;
        if (!isOver(mouseX, mouseY, insideX, insideY, INSIDE_WIDTH, INSIDE_HEIGHT)) return null;
        int scrollX = (int) Math.floor(view.x);
        int scrollY = (int) Math.floor(view.y);
        for (GoalNode node : nodes) {
            int nodeX = insideX + scrollX + node.x() + 3;
            int nodeY = insideY + scrollY + node.y();
            if (isOver(mouseX, mouseY, nodeX, nodeY, NODE_SIZE, NODE_SIZE)) return node;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int windowX = (width - WINDOW_WIDTH) / 2;
            int windowY = (height - WINDOW_HEIGHT) / 2;
            GoalCategory[] categories = GoalCategory.values();
            for (int index = 0; index < categories.length; index++) {
                TabPosition tab = tabPosition(index, windowX, windowY);
                if (isOver(event.x(), event.y(), tab.x(), tab.y(), TAB_WIDTH, TAB_HEIGHT)) {
                    selected = categories[index];
                    scrolling = false;
                    return true;
                }
            }
            scrolling = isOver(event.x(), event.y(), windowX + INSIDE_X, windowY + INSIDE_Y,
                    INSIDE_WIDTH, INSIDE_HEIGHT);
            if (scrolling) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() != 0) {
            scrolling = false;
            return false;
        }
        if (scrolling) {
            ViewState view = views.get(selected);
            view.x += dragX;
            view.y += dragY;
            if (selected == GoalCategory.INFO) clampInformationView(view);
            else clampView(view, layouts.get(selected));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        scrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int windowX = (width - WINDOW_WIDTH) / 2;
        int windowY = (height - WINDOW_HEIGHT) / 2;
        if (isOver(mouseX, mouseY, windowX + INSIDE_X, windowY + INSIDE_Y, INSIDE_WIDTH, INSIDE_HEIGHT)) {
            ViewState view = views.get(selected);
            view.x += horizontalAmount * SCROLL_SPEED;
            view.y += verticalAmount * SCROLL_SPEED;
            if (selected == GoalCategory.INFO) clampInformationView(view);
            else clampView(view, layouts.get(selected));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (AllGoalsClient.isOpenKey(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private PlayerGoalProgress currentProgress() {
        if (minecraft == null || minecraft.player == null) return PlayerGoalProgress.empty();
        return minecraft.player.getAttachedOrElse(AllGoalsAttachments.PLAYER_PROGRESS, PlayerGoalProgress.empty());
    }

    private GoalIcon iconFor(GoalDefinition goal) {
        if (!goal.hasRotatingIcon()) return goal.icons().getFirst();
        long period = Math.max(250L, goal.rotationPeriodMillis());
        int frame = (int) ((System.currentTimeMillis() / period) % goal.icons().size());
        return goal.icons().get(frame);
    }

    private GoalIcon iconFor(GoalNode node) {
        if (!node.isGroup()) return iconFor(node.primary());
        int goalIndex = (int) ((System.currentTimeMillis() / 900L) % node.goals().size());
        return iconFor(node.goals().get(goalIndex));
    }

    private static List<GoalDefinition> goalsForCategory(List<GoalDefinition> goals, GoalCategory category) {
        return goals.stream().filter(goal -> primaryCategory(goal) == category).toList();
    }

    private static List<GoalNode> createMilestoneLayout(List<GoalDefinition> goals) {
        List<GoalNode> result = new ArrayList<>();
        for (int index = 0; index < MILESTONE_IDS.size(); index++) {
            String sourceId = MILESTONE_IDS.get(index);
            GoalDefinition goal = goals.stream().filter(candidate -> candidate.sourceId().equals(sourceId))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Missing Milestone goal: " + sourceId));
            int x;
            int y;
            if (index < 8) {
                x = 10 + index % 4 * 52;
                y = 5 + index / 4 * 32;
            } else {
                int lowerIndex = index - 8;
                x = 10 + lowerIndex % 4 * 52;
                y = 84 + lowerIndex / 4 * 32;
            }
            result.add(GoalNode.single(goal, x, y));
        }
        return List.copyOf(result);
    }

    private static List<GoalNode> createDyeLayout(List<GoalDefinition> goals) {
        List<GoalNode> result = new ArrayList<>();
        result.add(familyNode(goals, "Kill Coloured Sheep", VariantGoalIds.COLORED_SHEEP, null, 18, 8));
        result.add(familyNode(goals, "Obtain 64 of Every Concrete Colour", VariantGoalIds.COLORED_CONCRETE, null, 70, 8));
        result.add(familyNode(goals, "Obtain 64 of Every Wool Colour", VariantGoalIds.COLORED_WOOL, null, 122, 8));
        result.add(familyNode(goals, "Obtain Every Glazed Terracotta Colour", VariantGoalIds.GLAZED_TERRACOTTA, null, 174, 8));
        result.add(familyNode(goals, "Wear Every Colour of Leather Cap", VariantGoalIds.DYED_LEATHER, "LEATHER_HELMET", 18, 56));
        result.add(familyNode(goals, "Wear Every Colour of Leather Tunic", VariantGoalIds.DYED_LEATHER, "LEATHER_CHESTPLATE", 70, 56));
        result.add(familyNode(goals, "Wear Every Colour of Leather Pants", VariantGoalIds.DYED_LEATHER, "LEATHER_LEGGINGS", 122, 56));
        result.add(familyNode(goals, "Wear Every Colour of Leather Boots", VariantGoalIds.DYED_LEATHER, "LEATHER_BOOTS", 174, 56));
        goals.stream().filter(goal -> goal.sourceId().equals("WEAR_UNIQUE_COLORED_LEATHER_ARMOR"))
                .findFirst().ifPresent(goal -> result.add(GoalNode.single(goal, 96, 104)));
        return List.copyOf(result);
    }

    private static GoalNode familyNode(List<GoalDefinition> goals, String title, String family,
                                       String requiredText, int x, int y) {
        List<GoalDefinition> variants = goals.stream()
                .filter(goal -> VariantGoalIds.belongsToFamily(goal.sourceId(), family))
                .filter(goal -> requiredText == null || goal.sourceId().contains(requiredText)).toList();
        if (variants.isEmpty()) throw new IllegalStateException("No goal variants found for " + title);
        return new GoalNode(title, variants, x, y);
    }

    private static List<GoalNode> createBreedLayout(List<GoalDefinition> goals) {
        List<GoalNode> result = new ArrayList<>();
        List<GoalDefinition> uniqueGoals = goals.stream()
                .filter(goal -> goal.sourceId().matches("BREED_(4|6|8)_UNIQUE_ANIMALS"))
                .sorted(Comparator.comparing(GoalDefinition::sourceId)).toList();
        for (int index = 0; index < uniqueGoals.size(); index++) {
            result.add(GoalNode.single(uniqueGoals.get(index), 104, 37 + index * 38));
        }
        List<GoalDefinition> individualBreeds = goals.stream()
                .filter(goal -> goal.sourceId().startsWith("BREED_") && !goal.sourceId().contains("UNIQUE_ANIMALS"))
                .sorted(Comparator.comparing(GoalDefinition::displayName)).toList();
        for (int index = 0; index < individualBreeds.size(); index++) {
            result.add(GoalNode.single(individualBreeds.get(index), index % 2 == 0 ? 24 : 184,
                    4 + index / 2 * 31));
        }
        List<GoalDefinition> tameGoals = goals.stream().filter(goal -> goal.sourceId().startsWith("TAME_"))
                .sorted(Comparator.comparing(GoalDefinition::displayName)).toList();
        for (int index = 0; index < tameGoals.size(); index++) {
            result.add(GoalNode.single(tameGoals.get(index), 14 + index * 44, 193));
        }
        return List.copyOf(result);
    }

    private static List<GoalNode> createGridLayout(List<GoalDefinition> goals, GoalCategory category) {
        Comparator<GoalDefinition> comparator = Comparator
                .comparingInt((GoalDefinition goal) -> sortRank(goal, category))
                .thenComparing(GoalDefinition::displayName);
        List<GoalDefinition> sorted = goals.stream().sorted(comparator).toList();
        List<GoalNode> result = new ArrayList<>(sorted.size());
        for (int index = 0; index < sorted.size(); index++) {
            result.add(GoalNode.single(sorted.get(index), index % GRID_COLUMNS * NODE_X_SPACING,
                    index / GRID_COLUMNS * NODE_Y_SPACING));
        }
        return List.copyOf(result);
    }

    private static int sortRank(GoalDefinition goal, GoalCategory category) {
        String id = goal.sourceId();
        if (category == GoalCategory.EQUIPMENT) {
            if (id.matches("OBTAIN_(WOODEN|STONE|IRON|GOLDEN|DIAMOND|COPPER)_TOOLS") || id.equals("BREAK_ANY_TOOL")) return 0;
            if (id.startsWith("WEAR_") || id.contains("ARMOR") || id.contains("ARMOUR")) return 1;
            if (id.contains("TRIM")) return 2;
            if (id.contains("SHIELD")) return 3;
            if (id.contains("WORKSTATION")) return 4;
        }
        if (category == GoalCategory.FOOD) {
            if (id.matches("EAT_(5|10|15|20|25)_UNIQUE_FOOD")) return 0;
            if (id.startsWith("EAT_")) return 1;
            if (id.startsWith("DRINK_")) return 2;
            if (id.startsWith("COMPOST_")) return 3;
        }
        return 10;
    }

    private static GoalCategory primaryCategory(GoalDefinition goal) {
        String id = goal.sourceId().toUpperCase(Locale.ROOT);
        if (VariantGoalIds.isVariantGoal(id) || id.equals("WEAR_UNIQUE_COLORED_LEATHER_ARMOR")) return GoalCategory.DYES;
        if (id.startsWith("BREED_") || id.startsWith("TAME_")) return GoalCategory.BREEDS;
        if (id.startsWith("EAT_") || id.startsWith("DRINK_") || id.startsWith("COMPOST_") || id.equals("EMPTY_HUNGER_BAR")) return GoalCategory.FOOD;
        boolean equipment = id.startsWith("WEAR_") || id.startsWith("EQUIP_") || id.startsWith("ENCHANT_")
                || id.contains("ARMOR") || id.contains("ARMOUR") || id.contains("SHIELD") || id.contains("TRIM")
                || id.contains("WORKSTATION") || id.matches("OBTAIN_(WOODEN|STONE|IRON|GOLDEN|DIAMOND|COPPER)_TOOLS")
                || id.equals("BREAK_ANY_TOOL") || id.equals("PUT_WOLF_ARMOR_ON_WOLF");
        if (equipment) return GoalCategory.EQUIPMENT;
        boolean combat = id.startsWith("KILL_") || id.startsWith("DEAL_") || id.startsWith("SHOOT_")
                || id.startsWith("HIT_") || id.startsWith("DEFLECT_") || id.contains("PROJECTILE")
                || id.startsWith("ENRAGE_") || id.equals("HAVE_YOUR_SHIELD_DISABLED");
        if (combat) return GoalCategory.COMBAT;
        boolean exploration = id.startsWith("VISIT_") || id.startsWith("ENTER_") || id.startsWith("FIND_")
                || id.equals("GET_EYE_SPY_ADVANCEMENT") || id.equals("GET_THE_CITY_AT_THE_END_OF_THE_GAME_ADVANCEMENT")
                || id.equals("GET_THOSE_WERE_THE_DAYS_ADVANCEMENT")
                || id.equals("GET_A_TERRIBLE_FORTRESS_ADVANCEMENT")
                || id.startsWith("REACH_") && !id.startsWith("REACH_EXP_LEVEL_");
        if (exploration) return GoalCategory.EXPLORATION;
        if (id.startsWith("OBTAIN_") || id.startsWith("MINE_") || id.startsWith("CRAFT_")) return GoalCategory.COLLECTION;
        boolean challenge = id.startsWith("DIE_") || id.equals("FREEZE_TO_DEATH")
                || id.matches("GET_\\d+_ADVANCEMENTS") || id.contains("STATUS_EFFECT")
                || id.startsWith("GET_") && id.endsWith("_ADVANCEMENT") || id.equals("TAKE_200_DAMAGE")
                || id.equals("SPRINT_1_KM") || id.equals("WEAR_CARVED_PUMPKIN_FOR_5_MINUTES");
        return challenge ? GoalCategory.CHALLENGES : GoalCategory.ACTIVITIES;
    }

    private static void prepareView(ViewState view, List<GoalNode> nodes) {
        if (!Double.isNaN(view.x) && !Double.isNaN(view.y)) return;
        int contentWidth = contentWidth(nodes);
        int contentHeight = contentHeight(nodes);
        view.x = contentWidth <= INSIDE_WIDTH ? (INSIDE_WIDTH - contentWidth) / 2.0 : 8.0;
        view.y = (INSIDE_HEIGHT - contentHeight) / 2.0;
        clampView(view, nodes);
    }

    private void prepareInformationView(ViewState view) {
        if (!Double.isNaN(view.x) && !Double.isNaN(view.y)) return;
        view.x = 0.0;
        view.y = 0.0;
        clampInformationView(view);
    }

    private void clampInformationView(ViewState view) {
        view.x = 0.0;
        view.y = clamp(view.y, Math.min(0.0, INSIDE_HEIGHT - informationContentHeight()), 0.0);
    }

    private int informationContentHeight() {
        if (cachedInformationContentHeight >= 0) return cachedInformationContentHeight;
        int height = 7;
        for (InformationSection section : INFORMATION_SECTIONS) {
            height += TITLE_LINE_HEIGHT + 5;
            for (String paragraph : section.paragraphs()) {
                height += font.split(Component.literal(paragraph), INSIDE_WIDTH - 16).size()
                        * STATUS_LINE_HEIGHT + 5;
            }
            height += 3;
        }
        height += STATUS_LINE_HEIGHT;
        cachedInformationContentHeight = height;
        return cachedInformationContentHeight;
    }

    private static void clampView(ViewState view, List<GoalNode> nodes) {
        int contentWidth = contentWidth(nodes);
        int contentHeight = contentHeight(nodes);
        view.x = contentWidth <= INSIDE_WIDTH ? (INSIDE_WIDTH - contentWidth) / 2.0
                : clamp(view.x, INSIDE_WIDTH - contentWidth - 8.0, 8.0);
        view.y = contentHeight <= INSIDE_HEIGHT ? (INSIDE_HEIGHT - contentHeight) / 2.0
                : clamp(view.y, INSIDE_HEIGHT - contentHeight - 4.0, 4.0);
    }

    private static int contentWidth(List<GoalNode> nodes) {
        return nodes.stream().mapToInt(node -> node.x() + NODE_SIZE + 3).max().orElse(0);
    }

    private static int contentHeight(List<GoalNode> nodes) {
        return nodes.stream().mapToInt(node -> node.y() + NODE_SIZE).max().orElse(0);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    private static Identifier minecraft(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    private static String progressDetail(GoalDefinition goal, PlayerGoalProgress progress) {
        return switch (goal.sourceId()) {
            case "KILL_100_MOBS" -> Math.min(100, progress.counter("mobs_killed")) + "/100";
            case "DEAL_400_DAMAGE" -> Math.min(400, progress.counter("damage_dealt_tenths") / 10) + "/400 damage";
            case "TAKE_200_DAMAGE" -> Math.min(200, progress.counter("damage_taken_tenths") / 10) + "/200 damage";
            case "SPRINT_1_KM" -> Math.min(1000, progress.counter("sprint_cm") / 100) + "/1000 m";
            case "MINE_3_TYPES_OF_ICE" -> "Ice types mined: "
                    + Math.min(3, IceGoalProgress.canonicalTypes(
                            progress.observations(IceGoalProgress.OBSERVATION_KEY)).size()) + "/3";
            case "EAT_5_UNIQUE_FOOD" -> "Unique foods eaten: " + count(progress, "eaten_foods", 5);
            case "EAT_10_UNIQUE_FOOD" -> "Unique foods eaten: " + count(progress, "eaten_foods", 10);
            case "EAT_15_UNIQUE_FOOD" -> "Unique foods eaten: " + count(progress, "eaten_foods", 15);
            case "EAT_20_UNIQUE_FOOD" -> "Unique foods eaten: " + count(progress, "eaten_foods", 20);
            case "EAT_25_UNIQUE_FOOD" -> "Unique foods eaten: " + count(progress, "eaten_foods", 25);
            case "COMPOST_3_UNIQUE_FOODS" -> "Unique foods composted: " + count(progress, "composted_foods", 3);
            case "COMPOST_5_UNIQUE_FOODS" -> "Unique foods composted: " + count(progress, "composted_foods", 5);
            case "COMPOST_7_UNIQUE_FOODS" -> "Unique foods composted: " + count(progress, "composted_foods", 7);
            case "CRAFT_20_UNIQUE_ITEMS" -> "Unique crafts: " + counter(progress, "unique_crafts_max", 20);
            case "CRAFT_50_UNIQUE_ITEMS" -> "Unique crafts: " + counter(progress, "unique_crafts_max", 50);
            case "CRAFT_100_UNIQUE_ITEMS" -> "Unique crafts: " + counter(progress, "unique_crafts_max", 100);
            case "KILL_7_UNIQUE_HOSTILE_MOBS" -> "Hostile mobs killed: " + count(progress, "killed_hostile_entities", 7);
            case "KILL_10_UNIQUE_HOSTILE_MOBS" -> "Hostile mobs killed: " + count(progress, "killed_hostile_entities", 10);
            case "KILL_13_UNIQUE_HOSTILE_MOBS" -> "Hostile mobs killed: " + count(progress, "killed_hostile_entities", 13);
            case "KILL_15_UNIQUE_HOSTILE_MOBS" -> "Hostile mobs killed: " + count(progress, "killed_hostile_entities", 15);
            case "KILL_ALL_RAID_MOBS" -> "Raid mobs killed: " + count(progress, "killed_raid_mobs", 6);
            case "KILL_20_ARTHROPOD_MOBS" -> Math.min(20, progress.counter("arthropods_killed")) + "/20 killed";
            case "KILL_30_UNDEAD_MOBS" -> Math.min(30, progress.counter("undead_killed")) + "/30 killed";
            case "SPY_10_UNIQUE_MOBS" -> "Mobs spied on: " + count(progress, "spied_entities", 10);
            case "SPY_15_UNIQUE_MOBS" -> "Mobs spied on: " + count(progress, "spied_entities", 15);
            case "SPY_20_UNIQUE_MOBS" -> "Mobs spied on: " + count(progress, "spied_entities", 20);
            case "SPY_25_UNIQUE_MOBS" -> "Mobs spied on: " + count(progress, "spied_entities", 25);
            case "BREED_4_UNIQUE_ANIMALS" -> "Animals bred: " + count(progress, "bred_animals", 4);
            case "BREED_6_UNIQUE_ANIMALS" -> "Animals bred: " + count(progress, "bred_animals", 6);
            case "BREED_8_UNIQUE_ANIMALS" -> "Animals bred: " + count(progress, "bred_animals", 8);
            case "GET_10_ADVANCEMENTS" -> counter(progress, "advancements", 10);
            case "GET_20_ADVANCEMENTS" -> counter(progress, "advancements", 20);
            case "GET_30_ADVANCEMENTS" -> counter(progress, "advancements", 30);
            case "WEAR_CARVED_PUMPKIN_FOR_5_MINUTES" -> Math.min(300, progress.counter("carved_pumpkin_ticks") / 20) + "/300 sec";
            default -> "";
        };
    }

    private static String count(PlayerGoalProgress progress, String key, int target) {
        return Math.min(target, progress.observations(key).size()) + "/" + target;
    }

    private static String counter(PlayerGoalProgress progress, String key, int target) {
        return Math.min(target, progress.counter(key)) + "/" + target;
    }

    private record GoalNode(String title, List<GoalDefinition> goals, int x, int y) {
        private GoalNode {
            goals = List.copyOf(goals);
            if (goals.isEmpty()) throw new IllegalArgumentException("A goal node cannot be empty");
        }
        private static GoalNode single(GoalDefinition goal, int x, int y) {
            return new GoalNode(goal.displayName(), List.of(goal), x, y);
        }
        private GoalDefinition primary() { return goals.getFirst(); }
        private boolean isGroup() { return goals.size() > 1; }
        private int goalCount() { return goals.size(); }
        private int completedCount(PlayerGoalProgress progress) {
            return (int) goals.stream().filter(goal -> progress.isComplete(goal.sourceId())).count();
        }
        private boolean isComplete(PlayerGoalProgress progress) { return completedCount(progress) == goalCount(); }
    }

    private record ChecklistLine(String label, boolean complete) { }
    private record InformationSection(String heading, List<String> paragraphs) { }
    private record TabPosition(int x, int y, boolean below) { }

    private static final class ViewState {
        private double x = Double.NaN;
        private double y = Double.NaN;
    }

    private enum GoalCategory {
        MILESTONES("Milestones", "textures/block/obsidian.png", new ItemStack(Items.NETHER_STAR)),
        DYES("Dyes", "textures/block/white_wool.png", new ItemStack(Items.RED_DYE)),
        BREEDS("Breeds & Taming", "textures/block/oak_planks.png", new ItemStack(Items.WHEAT)),
        FOOD("Food", "textures/block/hay_block_side.png", new ItemStack(Items.COOKED_BEEF)),
        EQUIPMENT("Equipment", "textures/block/iron_block.png", new ItemStack(Items.IRON_CHESTPLATE)),
        COMBAT("Combat", "textures/block/netherrack.png", new ItemStack(Items.IRON_SWORD)),
        EXPLORATION("Exploration", "textures/block/deepslate.png", new ItemStack(Items.COMPASS)),
        COLLECTION("Collection", "textures/block/blackstone.png", new ItemStack(Items.CHEST)),
        ACTIVITIES("Activities", "textures/block/bricks.png", new ItemStack(Items.LEVER)),
        CHALLENGES("Challenges", "textures/block/soul_sand.png", new ItemStack(Items.TOTEM_OF_UNDYING)),
        INFO("Information", "textures/block/blue_concrete.png", ItemStack.EMPTY);

        private final String label;
        private final Identifier background;
        private final ItemStack icon;

        GoalCategory(String label, String background, ItemStack icon) {
            this.label = label;
            this.background = minecraft(background);
            this.icon = icon;
        }
    }
}
