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

    private final Map<BlockPos, TickCounter> cableAmperage = new HashMap<>();

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

    public List<RoutePath> computePatches(BlockPos startPos) {
        Node<WireProperties> rootNode = getNodeAt(startPos);
        List<RoutePath> routes = new ArrayList<>();

        if (rootNode == null) {
            return routes;
        }

        int capacity = getAllNodes().size();
        RouteTree tree = new RouteTree(capacity);
        HashSet<BlockPos> settled = new HashSet<>(capacity * 2);
        PriorityQueue<Candidate> queue = new PriorityQueue<>();
        MutableBlockPos probe = new MutableBlockPos();
        int sequence = 0;

        queue.add(new Candidate(startPos, rootNode, -1, Math.max(0,rootNode.data.lossPerBlock), rootNode.data.voltage, 0, sequence++));

        while(!queue.isEmpty()) {
            Candidate candidate = queue.poll();
            if (!settled.add(candidate.pos)) {
                continue; // cheaper path alrd found
            }

            int index = tree.add(candidate.pos, candidate.node.data, candidate.parent);

            if (candidate.parent < 0 || candidate.node.isActive) {
                tree.markUsed(index);
                routes.add(new RoutePath(tree, index, candidate.node, candidate.pos, candidate.loss, candidate.minVoltage));
            }

            for (EnumFacing facing : EnumFacing.VALUES) {
                probe.setPos(candidate.pos).move(facing);
                if (settled.contains(probe)) {
                    continue;
                }

                Node<WireProperties> secondNode = getNodeAt(probe);
                if (secondNode == null || !canNodesConnect(candidate.node, facing, secondNode, this)) {
                    continue;
                }

                queue.add(new Candidate(probe.toImmutable(), secondNode, index, candidate.loss + Math.max(0, secondNode.data.lossPerBlock),
                        Math.min(candidate.minVoltage, secondNode.data.voltage), candidate.hops + 1, sequence++)
                );
            }
        }

        tree.resolve(this);
        return routes;
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

    public TickCounter getCableCounter(BlockPos cablePos) {
        TickCounter counter = cableAmperage.get(cablePos);
        if (counter == null) {
            counter = new TickCounter();
            cableAmperage.put(cablePos.toImmutable(), counter);
        }

        return counter;
    }

    @Override
    protected Node<WireProperties> removeNodeWithoutRebuilding(BlockPos nodePos) {
        cableAmperage.remove(nodePos);
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
}
