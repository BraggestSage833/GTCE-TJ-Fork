package gregtech.common.channels;


import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.integration.jei.multiblock.channel.StructureChannels;

public final class ChannelRegistry {
    public static void init() {
        StructureChannels.register();

        int counter = 1;
        for (BlockWireCoil.CoilType type : BlockWireCoil.CoilType.values()) {
            StructureChannels.COIL.registerIndicator(MetaBlocks.WIRE_COIL.getItemVariant(type), counter++);
        }

        counter = 1;

        for (BlockMetalCasing.MetalCasingType type : BlockMetalCasing.MetalCasingType.values()) {
            StructureChannels.CASING.registerIndicator(MetaBlocks.METAL_CASING.getItemVariant(type), counter++);
        }

        counter = 1;



        for (BlockMetalCasing.MetalCasingType type : BlockMetalCasing.MetalCasingType.values()) {
            StructureChannels.CASING.registerIndicator(MetaBlocks.METAL_CASING.getItemVariant(type), counter++);
        }

        //StructureChannels.VOLTAGE


        // TODO: Keksi tää jotenki järkevästi
        // 4) Height / parallel / voltage tierit (jos haluat)
        //StructureChannels.HEIGHT.registerIndicator(ModItems.HEIGHT_UPGRADE_3, 3);
       // StructureChannels.HEIGHT.registerIndicator(ModItems.HEIGHT_UPGRADE_4, 4);
    }
}
