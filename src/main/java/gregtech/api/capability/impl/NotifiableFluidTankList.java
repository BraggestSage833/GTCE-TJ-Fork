package gregtech.api.capability.impl;

import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NotifiableFluidTankList extends FluidTankList implements IInventoryNotifiable {

    public NotifiableFluidTankList(MetaTileEntity metaTileEntity, boolean allowSameFluidFill, IFluidTank... fluidTanks) {
        super(allowSameFluidFill, fluidTanks);
        this.setMetaTileEntity(metaTileEntity);
    }

    public NotifiableFluidTankList(MetaTileEntity metaTileEntity, boolean allowSameFluidFill, List<? extends IFluidTank> fluidTanks) {
        super(allowSameFluidFill, fluidTanks);
        this.setMetaTileEntity(metaTileEntity);
    }

    public NotifiableFluidTankList(MetaTileEntity metaTileEntity, boolean allowSameFluidFill, FluidTankList parent, IFluidTank... fluidTanks) {
        super(allowSameFluidFill, parent, fluidTanks);
        this.setMetaTileEntity(metaTileEntity);
    }

    @Override
    public void setMetaTileEntity(MetaTileEntity metaTileEntity) {
        for (IFluidTank tank : this.fluidTanks) {
            if (tank instanceof IInventoryNotifiable)
                ((IInventoryNotifiable) tank).setMetaTileEntity(metaTileEntity);
        }
    }
}
