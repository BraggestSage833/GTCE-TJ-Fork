package gregtech.integration.jei.multiblock.infos;

import com.google.common.collect.Lists;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockMachineCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import gregtech.integration.jei.multiblock.channel.PlaceholderType;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class PyrolyzeOvenInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return MetaTileEntities.PYROLYSE_OVEN;
    }

    @Override
    public MultiblockShapeInfo getMatchingShapes() {
        return MultiblockShapeInfo.builder()
                .aisle("XXX", "ISF", "XXX")
                .aisle("CCC", "C#C", "CCC")
                .aisle("CCC", "C#C", "CCC")
                .aisle("XXX", "BEH", "XXX")
                .where('S', MetaTileEntities.PYROLYSE_OVEN, EnumFacing.NORTH)
                .where('X', MetaBlocks.MACHINE_CASING.getState(BlockMachineCasing.MachineCasingType.ULV))
                .where('C', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                .where('#', Blocks.AIR.getDefaultState())
                .where('I', PlaceholderType.INPUT_BUS, MetaTileEntities.ITEM_IMPORT_BUS[GTValues.HV], EnumFacing.NORTH)
                .where('F', PlaceholderType.INPUT_HATCH, MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.HV], EnumFacing.NORTH)
                .where('E', PlaceholderType.ENERGY_INPUT_HATCH, MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.HV], EnumFacing.SOUTH)
                .where('B', PlaceholderType.OUTPUT_BUS, MetaTileEntities.ITEM_EXPORT_BUS[GTValues.HV], EnumFacing.SOUTH)
                .where('H', PlaceholderType.OUTPUT_HATCH, MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.HV], EnumFacing.SOUTH)
                .build();
    }

    @Override
    public String[] getDescription() {
        return new String[]{I18n.format("gregtech.multiblock.pyrolyze_oven.description")};
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        ItemStack coils = MetaBlocks.WIRE_COIL.getItemVariant(BlockWireCoil.CoilType.CUPRONICKEL);
        ITextComponent tooltip = new TextComponentTranslation("gregtech.multiblock.preview.only", coils.getDisplayName()).setStyle(new Style().setColor(TextFormatting.RED));
        addBlockTooltip(coils, tooltip);
    }

}
