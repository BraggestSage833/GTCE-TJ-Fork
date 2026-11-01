package gregtech.api.capability.impl;

import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import java.util.List;

/**
 * Use {@link NotifiableFluidTank} to notify {@link MetaTileEntity} that fluid tank inventory has changed.
 * Only should notify if fluid amounts have changed since last insertion or extraction.
 * This can be used to trigger recipe search only when necessary.
 */
public class NotifiableFluidTank extends FluidTank implements IInventoryNotifiable {

    protected final List<MetaTileEntity> metaTileEntitiesToNotify = new ObjectArrayList<>();

    public NotifiableFluidTank(MetaTileEntity metaTileEntity, int capacity) {
        super(capacity);
        if (metaTileEntity != null)
            this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    public NotifiableFluidTank(MetaTileEntity metaTileEntity, FluidStack fluidStack, int amount) {
        super(fluidStack, amount);
        if (metaTileEntity != null)
            this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    public NotifiableFluidTank(MetaTileEntity metaTileEntity, Fluid fluid, int amount, int capacity) {
        super(fluid, amount, capacity);
        if (metaTileEntity != null)
            this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    @Override
    protected void onContentsChanged() {
        for (MetaTileEntity metaTileEntity : this.metaTileEntitiesToNotify)
            metaTileEntity.setInventoryNotified(true);
    }

    @Override
    public void addMetaTileEntityToNotifiable(MetaTileEntity metaTileEntity) {
        if (metaTileEntity != null)
            this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    @Override
    public void removeMetaTileEntityFromNotifiable(MetaTileEntity metaTileEntity) {
        this.metaTileEntitiesToNotify.remove(metaTileEntity);
    }

    @Override
    public int fillInternal(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) {
            return 0;
        }
        if (!doFill) {
            if (fluid == null) {
                return Math.min(capacity, resource.amount);
            }
            if (!fluid.isFluidEqual(resource)) {
                return 0;
            }
            return Math.min(capacity - fluid.amount, resource.amount);
        }
        if (fluid == null) {
            fluid = new FluidStack(resource, Math.min(capacity, resource.amount));
            onContentsChanged();
            if (tile != null) {
                FluidEvent.fireEvent(new FluidEvent.FluidFillingEvent(fluid, tile.getWorld(), tile.getPos(), this, fluid.amount));
            }
            return fluid.amount;
        }
        if (!fluid.isFluidEqual(resource)) {
            return 0;
        }
        int filled = capacity - fluid.amount;
        if (resource.amount < filled) {
            fluid.amount += resource.amount;
            filled = resource.amount;
        } else {
            fluid.amount = capacity;
        }
        if (filled > 0) // don't notify if fluid amounts have not been changed.
            onContentsChanged();
        if (tile != null) {
            FluidEvent.fireEvent(new FluidEvent.FluidFillingEvent(fluid, tile.getWorld(), tile.getPos(), this, filled));
        }
        return filled;
    }
}
