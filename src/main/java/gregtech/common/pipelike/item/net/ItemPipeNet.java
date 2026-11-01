package gregtech.common.pipelike.item.net;

import gregtech.api.pipenet.*;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.common.pipelike.item.ItemPipeProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.*;

public class ItemPipeNet extends RoutedPipeNet<ItemPipeProperties> {

    public ItemPipeNet(WorldPipeNet<ItemPipeProperties, ItemPipeNet> world) {
        super(world);
    }

    @Override
    protected int getCost(Node<ItemPipeProperties> node) {
        return Math.max(1, node.data.getPriority());
    }

    @Override
    protected int getLimit(Node<ItemPipeProperties> node) {
        return node.data.getItemsPerSecond();
    }


    public ItemStack insert(BlockPos sourcePipePos, @Nullable EnumFacing sourceSide, ItemStack stack, boolean simulate) {
        Node<ItemPipeProperties> sourceNode = getNodeAt(sourcePipePos);
        if (stack.isEmpty() || sourceNode == null) {
            return stack;
        }

        World world = worldData.getWorld();
        long now = world.getTotalWorldTime();
        Bucket sourceBucket = getPipeBucket(sourcePipePos);
        int sourceRate = getLimit(sourceNode);
        BlockPos sourceInvPos = sourceSide == null ? null : sourcePipePos.offset(sourceSide);

        Set<BlockPos> offered = simulate ? new HashSet<>() : null;
        List<Runnable> undo = new ArrayList<>();

        ItemStack remaining = stack.copy();
        for (RouteStep route : getRoutes(sourcePipePos)) {
            if (remaining.isEmpty() || sourceBucket.available(now, sourceRate) <= 0) {
                break;
            }

            int toSend = Math.min(route.remainingCapacity(now), remaining.getCount());
            if (toSend <= 0) {
                continue;
            }

            ItemStack attempt = ItemHandlerHelper.copyStackWithSize(remaining, toSend);
            ItemStack leftover = insertAtDestination(world, route.pos, sourceInvPos, attempt, simulate, offered);

            int inserted = toSend - leftover.getCount();
            if (inserted <= 0) {
                continue;
            }

            remaining.shrink(inserted);
            route.commit(now, inserted);
            if (simulate) {
                undo.add(() -> route.commit(now, -inserted));
            }
        }

        undo.forEach(Runnable::run);

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    private ItemStack insertAtDestination(World world, BlockPos pipePos, @Nullable BlockPos sourceInvPos, ItemStack stack, boolean simulate, @Nullable Set<BlockPos> offered) {
        if (!world.isBlockLoaded(pipePos)) {
            return stack;
        }

        TileEntity pipeTile = world.getTileEntity(pipePos);
        if (!(pipeTile instanceof IPipeTile<?, ?> pipe)) {
            return stack;
        }

        ItemStack remaining = stack;
        for (EnumFacing side : EnumFacing.VALUES) {
            if (remaining.isEmpty()) {
                break;
            }

            if (!pipe.isConnected(side)) {
                continue;
            }

            BlockPos inventoryPos = pipePos.offset(side);
            if (inventoryPos.equals(sourceInvPos) || !world.isBlockLoaded(inventoryPos)) {
                continue;
            }

            if (offered != null && !offered.add(inventoryPos)) {
                continue;
            }

            TileEntity inventory = world.getTileEntity(inventoryPos);
            if (inventory == null || inventory instanceof IPipeTile) {
                continue;
            }

            IItemHandler handler = inventory.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            if (handler != null) {
                remaining = ItemHandlerHelper.insertItemStacked(handler, remaining, simulate);
            }
        }

        return remaining;
    }


    @Override
    protected void writeNodeData(ItemPipeProperties nodeData, NBTTagCompound tagCompound) {
        tagCompound.setInteger("priority", nodeData.getPriority());
        tagCompound.setFloat("rate", nodeData.getTransferRate());
    }

    @Override
    protected ItemPipeProperties readNodeData(NBTTagCompound tagCompound) {
        int priority = tagCompound.getInteger("priority");
        float rate = tagCompound.getFloat("rate");
        return new ItemPipeProperties(priority, rate);
    }

}
