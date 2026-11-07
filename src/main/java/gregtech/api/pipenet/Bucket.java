package gregtech.api.pipenet;

public class Bucket {
    private final int windowTicks;

    private long window = Long.MIN_VALUE;
    private int used;

    public Bucket(int windowTicks) {
        this.windowTicks = Math.max(1, windowTicks);
    }

    public int available(long now, int limit) {
        rollWindow(now);
        return Math.max(0, limit - used);
    }

    public void consume(long now, int amount) {
        rollWindow(now);
        used += amount;
    }

    private void rollWindow(long now) {
        long current = now / windowTicks;
        if (current == window) {
            return;
        }

        window = current;
        used = 0;
    }
}
