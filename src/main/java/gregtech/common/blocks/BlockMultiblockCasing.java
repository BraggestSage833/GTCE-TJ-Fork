package gregtech.common.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockMultiblockCasing extends VariantBlock<BlockMultiblockCasing.MultiblockCasingType> {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockMultiblockCasing() {
        super(Material.IRON);
        setTranslationKey("multiblock_casing");
        setHardness(5.0f);
        setResistance(10.0f);
        setSoundType(SoundType.METAL);
        setHarvestLevel("wrench", 2);
        setDefaultState(getState(MultiblockCasingType.ENGINE_INTAKE_CASING));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        super.createBlockState();
        return new BlockStateContainer(this, VARIANT, ACTIVE);
    }

    @Override
    public IBlockState getState(MultiblockCasingType variant) {
        return super.getState(variant).withProperty(ACTIVE, false);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta % 9).withProperty(ACTIVE, meta / VALUES.length >= 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return Math.min(15, super.getMetaFromState(state) + (state.getValue(ACTIVE) ? VALUES.length : 0));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return super.damageDropped(state) - (state.getValue(ACTIVE) ? VALUES.length : 0);
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, SpawnPlacementType type) {
        return false;
    }

    public enum MultiblockCasingType implements IStringSerializable {

        ENGINE_INTAKE_CASING("engine_intake"),
        GRATE_CASING("grate"),
        ASSEMBLER_CASING("assembler"),
        FUSION_CASING("fusion"),
        FUSION_CASING_MK2("fusion_mk2"),
        EXTREME_ENGINE_INTAKE_CASING("extreme_engine_intake"),;

        private final String name;

        MultiblockCasingType(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

    }

}
