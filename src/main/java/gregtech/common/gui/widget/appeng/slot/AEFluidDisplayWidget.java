package gregtech.common.gui.widget.appeng.slot;

import appeng.api.storage.data.IAEFluidStack;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.RenderUtil;
import gregtech.api.util.FluidTooltipUtil;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import gregtech.common.gui.widget.appeng.AEListGridWidget;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author GlodBlock
 * @Description Display a certain {@link IAEFluidStack} element.
 * @Date 2023/4/19-0:30
 */
public class AEFluidDisplayWidget extends Widget {

    private final AEListGridWidget<IAEFluidStack> gridWidget;
    private final int index;

    public AEFluidDisplayWidget(int x, int y, AEListGridWidget<IAEFluidStack> gridWidget, int index) {
        super(new Position(x, y), new Size(18, 18));
        this.gridWidget = gridWidget;
        this.index = index;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        super.drawInBackground(mouseX, mouseY, context);
        Position position = getPosition();
        IAEFluidStack fluid = this.gridWidget.getAt(this.index);
        GuiTextures.FLUID_SLOT.draw(position.x, position.y, 18, 18);
        GuiTextures.NUMBER_BACKGROUND.draw(position.x + 18, position.y, 140, 18);
        int stackX = position.x + 1;
        int stackY = position.y + 1;
        if (fluid != null) {
            RenderUtil.drawFluidForGui(fluid.getFluidStack(), fluid.getFluidStack().amount, stackX, stackY, 17, 17);
            String amountStr = String.format("x%,d", fluid.getStackSize());
            drawStringSized(amountStr, stackX + 20, stackY + 5, 0xFFFFFFFF, false, 1, false);
        }
        if (isMouseOverElement(mouseX, mouseY)) {
            drawSelectionOverlay(stackX, stackY, 16, 16);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInForeground(int mouseX, int mouseY) {
        if (isMouseOverElement(mouseX, mouseY)) {
            IAEFluidStack fluid = this.gridWidget.getAt(this.index);
            if (fluid != null) {
                List<String> hoverStringList = new ArrayList<>();
                hoverStringList.add(fluid.getFluidStack().getLocalizedName());
                hoverStringList.add(String.format("%,d L", fluid.getStackSize()));
                List<String> formula = Collections.singletonList(FluidTooltipUtil.getFluidTooltip(fluid.getFluidStack()));
                if (formula != null) {
                    for (String s : formula) {
                        if (s == null || s.isEmpty()) continue;
                        hoverStringList.add(s);
                    }
                }
                drawHoveringText(ItemStack.EMPTY, hoverStringList, -1, mouseX, mouseY);
            }
        }
    }
}
