package gregtech.api.recipes.machines;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.ModHandler;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.builders.SimpleRecipeBuilder;
import gregtech.api.util.GTUtility;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;

public class RecipeMapFurnace extends RecipeMap<SimpleRecipeBuilder> {

    public RecipeMapFurnace(String unlocalizedName, int minInputs, int maxInputs, int minOutputs, int maxOutputs, int minFluidInputs, int maxFluidInputs, int minFluidOutputs, int maxFluidOutputs, int amperage, SimpleRecipeBuilder defaultRecipe) {
        super(unlocalizedName, minInputs, maxInputs, minOutputs, maxOutputs, minFluidInputs, maxFluidInputs, minFluidOutputs, maxFluidOutputs, defaultRecipe);
    }

    @Override
    @Nullable
    public Recipe searchRecipe(long voltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, int outputFluidTankCapacity, boolean useOptimizedRecipeLookUp) {
        Recipe normalRecipe = super.searchRecipe(voltage, inputs, fluidInputs, outputFluidTankCapacity, useOptimizedRecipeLookUp);
        if (normalRecipe != null || inputs.getSlots() == 0)
            return normalRecipe;

        for (ItemStack input : GTUtility.itemHandlerToList(inputs)) {
            ItemStack output = ModHandler.getSmeltingOutput(input);

            if (!output.isEmpty()) {
                return this.recipeBuilder()
                    .inputs(GTUtility.copyAmount(1, input))
                    .outputs(output)
                    .duration(128).EUt(4)
                    .build().getResult();
            }
        }

        return null;
    }
}
