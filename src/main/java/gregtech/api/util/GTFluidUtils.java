package gregtech.api.util;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class GTFluidUtils {

    public static int transferFluids(@Nonnull IFluidHandler sourceHandler, @Nonnull IFluidHandler destHandler, int transferLimit) {
        return transferFluids(sourceHandler, destHandler, transferLimit, fluidStack -> true);
    }

    public static int transferFluids(@Nonnull IFluidHandler sourceHandler, @Nonnull IFluidHandler destHandler, int transferLimit, @Nonnull Predicate<FluidStack> fluidFilter) {
        int fluidLeftToTransfer = transferLimit;

        for (IFluidTankProperties tankProperties : sourceHandler.getTankProperties()) {
            FluidStack currentFluid = tankProperties.getContents();
            if (currentFluid == null || currentFluid.amount == 0 || !fluidFilter.test(currentFluid)) {
                continue;
            }

            currentFluid.amount = fluidLeftToTransfer;
            FluidStack fluidStack = sourceHandler.drain(currentFluid, false);
            if (fluidStack == null || fluidStack.amount == 0) {
                continue;
            }

            int canInsertAmount = destHandler.fill(fluidStack, false);
            if (canInsertAmount > 0) {
                fluidStack.amount = canInsertAmount;
                fluidStack = sourceHandler.drain(fluidStack, true);
                if (fluidStack != null && fluidStack.amount > 0) {
                    destHandler.fill(fluidStack, true);

                    fluidLeftToTransfer -= fluidStack.amount;
                    if (fluidLeftToTransfer == 0) {
                        break;
                    }
                }
            }
        }
        return transferLimit - fluidLeftToTransfer;
    }

    public static boolean transferExactFluidStack(@Nonnull IFluidHandler sourceHandler, @Nonnull IFluidHandler destHandler, FluidStack fluidStack) {
        int amount = fluidStack.amount;
        FluidStack sourceFluid = sourceHandler.drain(fluidStack, false);
        if (sourceFluid == null || sourceFluid.amount != amount) {
            return false;
        }
        int canInsertAmount = destHandler.fill(sourceFluid, false);
        if (canInsertAmount == amount) {
            sourceFluid = sourceHandler.drain(sourceFluid, true);
            if (sourceFluid != null && sourceFluid.amount > 0) {
                destHandler.fill(sourceFluid, true);
                return true;
            }
        }
        return false;
    }

    public static boolean simulateFluidStackMerge(List<FluidStack> fluids, IFluidHandler handler) {
        boolean filledAll = true;
        for (FluidStack stack : fluids) {
            int filled = handler.fill(stack, false);
            filledAll &= filled == stack.amount;
            if (!filledAll) return false;
        }
        return filledAll;
    }

    public static IMultipleTankHandler createTankHandlerFromList(List<FluidStack> fluidStacks) {
        List<IFluidTank> tanks = new ArrayList<>();
        for (FluidStack fluidStack : fluidStacks) {
            tanks.add(new FluidTank(fluidStack, Integer.MAX_VALUE));
        }
        return new FluidTankList(true, tanks);
    }

    public static boolean findFluidFromTanks(IMultipleTankHandler tanks, FluidStack fluidStack) {
        for (int i = 0; i < tanks.getTanks(); i++) {
            IFluidTank tank = tanks.getTankAt(i);
            if (tank.getFluid() != null && tank.getFluid().isFluidEqual(fluidStack))
                return true;
        }
        return false;
    }
}
