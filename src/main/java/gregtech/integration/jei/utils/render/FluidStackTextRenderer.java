package gregtech.integration.jei.utils.render;

import gregtech.api.gui.resources.RenderUtil;
import gregtech.api.util.TextFormattingUtil;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.plugins.vanilla.ingredients.fluid.FluidStackRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FluidStackTextRenderer extends FluidStackRenderer {
    public FluidStackTextRenderer(int capacityMb, boolean showCapacity, int width, int height, @Nullable IDrawable overlay) {
        super(capacityMb, showCapacity, width, height, overlay);
    }

    @Override
    public void render(@Nonnull Minecraft minecraft, final int xPosition, final int yPosition, @Nullable FluidStack fluidStack) {
        if (fluidStack == null)
            return;
        int amount = fluidStack.amount;
        if (amount < 1)
            fluidStack.amount = 1;
        GlStateManager.disableBlend();

        RenderUtil.drawFluidForGui(fluidStack, fluidStack.amount, xPosition, yPosition, 17, 17);
        fluidStack.amount = amount;

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5, 0.5, 1);

        String s = amount > 0 ? TextFormattingUtil.formatLongToCompactString(fluidStack.amount, 4) + "L" : "NC";
        minecraft.fontRenderer.drawStringWithShadow(s, (xPosition + 6) * 2 - minecraft.fontRenderer.getStringWidth(s) + 21, (yPosition + 12) * 2, amount > 0 ? 0xFFFFFF : 0xFFEC00);

        GlStateManager.popMatrix();

        GlStateManager.enableBlend();
    }
}
