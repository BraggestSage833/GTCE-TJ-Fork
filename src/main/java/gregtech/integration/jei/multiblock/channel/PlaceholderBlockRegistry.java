package gregtech.integration.jei.multiblock.channel;

import gregtech.api.GTValues;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.util.BlockInfo;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderBlockRegistry {
    private static final Map<PlaceholderType, PlaceholderBlockFactory> factories = new HashMap<>();


    public static BlockInfo MTEHolderBuilder(MetaTileEntity mte, EnumFacing facing) {
        MetaTileEntityHolder holder = new MetaTileEntityHolder();
        holder.setMetaTileEntity(mte);
        holder.getMetaTileEntity().setFrontFacing(facing);
        return new BlockInfo(MetaBlocks.MACHINE.getDefaultState(), holder, null);
    }

    public static int IOMTEClamper(int voltageTier) {
        return Math.min(voltageTier, GTValues.V.length-1);
    }



    public static void init() {
        PlaceholderBlockRegistry.register(PlaceholderType.COIL, (context) -> new BlockInfo(MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.values()[context.coilTier])));
        PlaceholderBlockRegistry.register(PlaceholderType.INPUT_HATCH, (context) -> MTEHolderBuilder(MetaTileEntities.FLUID_IMPORT_HATCH[IOMTEClamper(context.voltageTier)], context.facing));
        PlaceholderBlockRegistry.register(PlaceholderType.OUTPUT_HATCH, (context) -> MTEHolderBuilder(MetaTileEntities.FLUID_EXPORT_HATCH[IOMTEClamper(context.voltageTier)], context.facing));
        PlaceholderBlockRegistry.register(PlaceholderType.INPUT_BUS, (context) -> MTEHolderBuilder(MetaTileEntities.ITEM_IMPORT_BUS[IOMTEClamper(context.voltageTier)], context.facing));
        PlaceholderBlockRegistry.register(PlaceholderType.OUTPUT_BUS, (context) -> MTEHolderBuilder(MetaTileEntities.ITEM_EXPORT_BUS[IOMTEClamper(context.voltageTier)], context.facing));
        PlaceholderBlockRegistry.register(PlaceholderType.ENERGY_INPUT_HATCH, (context) -> MTEHolderBuilder(MetaTileEntities.ENERGY_INPUT_HATCH[context.voltageTier], context.facing));
        PlaceholderBlockRegistry.register(PlaceholderType.ENERGY_OUTPUT_HATCH, (context) -> MTEHolderBuilder(MetaTileEntities.ENERGY_OUTPUT_HATCH[context.voltageTier], context.facing));
    }


    public static void register(PlaceholderType type, PlaceholderBlockFactory factory) {
        factories.put(type, factory);
    }

    public static BlockInfo resolve(PlaceholderType type, PlaceholderContext context) {
        PlaceholderBlockFactory factory = factories.get(type);
        if (factory == null) return null;
        return factory.create(context);
    }

    @FunctionalInterface
    public interface PlaceholderBlockFactory {
        BlockInfo create(PlaceholderContext context);
    }


    public static class PlaceholderContext {
        public final int voltageTier;
        public final int coilTier;
        public final EnumFacing facing;
        public final BlockPos pos;

        public PlaceholderContext(int voltageTier, int coilTier, EnumFacing facing, BlockPos pos) {
            this.voltageTier = voltageTier;
            this.coilTier = coilTier;
            this.facing = facing;
            this.pos = pos;
        }
    }

}

