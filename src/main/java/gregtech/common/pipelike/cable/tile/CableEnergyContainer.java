package gregtech.common.pipelike.cable.tile;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.common.pipelike.cable.Insulation;
import gregtech.common.pipelike.cable.WireProperties;
import gregtech.common.pipelike.cable.net.EnergyNet;
import gregtech.common.pipelike.cable.net.RoutePath;
import gregtech.common.pipelike.cable.net.WorldENet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.lang.ref.WeakReference;
import java.util.List;

public class CableEnergyContainer implements IEnergyContainer {

    private final IPipeTile<Insulation, WireProperties> tileEntityCable;
    private WeakReference<EnergyNet> currentEnergyNet = new WeakReference<>(null);
    private long lastCachedUpdate;
    private List<RoutePath> pathsCache;

    public CableEnergyContainer(IPipeTile<Insulation, WireProperties> tileEntityCable) {
        this.tileEntityCable = tileEntityCable;
    }

    @Override
    public long acceptEnergyFromNetwork(EnumFacing side, long voltage, long amperage) {
        EnergyNet energyNet = getEnergyNet();
        if (energyNet == null) {
            return 0;
        }

        List<RoutePath> paths = getPaths(energyNet);
        World world = tileEntityCable.getPipeWorld();

        long now = world.getTotalWorldTime();
        long ampsUsed = 0;

        for (RoutePath routePath : paths) {
            if (routePath.totalLoss >= voltage) {
                continue;
            }

            long ampsOnPath = dispatchEnergyToNode(routePath.destination, routePath.destinationNode.blockedConnections,
                    voltage - routePath.totalLoss, amperage - ampsUsed
            );

            ampsUsed += ampsOnPath;

            if (routePath.addAmperageAndBurn(world, now, voltage, ampsOnPath)) {
                break;
            }

            if (ampsUsed == amperage) {
                break;
            }
        }

        energyNet.incrementCurrentAmperage(amperage, voltage);
        return ampsUsed;
    }


    private long dispatchEnergyToNode(BlockPos nodePos, int nodeBlockedConnections, long volt, long amp) {
        long ampsUsed = 0L;
        World world = tileEntityCable.getPipeWorld();
        PooledMutableBlockPos blockPos = PooledMutableBlockPos.retain();

        try {
            for (EnumFacing facing : EnumFacing.VALUES) {
                if ((nodeBlockedConnections & 1 << facing.getIndex()) != 0) {
                    continue;
                }

                blockPos.setPos(nodePos).move(facing);

                Chunk chunk = world.getChunkProvider().getLoadedChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4);
                if (chunk == null) {
                    continue;
                }

                TileEntity tileEntity = chunk.getTileEntity(blockPos, Chunk.EnumCreateEntityType.IMMEDIATE);
                if (tileEntity == null || tileEntityCable.getPipeBlock().getPipeTileEntity(tileEntity) != null) {
                    continue;
                }

                IEnergyContainer energyContainer = tileEntity.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, facing.getOpposite());
                if (energyContainer == null) {
                    continue;
                }

                ampsUsed += energyContainer.acceptEnergyFromNetwork(facing.getOpposite(), volt, amp - ampsUsed);
                if (ampsUsed == amp) {
                    break;
                }
            }
        } finally {
            blockPos.release();
        }

        return ampsUsed;
    }

    @Override
    public long getInputAmperage() {
        return tileEntityCable.getNodeData().amperage;
    }

    @Override
    public long getInputVoltage() {
        return tileEntityCable.getNodeData().voltage;
    }

    @Override
    public long getEnergyCapacity() {
        return getInputVoltage() * getInputAmperage();
    }

    @Override
    public long changeEnergy(long energyToAdd) {
        //just a fallback case if somebody will call this method
        long volt = getInputVoltage();
        if (energyToAdd <= 0 || volt == 0) {
            return 0;
        }

        long amp = energyToAdd / volt;
        if (amp <= 0) {
            return 0;
        }

        return acceptEnergyFromNetwork(EnumFacing.UP, volt, amp) * volt;
    }

    @Override
    public boolean outputsEnergy(EnumFacing side) {
        return true;
    }

    @Override
    public boolean inputsEnergy(EnumFacing side) {
        return true;
    }

    @Override
    public long getEnergyStored() {
        return 0;
    }

    private List<RoutePath> getPaths(EnergyNet energyNet) {
        if (pathsCache == null || energyNet.getLastUpdate() != lastCachedUpdate) {
            lastCachedUpdate = energyNet.getLastUpdate();
            pathsCache = energyNet.computePatches(tileEntityCable.getPipePos());
        }

        return pathsCache;
    }

    private EnergyNet getEnergyNet() {
        EnergyNet currentEnergyNet = this.currentEnergyNet.get();
        if (currentEnergyNet != null && currentEnergyNet.isValid() && currentEnergyNet.containsNode(tileEntityCable.getPipePos())) {
            return currentEnergyNet;  //return current net if it is still valid
        }

        WorldENet worldENet = (WorldENet) tileEntityCable.getPipeBlock().getWorldPipeNet(tileEntityCable.getPipeWorld());
        currentEnergyNet = worldENet.getNetFromPos(tileEntityCable.getPipePos());

        if (currentEnergyNet != null) {
            this.currentEnergyNet = new WeakReference<>(currentEnergyNet);
        }

        return currentEnergyNet;
    }

    @Override
    public boolean isOneProbeHidden() {
        return true;
    }
}
