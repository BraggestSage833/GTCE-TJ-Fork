package gregtech.common.pipelike.fluidpipe.net;

import gregtech.api.pipenet.*;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.common.pipelike.fluidpipe.FluidPipeProperties;
import gregtech.common.pipelike.fluidpipe.tile.TileEntityFluidPipe;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.*;

public class FluidPipeNet extends RoutedPipeNet<FluidPipeProperties> {

    private static final int PIPE_CAN_HURT_TICKS = 100; // 5sec

    public static final int NO_RECENT_FLUID = Integer.MIN_VALUE;

    public static int HOT_VALUE = 373;
    public static int COLD_VALUE = 183;


    private final Map<BlockPos, RecentFluid> recentFluid = new HashMap<>();
    private final Map<BlockPos, SplitTree> treeCache = new HashMap<>();

    public FluidPipeNet(WorldPipeNet<FluidPipeProperties, FluidPipeNet> world) {
        super(world);
    }


    @Override
    protected int getCost(Node<FluidPipeProperties> node) {
       return 1;
    }

    @Override
    protected int getLimit(Node<FluidPipeProperties> node) {
        return node.data.throughput;
    }

    @Override
    public void invalidateRoutes() {
        treeCache.clear();
        super.invalidateRoutes();
    }


    protected SplitTree getSplitTree(BlockPos sourcePipePos) {
        return treeCache.computeIfAbsent(sourcePipePos.toImmutable(), pos -> SplitTree.build(computeRoutes(pos)));
    }


    public int fill(BlockPos sourcePipePos, @Nullable EnumFacing sourceSide, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || getNodeAt(sourcePipePos) == null) {
            return 0;
        }

        SplitTree tree = getSplitTree(sourcePipePos);
        if (tree.isEmpty()) {
            return 0;
        }

        World world = worldData.getWorld();
        BlockPos sourceInvPos = sourceSide == null ? null : sourcePipePos.offset(sourceSide);
        Flow flow = new Flow(world, tree, resource, !doFill, sourceInvPos);

        int accepted = distribute(flow, resource.amount);

        flow.undo.forEach(Runnable::run);

        if (doFill) {
            for (Failure failure : flow.failures) {
                destroyPipe(world, failure);
            }
        }

        return accepted;
    }


    private int distribute(Flow flow, int offered) {
        Frame first = open(flow, flow.tree.root(), offered);
        if (first == null) {
            return 0;
        }

        List<Frame> stack = new ArrayList<>();
        stack.add(first);

        while (true) {
            Frame frame = stack.get(stack.size() - 1);

            if (!frame.done) {
                Frame child = step(flow, frame);
                if (child != null) {
                    stack.add(child);
                    continue;
                }
            }

            stack.remove(stack.size() - 1);
            int moved = close(flow, frame);
            if (stack.isEmpty()) {
                return moved;
            }

            Frame parent = stack.get(stack.size() - 1);
            parent.remaining -= moved;
            if (moved < parent.pendingGive) {
                flow.exhausted.add(parent.pendingChild);
                parent.state.activeChildren--;
            }
        }
    }

    @Nullable
    private Frame open(Flow flow, int node, int offered) {
        if (flow.aborted) {
            return null;
        }

        int allowed = Math.min(offered, flow.tree.available(node, flow.now));
        if (allowed <= 0) {
            return null;
        }

        NodeState state = stateOf(flow, node);
        Frame frame = new Frame(node, state, allowed);


        if (state.failure != null) {
            flow.failures.add(state.failure);
            flow.aborted = true;
            frame.lost = true;
            frame.done = true;
        }

        return frame;
    }


    @Nullable
    private Frame step(Flow flow, Frame frame) {
        SplitTree tree = flow.tree;
        NodeState state = frame.state;

        while (frame.remaining > 0 && !flow.aborted) {
            if (!frame.inRound) {
                int branches = state.tanks.size() + state.activeChildren;
                if (branches == 0) {
                    break;
                }

                frame.share = Math.max(1, frame.remaining / branches);
                frame.tanks = state.tanks.iterator();
                frame.pipesStarted = false;
                frame.inRound = true;
            }

            if (frame.tanks.hasNext()) {
                Tank tank = frame.tanks.next();
                int give = Math.min(frame.share, frame.remaining);
                int accepted = fillTank(flow, tank, give);
                frame.remaining -= accepted;
                if (accepted < give) {
                    frame.tanks.remove();
                }
                continue;
            }

            if (!frame.pipesStarted) {
                frame.cursor = tree.firstChild(frame.node);
                frame.pipesStarted = true;
            }

            while (frame.cursor >= 0 && flow.exhausted.contains(frame.cursor)) {
                frame.cursor = tree.nextSibling(frame.cursor);
            }

            if (frame.cursor >= 0) {
                int pipe = frame.cursor;
                frame.cursor = tree.nextSibling(pipe);

                int give = Math.min(frame.share, frame.remaining);
                Frame child = open(flow, pipe, give);
                if (child == null) {
                    flow.exhausted.add(pipe);
                    state.activeChildren--;
                    continue;
                }

                frame.pendingChild = pipe;
                frame.pendingGive = give;
                return child;
            }

            frame.inRound = false;
        }

        frame.done = true;
        return null;
    }


    private int close(Flow flow, Frame frame) {
        int moved = frame.lost ? frame.allowed : frame.allowed - frame.remaining;

        if (moved > 0) {
            SplitTree tree = flow.tree;
            int node = frame.node;
            tree.consume(node, flow.now, moved);
            if (flow.simulate) {
                flow.undo.add(() -> tree.consume(node, flow.now, -moved));
            } else {
                markRecentFluid(tree.pos(node), flow.temperature, flow.now);
            }
        }

        return moved;
    }


    private NodeState stateOf(Flow flow, int node) {
        NodeState state = flow.states.get(node);
        if (state != null) {
            return state;
        }

        SplitTree tree = flow.tree;
        BlockPos pos = tree.pos(node);

        List<Tank> tanks = Collections.emptyList();
        if (tree.isDestination(node)) {
            tanks = new ArrayList<>(2);
            collectTanks(flow, pos, tanks);
        }

        int children = 0;
        for (int child = tree.firstChild(node); child >= 0; child = tree.nextSibling(child)) {
            children++;
        }

        state = new NodeState(tanks, children, findFailure(flow, pos));
        flow.states.put(node, state);
        return state;
    }

    private void collectTanks(Flow flow, BlockPos pipePos, List<Tank> out) {
        World world = flow.world;
        if (!world.isBlockLoaded(pipePos)) {
            return;
        }

        if (!(world.getTileEntity(pipePos) instanceof IPipeTile<?, ?> pipe)) {
            return;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            if (!pipe.isConnected(side)) {
                continue;
            }

            BlockPos neighbourPos = pipePos.offset(side);
            if (neighbourPos.equals(flow.sourceInvPos) || !world.isBlockLoaded(neighbourPos)) {
                continue;
            }

            if (!flow.seen.add(neighbourPos)) {
                continue;
            }

            TileEntity neighbour = world.getTileEntity(neighbourPos);
            if (neighbour == null || neighbour instanceof IPipeTile) {
                continue;
            }

            IFluidHandler handler = neighbour.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
            if (handler != null) {
                out.add(new Tank(handler));
            }
        }
    }


    private int fillTank(Flow flow, Tank tank, int give) {
        if (!flow.simulate) {
            return tank.handler.fill(new FluidStack(flow.resource, give), true);
        }

        int total = tank.handler.fill(new FluidStack(flow.resource, tank.promised + give), false);
        int accepted = Math.max(0, Math.min(give, total - tank.promised));
        tank.promised += accepted;
        return accepted;
    }



    private void markRecentFluid(BlockPos pos, int temp, long now) {
        if (temp < HOT_VALUE && temp > COLD_VALUE) {
            return;
        }

        recentFluid.put(pos.toImmutable(), new RecentFluid(temp, now + PIPE_CAN_HURT_TICKS));
    }

    public int getRecentTemp(BlockPos pos, long now) {
        RecentFluid fluid = recentFluid.get(pos);
        if (fluid == null) {
            return NO_RECENT_FLUID;
        }

        if (fluid.until < now) {
            recentFluid.remove(pos);
            return NO_RECENT_FLUID;
        }

        return fluid.temperature;
    }


    @Override
    protected Node<FluidPipeProperties> removeNodeWithoutRebuilding(BlockPos pos) {
        recentFluid.remove(pos);
        return super.removeNodeWithoutRebuilding(pos);
    }

    private Failure findFailure(Flow flow, BlockPos pos) {
        Node<FluidPipeProperties> node = getNodeAt(pos);
        if (node == null) {
            return null;
        }

        boolean leaking = flow.isGas && !node.data.gasProof;
        boolean burning = flow.temperature > node.data.maxFluidTemperature;
        return leaking || burning ? new Failure(pos.toImmutable(), leaking, burning) : null;
    }


    private static void destroyPipe(World world, Failure failure) {
        BlockPos pos = failure.pos;
        if (!world.isBlockLoaded(pos)) {
            return;
        }

        Random rand = world.rand;

        if (world.getTileEntity(pos) instanceof TileEntityFluidPipe) {
            if (failure.burning) {
                world.setBlockState(pos, Blocks.FIRE.getDefaultState());
            } else {
                world.setBlockToAir(pos);
            }
        }

        if (failure.burning) {
            TileEntityFluidPipe.spawnParticles(world, pos, EnumFacing.UP, EnumParticleTypes.FLAME, 3 + rand.nextInt(2), rand);
            if (rand.nextInt(4) == 0) {
                TileEntityFluidPipe.setNeighboursToFire(world, pos);
            }
        }

        if (failure.leaking && rand.nextInt(failure.burning ? 3 : 7) == 0) {
            world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.f + rand.nextFloat(), false);
        }
    }


    @Override
    protected void writeNodeData(FluidPipeProperties nodeData, NBTTagCompound tagCompound) {
        tagCompound.setInteger("max_temperature", nodeData.maxFluidTemperature);
        tagCompound.setInteger("throughput", nodeData.throughput);
        tagCompound.setBoolean("gas_proof", nodeData.gasProof);
    }

    @Override
    protected FluidPipeProperties readNodeData(NBTTagCompound tagCompound) {
        int maxTemperature = tagCompound.getInteger("max_temperature");
        int throughput = tagCompound.getInteger("throughput");
        boolean gasProof = tagCompound.getBoolean("gas_proof");
        return new FluidPipeProperties(maxTemperature, throughput, gasProof);
    }

    private static final class Flow {
        final World world;
        final SplitTree tree;
        final long now;
        final FluidStack resource;
        final boolean simulate;

        @Nullable
        final BlockPos sourceInvPos;
        final boolean isGas;
        final int temperature;

        final Set<BlockPos> seen = new HashSet<>();
        // pipes visited so far, and the child pipes that already dropped out of their parent's split
        final Int2ObjectOpenHashMap<NodeState> states = new Int2ObjectOpenHashMap<>();
        final IntOpenHashSet exhausted = new IntOpenHashSet();
        final List<Runnable> undo = new ArrayList<>();
        final List<Failure> failures = new ArrayList<>();
        boolean aborted;

        Flow(World world, SplitTree tree, FluidStack resource, boolean simulate, @Nullable BlockPos sourceInvPos) {
            this.world = world;
            this.tree = tree;
            this.now = world.getTotalWorldTime();
            this.resource = resource;
            this.simulate = simulate;
            this.sourceInvPos = sourceInvPos;
            this.isGas = resource.getFluid().isGaseous(resource);
            this.temperature = resource.getFluid().getTemperature(resource);
        }
    }


    private static final class Frame {
        final int node;
        final NodeState state;
        final int allowed;
        int remaining;
        int share;
        int pendingChild;
        int pendingGive;
        Iterator<Tank> tanks;
        int cursor;
        boolean inRound;
        boolean pipesStarted;
        boolean done;
        boolean lost;

        Frame(int node, NodeState state, int allowed) {
            this.node = node;
            this.state = state;
            this.allowed = allowed;
            this.remaining = allowed;
        }
    }


    private static final class NodeState {
        final List<Tank> tanks;
        int activeChildren;
        @Nullable
        final Failure failure;

        NodeState(List<Tank> tanks, int activeChildren, @Nullable Failure failure) {
            this.tanks = tanks;
            this.activeChildren = activeChildren;
            this.failure = failure;
        }
    }


    private static final class Tank {
        final IFluidHandler handler;
        // simulated fills only: how much this call has already promised to the tank
        int promised;

        Tank(IFluidHandler handler) {
            this.handler = handler;
        }
    }


    private static final class RecentFluid {
        final int temperature;
        final long until;

        RecentFluid(int temperature, long until) {
            this.temperature = temperature;
            this.until = until;
        }
    }

    private static final class Failure {
        final BlockPos pos;
        final boolean leaking;
        final boolean burning;

        Failure(BlockPos pos, boolean leaking, boolean burning) {
            this.pos = pos;
            this.leaking = leaking;
            this.burning = burning;
        }
    }
}
