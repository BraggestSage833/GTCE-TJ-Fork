package gregtech.common.pipelike.fluidpipe.net;

import gregtech.api.pipenet.RouteStep;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class SplitTree {
    private static final SplitTree EMPTY = new SplitTree(-1, new RouteStep[0], new int[0], new int[0], new boolean[0]);

    private final int root;
    private final RouteStep[] steps;
    private final int[] firstChild;
    private final int[] nextSibling;
    private final boolean[] destination;

    private SplitTree(int root, RouteStep[] steps, int[] firstChild, int[] nextSibling, boolean[] destination) {
        this.root = root;
        this.steps = steps;
        this.firstChild = firstChild;
        this.nextSibling = nextSibling;
        this.destination = destination;
    }

    public boolean isEmpty() {
        return root < 0;
    }

    public int root() {
        return root;
    }

    public BlockPos pos(int node) {
        return steps[node].pos;
    }

    boolean isDestination(int node) {
        return destination[node];
    }


    public int firstChild(int node) {
        return firstChild[node];
    }

    public int nextSibling(int node) {
        return nextSibling[node];
    }

    public int available(int node, long now) {
        return steps[node].available(now);
    }

    public void consume(int node, long now, int amount) {
        steps[node].consume(now, amount);
    }

    static SplitTree build(List<RouteStep> destinations) {
        if (destinations.isEmpty()) {
            return EMPTY;
        }

        // number every pipe on the way from each destination back to the source, once
        Map<RouteStep, Integer> indices = new IdentityHashMap<>();
        List<RouteStep> ordered = new ArrayList<>();
        for (RouteStep destinationStep : destinations) {
            for (RouteStep step = destinationStep; step != null && !indices.containsKey(step); step = step.parent) {
                indices.put(step, ordered.size());
                ordered.add(step);
            }
        }

        int size = ordered.size();
        int[] firstChild = new int[size];
        int[] nextSibling = new int[size];
        int[] lastChild = new int[size];
        Arrays.fill(firstChild, -1);
        Arrays.fill(nextSibling, -1);
        Arrays.fill(lastChild, -1);

        // link each pipe to its parent, keeping children in the order they were numbered
        int root = -1;
        for (int i = 0; i < size; i++) {
            RouteStep parentStep = ordered.get(i).parent;
            if (parentStep == null) {
                root = i;
                continue;
            }

            int parent = indices.get(parentStep);
            if (firstChild[parent] < 0) {
                firstChild[parent] = i;
            } else {
                nextSibling[lastChild[parent]] = i;
            }
            lastChild[parent] = i;
        }

        boolean[] destination = new boolean[size];
        for (RouteStep destinationStep : destinations) {
            destination[indices.get(destinationStep)] = true;
        }

        return new SplitTree(root, ordered.toArray(new RouteStep[0]), firstChild, nextSibling, destination);
    }

}
