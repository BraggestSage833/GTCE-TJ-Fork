package gregtech.api.capability.impl;

import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class FilteredFluidHandler extends NotifiableFluidTank {

    private Predicate<FluidStack> fillPredicate;

    public FilteredFluidHandler(int capacity) {
        this(null, capacity);
    }

    public FilteredFluidHandler(@Nullable MetaTileEntity metaTileEntity, int capacity) {
        super(metaTileEntity, capacity);
    }

    public FilteredFluidHandler(@Nullable MetaTileEntity metaTileEntity, @Nullable FluidStack fluidStack, int capacity) {
        super(metaTileEntity, fluidStack, capacity);
    }

    public FilteredFluidHandler(Fluid fluid, int amount, int capacity) {
        this(null, fluid, amount, capacity);
    }

    public FilteredFluidHandler(@Nullable MetaTileEntity metaTileEntity, Fluid fluid, int amount, int capacity) {
        super(metaTileEntity, fluid, amount, capacity);
    }

    public FilteredFluidHandler setFillPredicate(Predicate<FluidStack> predicate) {
        this.fillPredicate = predicate;
        return this;
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return canFill() && (fillPredicate == null || fillPredicate.test(fluid));
    }
}
