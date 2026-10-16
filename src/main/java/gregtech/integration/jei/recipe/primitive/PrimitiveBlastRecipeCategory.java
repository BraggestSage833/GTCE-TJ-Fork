package gregtech.integration.jei.recipe.primitive;

import gregtech.api.gui.*;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.recipes.recipes.PrimitiveBlastFurnaceRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;

public class PrimitiveBlastRecipeCategory extends PrimitiveRecipeCategory<PrimitiveBlastFurnaceRecipe, PrimitiveBlastRecipeWrapper> {

    protected final IDrawable slot;
    private final ModularUI modularUI;

	public PrimitiveBlastRecipeCategory(IGuiHelper guiHelper) {
		super("primitive_blast_furnace",
            "gregtech.machine.primitive_blast_furnace.bronze.name",
                guiHelper.createBlankDrawable(140, 60), guiHelper);
        this.slot = guiHelper.createDrawable(GuiTextures.SLOT.imageLocation, 0, 0, 18, 18, 18, 18);
        this.modularUI = ModularUI.builder(GuiTextures.BLANK_BACKGROUND, this.background.getWidth(), this.background.getHeight())
                .widget(new ProgressWidget(() -> 0, 57, 14, 20, 20, GuiTextures.BRONZE_BLAST_FURNACE_PROGRESS_BAR, ProgressWidget.MoveType.HORIZONTAL)
                        .setUpdateOnClient(true))
                .build(new BlankUIHolder(), Minecraft.getMinecraft().player);
        this.modularUI.initWidgets();
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, PrimitiveBlastRecipeWrapper recipeWrapper,
			IIngredients ingredients) {
		IGuiItemStackGroup itemStackGroup = recipeLayout.getItemStacks();
		itemStackGroup.init(0, true, 32, 4);
		itemStackGroup.init(1, true, 32, 22);

		itemStackGroup.init(2, false, 84, 13);
		itemStackGroup.init(3, false, 102, 13);
		itemStackGroup.set(ingredients);
        this.modularUI.guiWidgets.values().stream()
                .filter(widget -> widget instanceof ProgressWidget)
                .forEach(widget -> ((ProgressWidget) widget).setMaxProgress(recipeWrapper.getRecipe().getDuration()));
	}

	@Override
	public IRecipeWrapper getRecipeWrapper(PrimitiveBlastFurnaceRecipe recipe) {
		return new PrimitiveBlastRecipeWrapper(recipe);
	}

	@Override
	public void drawExtras(Minecraft minecraft) {
        for (Widget widget : this.modularUI.guiWidgets.values()) {
            widget.updateScreen();
            widget.drawInForeground(0, 0);
            widget.drawInBackground(0, 0, new IRenderContext() {
            });
        }
		this.slot.draw(minecraft, 32, 4);
		this.slot.draw(minecraft, 32, 22);
		this.slot.draw(minecraft, 84, 13);
		this.slot.draw(minecraft, 102, 13);
	}
}
