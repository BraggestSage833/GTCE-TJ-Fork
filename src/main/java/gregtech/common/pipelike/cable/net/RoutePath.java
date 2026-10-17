package gregtech.common.pipelike.cable.net;

import gregtech.api.pipenet.Node;
import gregtech.common.pipelike.cable.WireProperties;
import gregtech.common.pipelike.cable.tile.TileEntityCable;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;


public class RoutePath {

    public final BlockPos destination;
    public final Node<WireProperties> destinationNode;
    public final int totalLoss;
    public final int minVoltage;

    private final RouteTree tree;
    private final int leaf;


    public RoutePath(RouteTree tree, int leaf, Node<WireProperties> destinationNode, BlockPos destination, int totalLoss, int minVoltage) {
        this.tree = tree;
        this.leaf = leaf;
        this.destinationNode = destinationNode;
        this.destination = destination;
        this.totalLoss = totalLoss;
        this.minVoltage = minVoltage;
    }

    public boolean addAmperageAndBurn(World world, long now, long voltage, long amperes) {
        boolean overVoltage = voltage > minVoltage;

        if (amperes <= 0L && !overVoltage) {
            return false;
        }

        return tree.load(leaf, world, now, voltage, amperes, overVoltage);
    }

    public static void burnCable(World world, BlockPos blockPos) {
        if (world.isRemote) {
            return;
        }

        TileEntity tileEntity = world.getTileEntity(blockPos);

        if (tileEntity instanceof TileEntityCable) {
            world.setBlockToAir(blockPos);
            world.setBlockState(blockPos, Blocks.FIRE.getDefaultState());

            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5,
                    5 + world.rand.nextInt(3), 0.0, 0.0, 0.0, 0.1);
        }
    }
}
