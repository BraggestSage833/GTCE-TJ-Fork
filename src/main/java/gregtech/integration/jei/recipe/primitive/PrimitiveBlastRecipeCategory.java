package gregtech.integration.jei.recipe.primitive;

import gregtech.api.gui.*;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.recipes.recipes.PrimitiveBlastFurnaceRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class PrimitiveBlastRecipeCategory extends PrimitiveRecipeCategory<PrimitiveBlastFurnaceRecipe, PrimitiveBlastRecipeWrapper> {

    private final ModularUI modularUI;

	public PrimitiveBlastRecipeCategory(IGuiHelper guiHelper) {
		super("primitive_blast_furnace",
            "gregtech.machine.primitive_blast_furnace.bronze.name",
                guiHelper.createBlankDrawable(140, 60), guiHelper);
        this.modularUI = ModularUI.builder(GuiTextures.BLANK_BACKGROUND, this.background.getWidth(), this.background.getHeight())
                .widget(new ProgressWidget(() -> 0, 57, 14, 20, 20, GuiTextures.BRONZE_BLAST_FURNACE_PROGRESS_BAR, ProgressWidget.MoveType.HORIZONTAL)
                        .setUpdateOnClient(true))
                .widget(new SlotWidget(new ItemStackHandler(1), 0, 32, 4, false, false)
                        .setBackgroundTexture(GuiTextures.SLOT))
                .widget(new SlotWidget(new ItemStackHandler(1), 0, 32, 22, false, false)
                        .setBackgroundTexture(GuiTextures.SLOT))
                .widget(new SlotWidget(new ItemStackHandler(1), 0, 84, 13, false, false)
                        .setBackgroundTexture(GuiTextures.SLOT))
                .widget(new SlotWidget(new ItemStackHandler(1), 0, 102, 13, false, false)
                        .setBackgroundTexture(GuiTextures.SLOT))
                .build(new BlankUIHolder(), Minecraft.getMinecraft().player);
        this.modularUI.initWidgets();
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, @NotNull PrimitiveBlastRecipeWrapper recipeWrapper, @NotNull IIngredients ingredients) {
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
	public @NotNull IRecipeWrapper getRecipeWrapper(@NotNull PrimitiveBlastFurnaceRecipe recipe) {
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
	}
}
