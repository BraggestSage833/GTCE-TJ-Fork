package gregtech.api.pipenet;


import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import net.minecraft.util.math.BlockPos;

public final class RouteStep {
    public final BlockPos pos;
    public final Bucket bucket;
    public final int limit;

    @Nullable
    public final RouteStep parent;


    RouteStep(BlockPos pos, Bucket bucket, int limit, @Nullable RouteStep parent) {
        this.pos = pos;
        this.bucket = bucket;
        this.limit = limit;
        this.parent = parent;
    }

    public int available(long now) {
        return bucket.available(now, limit);
    }

    public void consume(long now, int amount) {
        bucket.consume(now, amount);
    }

    public int remainingCapacity(long now) {
        int remaining = Integer.MAX_VALUE;
        for (RouteStep step = this; step != null; step = step.parent) {
            remaining = Math.min(remaining, step.available(now));
            if (remaining <= 0) {
                return 0;
            }
        }

        return remaining;
    }

    public void commit(long now, int amount) {
        for (RouteStep step = this; step != null; step = step.parent) {
            step.consume(now, amount);
        }
    }
}
