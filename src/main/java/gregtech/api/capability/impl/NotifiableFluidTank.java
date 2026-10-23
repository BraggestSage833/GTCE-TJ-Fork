package gregtech.api.capability.impl;

import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import java.util.List;

public class NotifiableFluidTank extends FluidTank implements IInventoryNotifiable {

    protected final List<MetaTileEntity> metaTileEntitiesToNotify = new ObjectArrayList<>();

    public NotifiableFluidTank(MetaTileEntity metaTileEntity, int capacity) {
        super(capacity);
        this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    public NotifiableFluidTank(MetaTileEntity metaTileEntity, FluidStack fluidStack, int amount) {
        super(fluidStack, amount);
        this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    public NotifiableFluidTank(MetaTileEntity metaTileEntity, Fluid fluid, int amount, int capacity) {
        super(fluid, amount, capacity);
        this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    @Override
    protected void onContentsChanged() {
        for (MetaTileEntity metaTileEntity : this.metaTileEntitiesToNotify)
            metaTileEntity.setInventoryNotified(true);
    }

    @Override
    public void addMetaTileEntityToNotifiable(MetaTileEntity metaTileEntity) {
        this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    @Override
    public void removeMetaTileEntityFromNotifiable(MetaTileEntity metaTileEntity) {
        this.metaTileEntitiesToNotify.remove(metaTileEntity);
    }
}
