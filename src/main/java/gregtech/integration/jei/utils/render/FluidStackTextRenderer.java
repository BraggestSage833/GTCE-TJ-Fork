package gregtech.integration.jei.utils.render;

import gregtech.api.gui.resources.RenderUtil;
import gregtech.api.util.TextFormattingUtil;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.plugins.vanilla.ingredients.fluid.FluidStackRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

public class FluidStackTextRenderer extends FluidStackRenderer {
    public FluidStackTextRenderer(int capacityMb, boolean showCapacity, int width, int height, @Nullable IDrawable overlay) {
        super(capacityMb, showCapacity, width, height, overlay);
    }

    @Override
    public void render(Minecraft minecraft, final int xPosition, final int yPosition, @Nullable FluidStack fluidStack) {
        if (fluidStack == null)
            return;

        GlStateManager.disableBlend();

        RenderUtil.drawFluidForGui(fluidStack, fluidStack.amount, xPosition, yPosition, 17, 17);

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5, 0.5, 1);

        String s = TextFormattingUtil.formatLongToCompactString(fluidStack.amount, 4) + "L";

        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        fontRenderer.drawStringWithShadow(s, (xPosition + 6) * 2 - fontRenderer.getStringWidth(s) + 21, (yPosition + 12) * 2, 0xFFFFFF);
        GlStateManager.popMatrix();

        GlStateManager.enableBlend();
    }
}
