package dev.allgoals.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

final class PixelCheckmark {
    private PixelCheckmark() {
    }

    static void draw(GuiGraphicsExtractor graphics, int x, int y, int color, boolean shadow) {
        if (shadow) drawShape(graphics, x + 1, y + 1, 0xB0000000);
        drawShape(graphics, x, y, color);
    }

    private static void drawShape(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y + 4, x + 2, y + 6, color);
        graphics.fill(x + 2, y + 6, x + 4, y + 8, color);
        graphics.fill(x + 4, y + 4, x + 6, y + 6, color);
        graphics.fill(x + 6, y + 2, x + 8, y + 4, color);
        graphics.fill(x + 8, y, x + 10, y + 2, color);
    }
}
