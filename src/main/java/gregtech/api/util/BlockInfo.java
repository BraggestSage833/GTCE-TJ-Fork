package gregtech.api.util;

import com.google.common.base.Preconditions;
import gregtech.integration.jei.multiblock.channel.PlaceholderType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * BlockInfo represents immutable information for block in world
 * This includes block state and tile entity, and needed for complete representation
 * of some complex blocks like machines, when rendering or manipulating them without world instance
 */
public class BlockInfo {

    public static final BlockInfo EMPTY = new BlockInfo(Blocks.AIR.getDefaultState());

    private final IBlockState blockState;
    private final TileEntity tileEntity;
    private final PlaceholderType placeholder;

    public BlockInfo(Block block) {
        this(block.getDefaultState());
    }

    public BlockInfo(IBlockState blockState) {
        this(blockState, null, null);
    }

    public BlockInfo(IBlockState blockState, PlaceholderType type) {
        this(blockState, null, type);
    }

    public BlockInfo(IBlockState blockState, TileEntity tileEntity, PlaceholderType type) {
        this.blockState = blockState;
        this.tileEntity = tileEntity;
        this.placeholder = type;
        Preconditions.checkArgument(tileEntity == null || blockState.getBlock().hasTileEntity(blockState),
            "Cannot create block info with tile entity for block not having it");
    }

    public static BlockInfo placeholder(PlaceholderType type) {
        return new BlockInfo(null, null, type);
    }

    public boolean isPlaceholder(PlaceholderType type) {
        return placeholder == type;
    }


    public IBlockState getBlockState() {
        return blockState;
    }

    public TileEntity getTileEntity() {
        return tileEntity;
    }

    public PlaceholderType getPlaceHolderType() {
        return placeholder;
    }

    public void apply(World world, BlockPos pos) {
        if (blockState == null) {
            return;
        }
        world.setBlockState(pos, blockState);
        if (tileEntity != null) {
            world.setTileEntity(pos, tileEntity);
        }
    }
}
