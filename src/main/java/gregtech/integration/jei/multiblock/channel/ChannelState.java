package gregtech.integration.jei.multiblock.channel;
import java.util.EnumMap;
import java.util.Map;

/**
 * Holds the current selected value for each channel.
 * Used by JEI buttons, Freedom Wrench and controller.
 */
public class ChannelState {

    private final Map<StructureChannels, Integer> values = new EnumMap<>(StructureChannels.class);

    /** Returns the selected index for a channel (default 0). */
    public int get(StructureChannels id) {
        return values.getOrDefault(id, 0);
    }

    /** Sets the selected index for a channel. */
    public void set(StructureChannels id, int index) {
        values.put(id, index);
    }

    /** Returns all channel values (used for NBT saving). */
    public Map<StructureChannels, Integer> getAll() {
        return values;
    }

    /** Creates a deep copy (useful for JEI). */
    public ChannelState copy() {
        ChannelState copy = new ChannelState();
        copy.values.putAll(this.values);
        return copy;
    }
}
