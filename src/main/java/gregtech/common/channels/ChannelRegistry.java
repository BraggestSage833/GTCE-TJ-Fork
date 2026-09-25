package gregtech.common.channels;


import gregtech.api.util.BlockInfo;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.channel.Channel;
import gregtech.integration.jei.multiblock.channel.PlaceholderType;
import net.minecraft.block.Block;

public final class ChannelRegistry {
    public static final PlaceholderType COIL = PlaceholderType.create("coil");
    public static final PlaceholderType INPUT_HATCH = PlaceholderType.create("input hatch");
    public static final PlaceholderType OUTPUT_HATCH = PlaceholderType.create("output hatch");
    public static final PlaceholderType INPUT_BUS = PlaceholderType.create("input bus");
    public static final PlaceholderType OUTPUT_BUS = PlaceholderType.create("output bus");
    public static final PlaceholderType ENERGY_INPUT_HATCH  = PlaceholderType.create("energy input hatch");
    public static final PlaceholderType ENERGY_OUTPUT_HATCH = PlaceholderType.create("energy output hatch");


    public static void init() {
        int counter = 1;
        for (BlockWireCoil.CoilType type : BlockWireCoil.CoilType.values()) {
            Channel.COIL.registerIndicator(MetaBlocks.WIRE_COIL.getItemVariant(type),counter++);
        }
        COIL.registerResolver(context -> new BlockInfo(MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.values()[
                PlaceholderType.clampIndex(context.getTier(Channel.COIL),1,BlockWireCoil.CoilType.values().length-2)]))
        );

        INPUT_HATCH.registerResolver(context -> PlaceholderType.mteHolder(
                MetaTileEntities.FLUID_IMPORT_HATCH[PlaceholderType.mteVoltageClamp(context.getTier(Channel.VOLTAGE))], context.facing)
        );

        OUTPUT_HATCH.registerResolver(context -> PlaceholderType.mteHolder(
                MetaTileEntities.FLUID_EXPORT_HATCH[PlaceholderType.mteVoltageClamp(context.getTier(Channel.VOLTAGE))], context.facing)
        );

        INPUT_BUS.registerResolver(context -> PlaceholderType.mteHolder(
                MetaTileEntities.ITEM_IMPORT_BUS[PlaceholderType.mteVoltageClamp(context.getTier(Channel.VOLTAGE))], context.facing)
        );

        OUTPUT_BUS.registerResolver(context -> PlaceholderType.mteHolder(
                MetaTileEntities.ITEM_EXPORT_BUS[PlaceholderType.mteVoltageClamp(context.getTier(Channel.VOLTAGE))], context.facing)
        );

        ENERGY_INPUT_HATCH.registerResolver(context -> PlaceholderType.mteHolder(
                MetaTileEntities.ENERGY_INPUT_HATCH[PlaceholderType.mteVoltageClamp(context.getTier(Channel.VOLTAGE))], context.facing)
        );

        ENERGY_OUTPUT_HATCH.registerResolver(context -> PlaceholderType.mteHolder(
                MetaTileEntities.ENERGY_OUTPUT_HATCH[PlaceholderType.mteVoltageClamp(context.getTier(Channel.VOLTAGE))], context.facing)
        );

    }



}
