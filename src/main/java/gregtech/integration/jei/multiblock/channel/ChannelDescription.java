package gregtech.integration.jei.multiblock.channel;

import gregtech.api.util.ItemStackKey;
import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChannelDescription {

    private static final Map<String, ChannelDescription> REGISTRY = new HashMap<>();
    private static final Map<ItemStackKey, Map<String, Integer>> ITEM_TO_CHANNELS = new HashMap<>();

    private final String channelName;
    private final Map<ItemStackKey, Integer> items = new HashMap<>();

    public static ChannelDescription get(String name) {
        return REGISTRY.computeIfAbsent(name, ChannelDescription::new);
    }

    public static boolean has(String name) {
        return REGISTRY.containsKey(name);
    }

    public static Collection<Map.Entry<String, Integer>> getChannelsForItem(ItemStack stack) {
        ItemStackKey key = new ItemStackKey(stack);
        Map<String, Integer> map = ITEM_TO_CHANNELS.get(key);
        return map == null ? Collections.emptyList() : map.entrySet();
    }

    public static void registerChannelItem(String channel, int value, ItemStack stack) {
        ChannelDescription desc = get(channel);
        ItemStackKey key = new ItemStackKey(stack);
        desc.items.put(key, value);
        ITEM_TO_CHANNELS.computeIfAbsent(key, k -> new HashMap<>()).put(channel, value);
    }

    private ChannelDescription(String name) {
        this.channelName = name;
    }

    public String getName() {
        return channelName;
    }

    public Map<ItemStackKey, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }
}
