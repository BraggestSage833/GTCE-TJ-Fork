package gregtech.common.pipelike.item;

import java.util.Objects;

public class ItemPipeProperties {
    public float transferRate;
    public int priority;

    public static final int STACK_SIZE = 64;

    public ItemPipeProperties(int priority, float transferRate) {
        this.priority = priority;
        this.transferRate = transferRate;
    }


    public ItemPipeProperties() {
        this(1, 0.25f);
    }


    public int getPriority() {
        return priority;
    }


    public ItemPipeProperties setPriority(int priority) {
        this.priority = priority;
        return this;
    }


    public float getTransferRate() {
        return transferRate;
    }


    public ItemPipeProperties setTransferRate(float transferRate) {
        this.transferRate = transferRate;
        return this;
    }

    public int getItemsPerSecond() {
        return Math.max(0, Math.round(transferRate));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemPipeProperties that = (ItemPipeProperties) o;
        return priority == that.priority && Float.compare(that.transferRate, transferRate) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, transferRate);
    }

    @Override
    public String toString() {
        return "ItemPipeProperties{" +
                "priority=" + priority +
                ", transferRate=" + transferRate +
                '}';
    }
}
