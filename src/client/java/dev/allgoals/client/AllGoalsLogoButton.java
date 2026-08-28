package dev.allgoals.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Compact item shortcut which keeps the mod settings separate from
 * Minecraft's primary menu actions.
 */
final class AllGoalsLogoButton extends Button {
    static final int SIZE = 20;
    private static final Identifier ICON = Identifier.withDefaultNamespace("textures/item/nether_star.png");

    AllGoalsLogoButton(int x, int y, OnPress onPress) {
        super(x, y, SIZE, SIZE, Component.translatable("gui.all_goals.logo_button"),
                onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.translatable("gui.all_goals.logo_button.tooltip")));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, getX() + 2, getY() + 2,
                0.0F, 0.0F, 16, 16, 16, 16, 16, 16);
    }
}
