package gregtech.integration.jei.utils.render;

import gregtech.api.GTValues;
import gregtech.api.gui.Widget;
import mezz.jei.plugins.vanilla.ingredients.item.ItemStackRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;

public class ItemStackTextRenderer extends ItemStackRenderer {

    private final boolean consumable;
    private final int chance;
    private final int boost;

    public ItemStackTextRenderer(int chance, int boost) {
        this(true, chance, boost);
    }

    public ItemStackTextRenderer(boolean consumable) {
        this(consumable, 0, 0);
    }

    public ItemStackTextRenderer(boolean consumable, int chance, int boost) {
        this.consumable = consumable;
        this.chance = chance;
        this.boost = boost;
    }

    @Override
    public void render(@Nonnull Minecraft minecraft, int xPosition, int yPosition, @Nullable ItemStack ingredient) {
        if (ingredient == null) return;
        Widget.drawItemStack(ingredient, xPosition + 1, yPosition + 1, null);
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5, 0.5, 1);
        if (!this.consumable && !ingredient.isEmpty())
            minecraft.fontRenderer.drawStringWithShadow("NC", (xPosition + 6) * 2 - minecraft.fontRenderer.getStringWidth("NC") + 21, (yPosition + 13) * 2, 0xFFEC00);
        if (this.chance > 0 && !(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
            String chance = GTValues.thousandTwoPlaceFormat.format(this.chance / 100.0) + "%";
            minecraft.fontRenderer.drawStringWithShadow(chance, (xPosition + 6) * 2 - minecraft.fontRenderer.getStringWidth(chance) + 21, (yPosition + 13) * 2, 0xFFEC00);
        }
        if (this.boost > 0 && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
            String boost = "+" + GTValues.thousandTwoPlaceFormat.format(this.boost / 100.0) + "%";
            minecraft.fontRenderer.drawStringWithShadow(boost, (xPosition + 6) * 2 - minecraft.fontRenderer.getStringWidth(boost) + 21, (yPosition + 13) * 2, 0xFFEC00);
        }
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
    }
}
