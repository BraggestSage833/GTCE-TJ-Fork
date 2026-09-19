package gregtech.integration.jei.multiblock.channel;

import net.minecraft.item.ItemStack;

public enum StructureChannels {

    COIL("coil"),
    CASING("casing"),
    EXTENT("extent"),
    VOLTAGE("voltage"),
    CELL("cell"),

    MOTOR("motor"),
    CONVEYOR("conveyor"),
    EMITTER("emitter"),
    FIELD_GEN("field Generator"),
    PISTON("piston"),
    PUMP("pump"),
    ROBOT_ARM("robot Arm"),
    SENSOR("sensor");

    private final String channel;

    StructureChannels(String channel) {
        this.channel = channel;
    }

    public String get() {
        return channel;
    }

    public void registerIndicator(ItemStack stack, int channelValue) {
        ChannelDescription.registerChannelItem(channel, channelValue, stack);
    }
}
