package gregtech.api.pipenet.tile;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.metatileentity.SyncedTileEntityBase;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public abstract class TileEntityPipeBase<PipeType extends Enum<PipeType> & IPipeType<NodeDataType>, NodeDataType> extends SyncedTileEntityBase implements IPipeTile<PipeType, NodeDataType> {

    private int connections = 0;
    private boolean connectionsInitialised = false; // convert old cables for new types.


    protected int insulationColor = DEFAULT_INSULATION_COLOR;
    protected final PipeCoverableImplementation coverableImplementation = new PipeCoverableImplementation(this);
    private NodeDataType cachedNodeData;
    private BlockPipe<PipeType, NodeDataType, ?> pipeBlock;
    private PipeType pipeType = getPipeTypeClass().getEnumConstants()[0];
    private boolean detachedConversionMode;
    private boolean wasInDetachedConversionMode;

    public TileEntityPipeBase() {
    }

    public void setDetachedConversionMode(boolean detachedConversionMode) {
        this.detachedConversionMode = detachedConversionMode;
        this.wasInDetachedConversionMode = true;
    }

    public boolean wasInDetachedConversionMode() {
        return wasInDetachedConversionMode;
    }

    public void setPipeData(BlockPipe<PipeType, NodeDataType, ?> pipeBlock, PipeType pipeType) {
        this.pipeBlock = pipeBlock;
        this.pipeType = pipeType;
        if(!getWorld().isRemote) {
            writeCustomData(-4, this::writePipeProperties);
        }
    }

    @Override
    public void transferDataFrom(IPipeTile<PipeType, NodeDataType> tileEntity) {
        this.pipeType = tileEntity.getPipeType();
        this.insulationColor = tileEntity.getInsulationColor();
        this.connections = tileEntity.getConnections();
        if (tileEntity instanceof TileEntityPipeBase<?,?> other) {
            this.connectionsInitialised = other.connectionsInitialised;
            this.updates.putAll(other.updates);
        } else {
            this.connectionsInitialised = true;
        }

        tileEntity.getCoverableImplementation().transferDataTo(coverableImplementation);
    }

    public abstract Class<PipeType> getPipeTypeClass();

    @Override
    public abstract boolean supportsTicking();

    @Override
    public World getPipeWorld() {
        return getWorld();
    }

    @Override
    public BlockPos getPipePos() {
        return getPos();
    }

    @Override
    public PipeCoverableImplementation getCoverableImplementation() {
        return coverableImplementation;
    }

    @Override
    public boolean canPlaceCoverOnSide(EnumFacing side) {
        return true;
    }

    @Override
    public IPipeTile<PipeType, NodeDataType> setSupportsTicking() {
        if (supportsTicking()) {
            return this;
        }
        //create new tickable tile entity, transfer data, and replace it
        IPipeTile<PipeType, NodeDataType> newTile = getPipeBlock().createNewTileEntity(true);
        newTile.transferDataFrom(this);
        getWorld().setTileEntity(getPos(), (TileEntity) newTile);
        return newTile;
    }

    @Override
    public BlockPipe<PipeType, NodeDataType, ?> getPipeBlock() {
        if (pipeBlock == null) {
            Block block = getBlockState().getBlock();
            //noinspection unchecked
            this.pipeBlock = block instanceof BlockPipe ? (BlockPipe<PipeType, NodeDataType, ?>) block : null;
        }
        return pipeBlock;
    }

    @Override
    public int getConnections() {
        return connections;
    }

    @Override
    public boolean isConnected(EnumFacing side) {
        return (connections & (1 << side.getIndex())) != 0;
    }

    @Override
    public void setConnection(EnumFacing side, boolean connected, boolean fromNeighbor) {
        World world = getWorld();
        if (world.isRemote || isConnected(side) == connected) {
            return;
        }

        int bit = 1 << side.getIndex();
        connections = connected ? (connections | bit) : (connections & ~bit);
        writeCustomData(-2, buffer -> buffer.writeVarInt(connections));
        markDirty();

        if (connected) {
            new Throwable("[connect] " + getPos() + " side=" + side + " fromNeighbor=" + fromNeighbor).printStackTrace();
        }

        if (!detachedConversionMode) {
            getPipeBlock().getWorldPipeNet(world).updateBlockedConnections(getPos(),side,!connected);
            notifyBlockUpdate();
        }

        if (!fromNeighbor) {
            IPipeTile<PipeType, NodeDataType> neighbor = getPipeBlock().getPipeTileEntity(world,getPos().offset(side));
            if (neighbor != null) {
                neighbor.setConnection(side.getOpposite(), connected, true);
            }
        }

    }

    public void markConnectionInitialised() {
        this.connectionsInitialised = true;
        markDirty();
    }

    public void initialiseConnections() {
        if (connectionsInitialised) {
            return;
        }

        connectionsInitialised = true;
        World world = getWorld();
        BlockPipe<PipeType, NodeDataType, ?> block = getPipeBlock();

        if (world.isRemote || block == null) {
            return;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            if (block.canAutoConnect(this, side)) {
                System.out.println("[autoconnect] side=" + side + " neighbor="
                        + world.getBlockState(getPos().offset(side)).getBlock().getRegistryName());
                setConnection(side, true, false);
            }
        }
        markDirty();
    }


    public int getVisualConnections() {
        BlockPipe<PipeType, NodeDataType, ?> block = getPipeBlock();
        if (block == null || world == null) {
            return 0;
        }

        int activeNodeConnections = block.getActiveVisualConnections(this);
        float thickness = getPipeType().getThickness();
        int result = 0;

        for (EnumFacing side : EnumFacing.VALUES) {
            int index = side.getIndex();
            boolean connected = isConnected(side);
            CoverBehavior cover = getCoverableImplementation().getCoverAtSide(side);

            if (!connected && (cover == null || !cover.shouldRenderConnected())) {
                continue;
            }

            result |= 1 << index;

            if (connected) {
                IPipeTile<PipeType, NodeDataType> other = block.getPipeTileEntity(world, pos.offset(side));
                if (other != null) {
                    if (other.isConnected(side.getOpposite()) &&
                            other.getPipeType().getThickness() < thickness) {
                        result |= 1 << (index + 6);
                    }
                } else if ((activeNodeConnections & (1 << index)) != 0) {
                    result |= 1 << (index + 6);
                }

            }

            if (cover != null) {
                result |= 1 << (index + 12);
            }

        }

        return result;
    }


    @Override
    public int getInsulationColor() {
        return insulationColor;
    }

    @Override
    public void setInsulationColor(int insulationColor) {
        this.insulationColor = insulationColor;
        if (!getWorld().isRemote) {
            if (!detachedConversionMode) {
                getPipeBlock().getWorldPipeNet(getWorld()).updateMark(getPos(), getCableMark());
            }
            writeCustomData(-1, buffer -> buffer.writeInt(insulationColor));
            markDirty();
        }
    }

    @Override
    public PipeType getPipeType() {
        return pipeType;
    }

    @Override
    public NodeDataType getNodeData() {
        if (cachedNodeData == null) {
            this.cachedNodeData = getPipeBlock().createProperties(this);
        }
        return cachedNodeData;
    }

    private int getCableMark() {
        return insulationColor == DEFAULT_INSULATION_COLOR ? 0 : insulationColor;
    }

    public <T> T getCapabilityInternal(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == GregtechTileCapabilities.CAPABILITY_COVERABLE) {
            return GregtechTileCapabilities.CAPABILITY_COVERABLE.cast(getCoverableImplementation());
        }
        return super.getCapability(capability, facing);
    }

    @Nullable
    @Override
    public final <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        boolean isCoverable = capability == GregtechTileCapabilities.CAPABILITY_COVERABLE;
        CoverBehavior coverBehavior = facing == null ? null : coverableImplementation.getCoverAtSide(facing);
        T defaultValue = getCapabilityInternal(capability, facing);
        if(isCoverable) {
            return defaultValue;
        }
        if(coverBehavior == null && facing != null) {
          return isConnectionEnabled(facing) ? defaultValue : null;
        }
        if (coverBehavior != null) {
            return coverBehavior.getCapability(capability, defaultValue);
        }
        return defaultValue;
    }

    @Override
    public final boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return getCapability(capability, facing) != null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        BlockPipe<PipeType, NodeDataType, ?> pipeBlock = getPipeBlock();
        if (pipeBlock != null) {
            //noinspection ConstantConditions
            compound.setString("PipeBlock", pipeBlock.getRegistryName().toString());
        }

        compound.setInteger("PipeType", pipeType.ordinal());

        if (connectionsInitialised) {
            compound.setInteger("Connections", connections);
        }

        compound.setInteger("InsulationColor", insulationColor);
        this.coverableImplementation.writeToNBT(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("PipeBlock", NBT.TAG_STRING)) {
            Block block = Block.REGISTRY.getObject(new ResourceLocation(compound.getString("PipeBlock")));
            //noinspection unchecked
            this.pipeBlock = block instanceof BlockPipe ? (BlockPipe<PipeType, NodeDataType, ?>) block : null;
        }

        this.pipeType = getPipeTypeClass().getEnumConstants()[compound.getInteger("PipeType")];

        if (compound.hasKey("Connections", NBT.TAG_INT)) {
            this.connections = compound.getInteger("Connections") & 63;
            this.connectionsInitialised = true;
        } else {
            this.connections = 0;
            this.connectionsInitialised = false;
        }

        this.insulationColor = compound.getInteger("InsulationColor");
        this.coverableImplementation.readFromNBT(compound);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.coverableImplementation.onLoad();

        if (!getWorld().isRemote && !connectionsInitialised) {
            getWorld().scheduleUpdate(getPos(), getBlockType(), 1);
        }
    }

    protected void writePipeProperties(PacketBuffer buf) {
        buf.writeVarInt(pipeType.ordinal());
    }

    protected void readPipeProperties(PacketBuffer buf) {
        this.pipeType = getPipeTypeClass().getEnumConstants()[buf.readVarInt()];
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        writePipeProperties(buf);
        buf.writeVarInt(connections);
        buf.writeInt(insulationColor);
        this.coverableImplementation.writeInitialSyncData(buf);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        readPipeProperties(buf);
        this.connections = buf.readVarInt();
        this.insulationColor = buf.readInt();
        this.coverableImplementation.readInitialSyncData(buf);
    }

    @Override
    public void receiveCustomData(int discriminator, PacketBuffer buf) {
        if (discriminator == -1) {
            this.insulationColor = buf.readInt();
            scheduleChunkForRenderUpdate();
        } else if (discriminator == -2) {
            this.connections = buf.readVarInt();
            scheduleChunkForRenderUpdate();
        } else if (discriminator == -3) {
            this.coverableImplementation.readCustomData(buf.readVarInt(), buf);
        } else if(discriminator == -4) {
            readPipeProperties(buf);
            scheduleChunkForRenderUpdate();
        }
    }

    @Override
    public void writeCoverCustomData(int id, Consumer<PacketBuffer> writer) {
        writeCustomData(-3, buffer -> {
            buffer.writeVarInt(id);
            writer.accept(buffer);
        });
    }

    @Override
    public void scheduleChunkForRenderUpdate() {
        BlockPos pos = getPos();
        getWorld().markBlockRangeForRenderUpdate(
            pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    @Override
    public void notifyBlockUpdate() {
        getWorld().notifyNeighborsOfStateChange(getPos(), getBlockType(), true);
        getPipeBlock().updateActiveNodeStatus(getWorld(), getPos(), this);
    }

    @Override
    public void markAsDirty() {
        markDirty();
    }

    @Override
    public boolean isValidTile() {
        return !isInvalid();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }
}
