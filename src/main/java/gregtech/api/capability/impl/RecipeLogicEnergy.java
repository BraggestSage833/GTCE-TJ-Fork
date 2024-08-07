package gregtech.api.capability.impl;

import gregtech.api.capability.IEnergyContainer;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.RecipeMap;

import java.util.function.Supplier;

public class RecipeLogicEnergy extends AbstractRecipeLogic {

    private final Supplier<IEnergyContainer> energyContainer;

    public RecipeLogicEnergy(MetaTileEntity tileEntity, RecipeMap<?> recipeMap, Supplier<IEnergyContainer> energyContainer) {
        this(tileEntity, recipeMap, energyContainer, 16);
    }
    public RecipeLogicEnergy(MetaTileEntity tileEntity, RecipeMap<?> recipeMap, Supplier<IEnergyContainer> energyContainer, int recipeCacheSize) {
        super(tileEntity, recipeMap, recipeCacheSize);
        this.energyContainer = energyContainer;
    }

    @Override
    protected long getEnergyStored() {
        return energyContainer.get().getEnergyStored();
    }

    @Override
    protected long getEnergyCapacity() {
        return energyContainer.get().getEnergyCapacity();
    }

    @Override
    protected boolean drawEnergy(int recipeEUt) {
        long resultEnergy = getEnergyStored() - recipeEUt;
        if (resultEnergy >= 0L && resultEnergy <= getEnergyCapacity()) {
            energyContainer.get().changeEnergy(-recipeEUt);
            return true;
        } else return false;
    }

    @Override
    protected long getMaxVoltage() {
        return Math.max(energyContainer.get().getInputVoltage(),
            energyContainer.get().getOutputVoltage());
    }

}
