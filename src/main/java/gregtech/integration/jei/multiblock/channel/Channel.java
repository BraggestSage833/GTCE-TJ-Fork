package gregtech.integration.jei.multiblock.channel;

import gregtech.api.util.ItemStackKey;
import net.minecraft.item.ItemStack;

import java.util.*;


public final class Channel {
    private static final Map<String, Channel> REGISTRY = new LinkedHashMap<>();
    private static final Map<ItemStackKey, Map<String, Integer>> ITEM_TO_CHANNELS = new HashMap<>();


    public static final Channel VOLTAGE = createDriver("voltage");


    public static final Channel COIL = create("coil");
    // TODO: io hatches and busses & energy


    private final String id;
    private final boolean driver;
    private final Map<ItemStackKey, Integer> indicators = new HashMap<>();

    private Channel(String id, boolean driver) {
        this.id = id;
        this.driver = driver;
    }

    public static Channel create(String id) {
        return REGISTRY.computeIfAbsent(id, key -> new Channel(key, false));
    }

    public static Channel createDriver(String id) {
        return REGISTRY.computeIfAbsent(id, key -> new Channel(key, true));
    }

    public static Channel get(String id) {
        return REGISTRY.get(id);
    }

    public static Collection<Channel> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public String getID() {
        return this.id;
    }

    public boolean isDriver() {
        return this.driver;
    }

    public void registerIndicator(ItemStack stack, int value) {
        ItemStackKey key = new ItemStackKey(stack);
        indicators.put(key, value);
        ITEM_TO_CHANNELS.computeIfAbsent(key, k -> new HashMap<>()).put(id, value);
    }

    public int getIndicatorMaxValue() {
        return indicators.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public static Collection<Map.Entry<String, Integer>> getChannelsForItem(ItemStack stack) {
        Map<String, Integer> map = ITEM_TO_CHANNELS.get(new ItemStackKey(stack));
        return map == null ? Collections.emptyList() : map.entrySet();
    }
}
