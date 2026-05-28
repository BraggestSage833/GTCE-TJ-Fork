package gregtech.loaders.oreprocessing;

import gregtech.api.recipes.ModHandler;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.unification.material.type.IngotMaterial;
import gregtech.api.unification.material.type.Material;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.unification.stack.UnificationEntry;
import net.minecraft.item.ItemStack;

public class PolarizingRecipeHandler {

    private static final OrePrefix[] POLARIZING_PREFIXES = new OrePrefix[]{
        OrePrefix.stick, OrePrefix.stickLong, OrePrefix.plate, OrePrefix.ingot, OrePrefix.plateDense, OrePrefix.rotor,
        OrePrefix.bolt, OrePrefix.screw, OrePrefix.wireFine, OrePrefix.foil, OrePrefix.dust, OrePrefix.ring};

    public static void register() {
        for (OrePrefix orePrefix : POLARIZING_PREFIXES) {
            orePrefix.addProcessingHandler(IngotMaterial.class, PolarizingRecipeHandler::processPolarizing);
        }
    }

    public static void processPolarizing(OrePrefix polarizingPrefix, IngotMaterial material) {
        if (material.magneticMaterial != null && polarizingPrefix.doGenerateItem(material.magneticMaterial)) {
            ItemStack magneticStack = OreDictUnifier.get(polarizingPrefix, material.magneticMaterial);
            RecipeMaps.POLARIZER_RECIPES.recipeBuilder() //polarizing
                .input(polarizingPrefix, material)
                .outputs(magneticStack)
                .duration(16)
                    .EUt( getTieredVoltageMultiplier(material))
                .buildAndRegister();

            ModHandler.addSmeltingRecipe(new UnificationEntry(polarizingPrefix, material.magneticMaterial),
                OreDictUnifier.get(polarizingPrefix, material)); //de-magnetizing
        }
    }
    private static int getTieredVoltageMultiplier(Material material) {
        int eut = 16;
        int heat = ((IngotMaterial) material).blastFurnaceTemperature;
        if (heat < 2700){
            eut = 28;
        }
        if ( heat >= 2700 && heat < 3600){
            eut = 500;
        }
        if ( heat >= 3600 && heat < 4500){
            eut = 1000;
        }
        if( heat >= 4500 && heat < 5400){
            eut = 4000;
        }
        if ( heat >= 5400 && heat < 7200 ){
            eut = 8000;
        }
        if ( heat >= 7200 && heat < 8600){
            eut = 32000;
        }
        if (heat >= 8600 && heat < 9600){
            eut = 131000;
        }
        if ( heat >= 9600 && heat < 10700){
            eut = 500000;
        }
        if ( heat >= 10700 && heat < 11200){
            eut = 2000000;
        }
        if ( heat >= 11200 && heat < 12600){
            eut = 8000000;
        }
        if ( heat >= 12600 && heat < 14200){
            eut = 33000000;
        }
        if( heat >= 14200 && heat < 56800) {
            eut = 134000000;
        }
        return eut;
    }

}
