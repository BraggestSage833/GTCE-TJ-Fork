package gregtech.integration.jei.utils.render;

import gregtech.api.gui.Widget;
import mezz.jei.plugins.vanilla.ingredients.item.ItemStackRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class ItemStackTextRenderer extends ItemStackRenderer {

    private final boolean consumable;

    public ItemStackTextRenderer(boolean consumable) {
        this.consumable = consumable;
    }

    @Override
    public void render(@Nonnull Minecraft minecraft, int xPosition, int yPosition, @Nullable ItemStack ingredient) {
        if (ingredient == null) return;
        Widget.drawItemStack(ingredient, xPosition + 1, yPosition + 1, null);
        if (!this.consumable && !ingredient.isEmpty()) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5, 0.5, 1);
            minecraft.fontRenderer.drawStringWithShadow("NC", (xPosition + 6) * 2 - minecraft.fontRenderer.getStringWidth("NC") + 21, (yPosition + 13) * 2, 0xFFEC00);
            GlStateManager.popMatrix();
            GlStateManager.enableBlend();
        }
    }
}
