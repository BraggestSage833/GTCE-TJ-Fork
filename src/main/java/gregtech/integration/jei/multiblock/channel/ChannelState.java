package gregtech.integration.jei.multiblock.channel;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;


public class ChannelState {

    private final Map<Channel, Integer> values = new IdentityHashMap<>();


    public int get(Channel channel) {
        return values.getOrDefault(channel, 0);
    }

    public void set(Channel channel, int index) {
        values.put(channel, index);
    }


    public Map<Channel, Integer> getAll() {
        return values;
    }

    public ChannelState copy() {
        ChannelState copy = new ChannelState();
        copy.values.putAll(this.values);
        return copy;
    }
}
