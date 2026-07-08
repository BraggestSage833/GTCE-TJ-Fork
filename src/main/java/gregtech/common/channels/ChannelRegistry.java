package gregtech.common.channels;


import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.integration.jei.multiblock.channel.StructureChannels;

public final class ChannelRegistry {
    public static void init() {
        int counter = 1;
        for (BlockWireCoil.CoilType type : BlockWireCoil.CoilType.values()) {
            StructureChannels.COIL.registerIndicator(MetaBlocks.WIRE_COIL.getItemVariant(type), counter++);
        }
    }



}
