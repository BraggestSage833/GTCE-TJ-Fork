package gregtech.api.capability.impl;

import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

public class NotifiableFluidTank extends FluidTank implements IInventoryNotifiable {

    protected MetaTileEntity metaTileEntity;

    public NotifiableFluidTank(MetaTileEntity metaTileEntity, int capacity) {
        super(capacity);
        this.metaTileEntity = metaTileEntity;
    }

    public NotifiableFluidTank(MetaTileEntity metaTileEntity, FluidStack fluidStack, int amount) {
        super(fluidStack, amount);
        this.metaTileEntity = metaTileEntity;
    }

    public NotifiableFluidTank(MetaTileEntity metaTileEntity, Fluid fluid, int amount, int capacity) {
        super(fluid, amount, capacity);
        this.metaTileEntity = metaTileEntity;
    }

    @Override
    protected void onContentsChanged() {
        this.metaTileEntity.setInventoryNotified(true);
    }

    @Override
    public void setMetaTileEntity(MetaTileEntity metaTileEntity) {
        this.metaTileEntity = metaTileEntity;
    }
}
