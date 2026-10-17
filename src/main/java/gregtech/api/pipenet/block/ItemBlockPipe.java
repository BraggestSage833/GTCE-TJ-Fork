package gregtech.api.pipenet.block;

import gregtech.api.pipenet.tile.AttachmentType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemBlockPipe<PipeType extends Enum<PipeType> & IPipeType<NodeDataType>, NodeDataType> extends ItemBlock {

    protected final BlockPipe<PipeType, NodeDataType, ?> blockPipe;

    public ItemBlockPipe(BlockPipe<PipeType, NodeDataType, ?> block) {
        super(block);
        this.blockPipe = block;
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }


    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                float hitX, float hitY, float hitZ, IBlockState newState) {
        boolean placed = super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
        if (placed && !world.isRemote) {
            IPipeTile<PipeType, NodeDataType> placedPipe = blockPipe.getPipeTileEntity(world, pos);
            if (placedPipe != null) {
                EnumFacing towardClicked = side.getOpposite();
                if (player.isSneaking() && blockPipe.canAutoConnect(placedPipe, towardClicked)) {
                    placedPipe.setConnection(towardClicked, true, false);
                }

                if (placedPipe instanceof TileEntityPipeBase<?,?> base) {
                    base.markConnectionInitialised();
                }
            }
        }
        return placed;
    }
}
