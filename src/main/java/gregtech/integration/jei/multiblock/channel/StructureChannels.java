package gregtech.integration.jei.multiblock.channel;

import gregtech.api.GTValues;
import net.minecraft.item.ItemStack;

public enum StructureChannels {

    COIL("coil", "Coil Tier"),
    CASING("casing", "Casing Tier"),
    HEIGHT("height", "Structure Height"),
    PARALLEL("parallel", "Parallel Tier"),
    VOLTAGE("voltage", "Voltage Tier");

    private final String channel;
    private final String description;

    StructureChannels(String channel, String description) {
        this.channel = channel;
        this.description = description;
    }

    public String get() {
        return channel;
    }

    public String getDescription() {
        return description;
    }


    public void registerIndicator(ItemStack stack, int channelValue) {
        ChannelDescription.registerChannelItem(channel, channelValue, stack);
    }

    public static void register() {
        for (StructureChannels c : values()) {
            ChannelDescription.registerChannelDescription(
                    c.channel,
                    GTValues.MODID,
                    "channels." + c.channel
            );
        }
    }
}
