package gregtech.api.pipenet.tile;

import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import java.util.function.Consumer;

public interface IPipeTile<PipeType extends Enum<PipeType> & IPipeType<NodeDataType>, NodeDataType> {

    int DEFAULT_INSULATION_COLOR = 0x777777;

    World getPipeWorld();

    BlockPos getPipePos();

    default long getTickTimer() {
        return getPipeWorld().getWorldTime();
    }

    BlockPipe<PipeType, NodeDataType, ?> getPipeBlock();

    void transferDataFrom(IPipeTile<PipeType, NodeDataType> sourceTile);

    int getInsulationColor();

    void setInsulationColor(int newInsulationColor);

    boolean isSideBlocked(EnumFacing side);

    void setSideBlocked(EnumFacing side, boolean blocked);

    default boolean canHaveBlockedSides() {
        return true;
    }

    int getBlockedConnections();


    int getConnections();

    boolean isConnected(EnumFacing side);

    void setConnection(EnumFacing side, boolean connected, boolean fromNeighbor);

    default int getUnconnectedSides() {
        return ~getConnections() & 63;
    }

    default boolean isConnectionEnabled(EnumFacing side) {
        return isConnected(side);
    }

    int getVisualConnections();

    @Deprecated
    default void setConnectionBlocked(AttachmentType type, EnumFacing side, boolean isBlocked) {
        setConnection(side, !isBlocked, false);
    }

    PipeType getPipeType();

    NodeDataType getNodeData();

    PipeCoverableImplementation getCoverableImplementation();

    boolean supportsTicking();

    IPipeTile<PipeType, NodeDataType> setSupportsTicking();

    boolean canPlaceCoverOnSide(EnumFacing side);

    <T> T getCapability(Capability<T> capability, EnumFacing side);

    <T> T getCapabilityInternal(Capability<T> capability, EnumFacing side);

    void notifyBlockUpdate();

    void writeCoverCustomData(int id, Consumer<PacketBuffer> writer);

    void markAsDirty();

    boolean isValidTile();

    void scheduleChunkForRenderUpdate();
}
