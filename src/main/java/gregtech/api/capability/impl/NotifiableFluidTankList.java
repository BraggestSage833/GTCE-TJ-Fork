package gregtech.api.capability.impl;

import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraftforge.fluids.IFluidTank;

import java.util.List;

/**
 * Meant to hold multiple {@link NotifiableFluidTank} or any other fluid tank that implements {@link IInventoryNotifiable}
 */
public class NotifiableFluidTankList extends FluidTankList implements IInventoryNotifiable {

    public NotifiableFluidTankList(MetaTileEntity metaTileEntity, boolean allowSameFluidFill, IFluidTank... fluidTanks) {
        super(allowSameFluidFill, fluidTanks);
        this.addMetaTileEntityToNotifiable(metaTileEntity);
    }

    public NotifiableFluidTankList(MetaTileEntity metaTileEntity, boolean allowSameFluidFill, List<? extends IFluidTank> fluidTanks) {
        super(allowSameFluidFill, fluidTanks);
        this.addMetaTileEntityToNotifiable(metaTileEntity);
    }

    public NotifiableFluidTankList(MetaTileEntity metaTileEntity, boolean allowSameFluidFill, FluidTankList parent, IFluidTank... fluidTanks) {
        super(allowSameFluidFill, parent, fluidTanks);
        this.addMetaTileEntityToNotifiable(metaTileEntity);
    }

    @Override
    public void addMetaTileEntityToNotifiable(MetaTileEntity metaTileEntity) {
        for (IFluidTank tank : this.fluidTanks) {
            if (tank instanceof IInventoryNotifiable)
                ((IInventoryNotifiable) tank).addMetaTileEntityToNotifiable(metaTileEntity);
        }
    }

    @Override
    public void removeMetaTileEntityFromNotifiable(MetaTileEntity metaTileEntity) {
        for (IFluidTank tank : this.fluidTanks) {
            if (tank instanceof IInventoryNotifiable)
                ((IInventoryNotifiable) tank).removeMetaTileEntityFromNotifiable(metaTileEntity);
        }
    }
}
