package gregtech.common.pipelike.cable.net;


import gregtech.common.pipelike.cable.WireProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
            RoutePath.burnCable(world, pos);
        }

        return true;
    }
}
