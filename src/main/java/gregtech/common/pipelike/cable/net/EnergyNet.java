package gregtech.common.pipelike.cable.net;

import gregtech.api.pipenet.Node;
import gregtech.api.pipenet.PipeNet;
import gregtech.api.pipenet.WorldPipeNet;
import gregtech.api.util.PerTickLongCounter;
import gregtech.common.pipelike.cable.WireProperties;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;


import java.util.*;

public class EnergyNet extends PipeNet<WireProperties> {

    private final PerTickLongCounter currentAmperageCounter = new PerTickLongCounter(0L);
    private final PerTickLongCounter currentMaxVoltageCounter = new PerTickLongCounter(0L);


    private final Map<BlockPos, TickCounter> cableCounters = new HashMap<>();

    protected EnergyNet(WorldPipeNet<WireProperties, EnergyNet> world) {
        super(world);
    }

    public long getLastAmperage() {
        return currentAmperageCounter.getLast(worldData.getWorld());
    }

    public long getLastMaxVoltage() {
        return currentMaxVoltageCounter.getLast(worldData.getWorld());
    }

    public void incrementCurrentAmperage(long amperage, long voltage) {
        currentAmperageCounter.increment(worldData.getWorld(), amperage);
        long currentMaxVoltage = currentMaxVoltageCounter.get(worldData.getWorld());

        if (voltage > currentMaxVoltage) {
            currentMaxVoltageCounter.set(worldData.getWorld(), voltage);
        }
    }

    public List<CableRoutePath> computePaths(BlockPos startPos) {
        Node<WireProperties> rootNode = getNodeAt(startPos);
        List<CableRoutePath> routes = new ArrayList<>();

        if (rootNode == null) {
            return routes;
        }

        int capacity = getAllNodes().size();
        RouteTree tree = new RouteTree(capacity);
        HashSet<BlockPos> settled = new HashSet<>(capacity * 2);
        PriorityQueue<Candidate> queue = new PriorityQueue<>();
        MutableBlockPos probe = new MutableBlockPos();
        int sequence = 0;

        queue.add(new Candidate(startPos, rootNode, -1, lossOf(rootNode), rootNode.data.voltage, 0, sequence++));

        while(!queue.isEmpty()) {
            Candidate candidate = queue.poll();
            if (!settled.add(candidate.pos)) {
                continue; // cheaper path alrd found
            }

            int index = tree.add(candidate.pos, candidate.node.data, candidate.parent);

            if (candidate.parent < 0 || candidate.node.isActive) {
                tree.markUsed(index);
                routes.add(new CableRoutePath(tree, index, candidate.node, candidate.pos, candidate.loss, candidate.minVoltage));
            }

            for (EnumFacing facing : EnumFacing.VALUES) {
                probe.setPos(candidate.pos).move(facing);
                if (settled.contains(probe)) {
                    continue;
                }

                Node<WireProperties> next = getNodeAt(probe);
                if (next == null || !canNodesConnect(candidate.node, facing, next, this)) {
                    continue;
                }

                queue.add(new Candidate(probe.toImmutable(), next, index, candidate.loss + lossOf(next),
                        Math.min(candidate.minVoltage, next.data.voltage), candidate.hops + 1, sequence++)
                );
            }
        }

        tree.resolve(this);
        return routes;
    }

    private static int lossOf(Node<WireProperties> node) {
        return Math.max(0, node.data.lossPerBlock);
    }


    public TickCounter getCableCounter(BlockPos cablePos) {
        return cableCounters.computeIfAbsent(cablePos.toImmutable(), pos -> new TickCounter());
    }

    @Override
    protected Node<WireProperties> removeNodeWithoutRebuilding(BlockPos nodePos) {
        cableCounters.remove(nodePos);
        return super.removeNodeWithoutRebuilding(nodePos);
    }

    @Override
    protected void writeNodeData(WireProperties nodeData, NBTTagCompound tagCompound) {
        tagCompound.setInteger("voltage", nodeData.voltage);
        tagCompound.setInteger("amperage", nodeData.amperage);
        tagCompound.setInteger("loss", nodeData.lossPerBlock);
    }

    @Override
    protected WireProperties readNodeData(NBTTagCompound tagCompound) {
        int voltage = tagCompound.getInteger("voltage");
        int amperage = tagCompound.getInteger("amperage");
        int lossPerBlock = tagCompound.getInteger("loss");
        return new WireProperties(voltage, amperage, lossPerBlock);
    }

    private static final class Candidate implements Comparable<Candidate> {
        final BlockPos pos;
        final Node<WireProperties> node;
        final int parent;
        final int loss;
        final int minVoltage;
        final int hops;
        final int sequence;

        Candidate(BlockPos pos, Node<WireProperties> node, int parent,
                  int loss, int minVoltage, int hops, int sequence) {
            this.pos = pos;
            this.node = node;
            this.parent = parent;
            this.loss = loss;
            this.minVoltage = minVoltage;
            this.hops = hops;
            this.sequence = sequence;
        }

        @Override
        public int compareTo(Candidate other) {
            int result = Integer.compare(loss, other.loss);
            if (result != 0) {
                return result;
            }

            result = Integer.compare(hops, other.hops);
            if (result != 0) {
                return result;
            }

            return Integer.compare(sequence, other.sequence);
        }
    }
}
