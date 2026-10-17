package gregtech.common.pipelike.cable.net;

public final class TickCounter {
    private long tick = Long.MIN_VALUE;
    private long value;

    public long add(long now, long amount) {
        if (tick != now) {
            tick = now;
            value = 0;
        }

        return value += amount;
    }
}
