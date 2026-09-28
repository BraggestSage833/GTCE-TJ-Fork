package gregtech.common.items.behaviors;

import gregtech.api.GTValues;
import gregtech.api.metatileentity.ITieredMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.common.ConfigHolder;
import gregtech.common.sound.GTSoundEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import appeng.api.util.AEColor;
import appeng.tile.networking.TileCableBus;
import net.minecraftforge.fml.common.Loader;

import java.util.*;

public class ColorSprayBehaviour extends AbstractUsableBehaviour {

    private final ItemStack empty;
    protected EnumDyeColor color;


    public ColorSprayBehaviour(ItemStack empty, int totalUses, int color) {
        super(totalUses);
        this.empty = empty;
        this.color = EnumDyeColor.values()[color];
    }


    protected EnumDyeColor getColor() {
        return color;
    }

    protected void setColor(EnumDyeColor color) {
        this.color = color;
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.canPlayerEdit(pos, side, stack)) {
            return EnumActionResult.FAIL;
        }
        if (!tryPaintBlock(world, pos, side, player)) {
            return EnumActionResult.PASS;
        }


        world.playSound(player, pos, GTSoundEvents.SPRAY_CAN, SoundCategory.PLAYERS,1F,1F);
        useItemDurability(player, hand, stack, empty.copy());
        return EnumActionResult.SUCCESS;
    }

    public boolean tryPaintBlock(World world, BlockPos pos, EnumFacing side, EntityPlayer player) {
        IBlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        return block.recolorBlock(world, pos, side, this.color) || tryPaintSpecialBlock(world, pos, block, player);
    }

    private boolean tryPaintSpecialBlock(World world, BlockPos pos, Block block, EntityPlayer player) {
        if (block == Blocks.GLASS) {
            IBlockState newBlockState = Blocks.STAINED_GLASS.getDefaultState()
                .withProperty(BlockStainedGlass.COLOR, this.color);
            world.setBlockState(pos, newBlockState);
            return true;
        }
        if (block == Blocks.GLASS_PANE) {
            IBlockState newBlockState = Blocks.STAINED_GLASS_PANE.getDefaultState()
                .withProperty(BlockStainedGlassPane.COLOR, this.color);
            world.setBlockState(pos, newBlockState);
            return true;
        }
        if (Loader.isModLoaded(GTValues.MODID_AE2)) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileCableBus) {
                TileCableBus cable = (TileCableBus) te;

                if (cable.getColor().ordinal() != color.ordinal()) {
                    cable.recolourBlock(null, AEColor.values()[color.ordinal()], player);
                    return true;
                }
            }
        }
        return false;
    }



    protected void paintConnectedBlocks(World world, BlockPos origin, EnumFacing side, int range, EntityPlayer player) {
        Block originBlock = normalizeBlock(world.getBlockState(origin).getBlock());
        TileEntity originTE = world.getTileEntity(origin);

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);


        int maxNodes = getMaxFloodFillNodes();

        while (!queue.isEmpty() && visited.size() <= maxNodes) {
            BlockPos current = queue.poll();

            tryPaintBlock(world, current, side, player);

            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(facing);
                if (visited.contains(neighbor) || !withinRange(origin, neighbor, range)) {
                    continue;
                }
                if (isSameBlockFamily(world, neighbor, originBlock, originTE)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

    }

    protected int getMaxFloodFillNodes() {
        return ConfigHolder.SprayCanOptions.floodFillRange;
    }

    private static boolean withinRange(BlockPos origin, BlockPos pos, int range) {
        return Math.abs(pos.getX() - origin.getX()) <= range
                && Math.abs(pos.getY() - origin.getY()) <= range
                && Math.abs(pos.getZ() - origin.getZ()) <= range;
    }

   
    protected boolean isSameBlockFamily(World world, BlockPos pos, Block originBlock, TileEntity originTE) {
        Block block = normalizeBlock(world.getBlockState(pos).getBlock());
        if (block != originBlock) {
            return false;
        }

        TileEntity te = world.getTileEntity(pos);
        if ((te == null) != (originTE == null)) {
            return false;
        }
        if (te == null) {
            return true; 
        }
        if (!te.getClass().isInstance(originTE)) {
            return false;
        }

      
        if (te instanceof MetaTileEntityHolder && originTE instanceof MetaTileEntityHolder) {
            MetaTileEntity teMTE = ((MetaTileEntityHolder) te).getMetaTileEntity();
            MetaTileEntity originMTE = ((MetaTileEntityHolder) originTE).getMetaTileEntity();

            if ((teMTE == null) != (originMTE == null)) {
                return false;
            }
            if (teMTE != null && originMTE != null) {
                if (!teMTE.metaTileEntityId.equals(originMTE.metaTileEntityId)) {
                    return false;
                }
                if (teMTE instanceof ITieredMetaTileEntity && originMTE instanceof ITieredMetaTileEntity) {
                    if (((ITieredMetaTileEntity) teMTE).getTier() != ((ITieredMetaTileEntity) originMTE).getTier()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static Block normalizeBlock(Block block) {
        if (block == Blocks.GLASS || block == Blocks.STAINED_GLASS) return Blocks.GLASS;
        if (block == Blocks.GLASS_PANE || block == Blocks.STAINED_GLASS_PANE) return Blocks.GLASS_PANE;
        return block;
    }


    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        int remainingUses = getUsesLeft(itemStack);
        lines.add(I18n.format("behaviour.paintspray." + this.color.getTranslationKey() + ".tooltip"));
        lines.add(I18n.format("behaviour.paintspray.uses", remainingUses));
    }
}
