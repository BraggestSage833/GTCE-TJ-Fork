package gregtech.integration.jei.recipe.primitive;

import gregtech.api.gui.*;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.TankWidget;
import gregtech.api.recipes.recipes.CokeOvenRecipe;
import gregtech.integration.jei.utils.render.FluidStackTextRenderer;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class CokeOvenRecipeCategory extends PrimitiveRecipeCategory<CokeOvenRecipe, CokeOvenRecipeWrapper> {

    private final ModularUI modularUI;
	
	public CokeOvenRecipeCategory(IGuiHelper guiHelper) {
		super("coke_oven",
            "gregtech.machine.coke_oven.name",
            guiHelper.createBlankDrawable(176, 166), guiHelper);
        this.modularUI = ModularUI.builder(GuiTextures.BLANK_BACKGROUND, this.background.getWidth(), this.background.getHeight())
                .widget(new ProgressWidget(() -> 0, 57, 20, 20, 20, GuiTextures.BRONZE_BLAST_FURNACE_PROGRESS_BAR, ProgressWidget.MoveType.HORIZONTAL)
                        .setUpdateOnClient(true))
                .widget(new SlotWidget(new ItemStackHandler(1), 0, 32, 19, false, false)
                        .setBackgroundTexture(GuiTextures.SLOT))
                .widget(new SlotWidget(new ItemStackHandler(1), 0, 4, 19, false, false)
                        .setBackgroundTexture(GuiTextures.SLOT))
                .widget(new TankWidget(new FluidTank(Integer.MAX_VALUE), 133, 3, 58, 20)
                        .setBackgroundTexture(GuiTextures.FLUID_TANK_BACKGROUND)
                        .setOverlayTexture(GuiTextures.FLUID_TANK_OVERLAY))
                .build(new BlankUIHolder(), Minecraft.getMinecraft().player);
        this.modularUI.initWidgets();
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, @NotNull CokeOvenRecipeWrapper recipeWrapper, @NotNull IIngredients ingredients) {
		IGuiItemStackGroup itemStackGroup = recipeLayout.getItemStacks();
		IGuiFluidStackGroup fluidStackGroup = recipeLayout.getFluidStacks();
		itemStackGroup.init(0, true, 32, 19);
		itemStackGroup.init(1, false, 84, 19);
		itemStackGroup.set(ingredients);
        int amount = recipeWrapper.getRecipe().getFluidOutput() != null ? recipeWrapper.getRecipe().getFluidOutput().amount : 0;
        fluidStackGroup.init(0, false, new FluidStackTextRenderer(amount, false, 58, 20, null),
                133, 3, 20, 58, 0, 0);
		fluidStackGroup.set(ingredients);
        this.modularUI.guiWidgets.values().stream()
                .filter(widget -> widget instanceof ProgressWidget)
                .forEach(widget -> ((ProgressWidget) widget).setMaxProgress(recipeWrapper.getRecipe().getDuration()));
	}

	@Override
	public @NotNull IRecipeWrapper getRecipeWrapper(@NotNull CokeOvenRecipe recipe) {
		return new CokeOvenRecipeWrapper(recipe);
	}

	@Override
	public void drawExtras(Minecraft minecraft) {
        for (Widget widget : this.modularUI.guiWidgets.values()) {
            widget.updateScreen();
            widget.drawInForeground(0, 0);
            widget.drawInBackground(0, 0, new IRenderContext() {
            });
        }
	}
}
