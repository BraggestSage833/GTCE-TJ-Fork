package gregtech.common.pipelike.cable.net;


import gregtech.common.pipelike.cable.WireProperties;
import gregtech.common.pipelike.cable.tile.TileEntityCable;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

public final class RouteTree {
    public final BlockPos[] positions;
    public final int[] parent;
    public final int[] maxVolts;
    public final int[] maxAmps;
    private final boolean[] used;
    private TickCounter[] counters;
    public int size;

    public RouteTree(int capacity) {
        this.positions = new BlockPos[capacity];
        this.parent = new int[capacity];
        this.maxAmps = new int[capacity];
        this.maxVolts = new int[capacity];
        this.used = new boolean[capacity];
    }

    public int add(BlockPos pos, WireProperties data, int parentIndex) {
        int index = size++;
        positions[index] = pos;
        parent[index] = parentIndex;
        maxAmps[index] = data.amperage;
        maxVolts[index] = data.voltage;
        return index;
    }

    public void markUsed(int index) {
        for (; index >= 0 && !used[index]; index = parent[index]) {
            used[index] = true;
        }
    }


    public void resolve(EnergyNet net) {
        counters = new TickCounter[size];
        for (int i = 0; i < size; i++) {
            if (used[i]) {
                counters[i] = net.getCableCounter(positions[i]);
            }
        }
    }


    public boolean load(int leaf, World world, long now, long volt, long amps, boolean overVoltage) {
        long add = Math.max(amps, 0L);
        List<BlockPos> overloaded = null;

        for (int i = leaf; i >= 0; i = parent[i]) {
            long total = counters[i].add(now, add);
            if (total > maxAmps[i] || (overVoltage && volt > maxVolts[i])) {
                if (overloaded == null) {
                    overloaded = new ArrayList<>(4);
                }
                overloaded.add(positions[i]);
            }
        }

        if (overloaded == null) {
            return false;
        }

        for (BlockPos pos : overloaded) {
            burnCable(world, pos);
        }
        return true;
    }

    private static void burnCable(World world, BlockPos pos) {
        if (world.isRemote) {
            return;
        }

        if (world.getTileEntity(pos) instanceof TileEntityCable) {
            world.setBlockToAir(pos);
            world.setBlockState(pos, Blocks.FIRE.getDefaultState());

            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    5 + world.rand.nextInt(3), 0.0, 0.0, 0.0, 0.1);
        }
    }
}
