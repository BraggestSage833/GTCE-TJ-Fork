package gregtech.common.pipelike.fluidpipe.tile;

import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.common.pipelike.fluidpipe.FluidPipeProperties;
import gregtech.common.pipelike.fluidpipe.FluidPipeType;
import gregtech.common.pipelike.fluidpipe.net.FluidPipeNet;
import gregtech.common.pipelike.fluidpipe.net.WorldFluidPipeNet;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class FluidPipeFluidHandler implements IFluidHandler {

    private final IPipeTile<FluidPipeType, FluidPipeProperties> pipeTile;

    @Nullable
    private final EnumFacing side;
    private WeakReference<FluidPipeNet> currentPipeNet = new WeakReference<>(null);

    public FluidPipeFluidHandler(IPipeTile<FluidPipeType, FluidPipeProperties> pipeTile, EnumFacing side) {
        this.pipeTile = pipeTile;
        this.side = side;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[] { new FluidTankProperties(null, pipeTile.getNodeData().throughput, true, false)};
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) {
            return 0;
        }

        if (side != null && pipeTile.isSideBlocked(side)) {
            return 0;
        }

        FluidPipeNet net = getFluidPipeNet();
        if (net == null) {
            return 0;
        }

        return net.fill(pipeTile.getPipePos(), side, resource, doFill);
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        return null;
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return null;
    }

    @Nullable
    private FluidPipeNet getFluidPipeNet() {
        FluidPipeNet cached = this.currentPipeNet.get();
        if (cached != null && cached.isValid() && cached.containsNode(pipeTile.getPipePos())) {
            return cached;
        }

        WorldFluidPipeNet worldNet = (WorldFluidPipeNet) pipeTile.getPipeBlock().getWorldPipeNet(pipeTile.getPipeWorld());
        FluidPipeNet found = worldNet.getNetFromPos(pipeTile.getPipePos());
        if (found != null) {
            this.currentPipeNet = new WeakReference<>(found);
        }

        return found;
    }


}
