package gregtech.common.pipelike.item.tile;

import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.common.pipelike.item.ItemPipeProperties;
import gregtech.common.pipelike.item.ItemPipeType;
import gregtech.common.pipelike.item.net.ItemPipeNet;
import gregtech.common.pipelike.item.net.WorldItemPipeNet;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;


public class ItemPipeItemHandler implements IItemHandler {
    private final IPipeTile<ItemPipeType, ItemPipeProperties> pipeTile;

    @Nullable
    private final EnumFacing side;
    private WeakReference<ItemPipeNet> currentPipeNet = new WeakReference<>(null);

    public ItemPipeItemHandler(IPipeTile<ItemPipeType, ItemPipeProperties> pipeTile, @Nullable EnumFacing side) {
        this.pipeTile = pipeTile;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (side != null && pipeTile.isSideBlocked(side)) {
            return stack;
        }

        ItemPipeNet net = getItemPipeNet();
        if (net == null) {
            return stack;
        }
        else {
            return net.insert(pipeTile.getPipePos(), side, stack, simulate);
        }
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    public ItemPipeNet getItemPipeNet() {
        ItemPipeNet cached = this.currentPipeNet.get();
        if (cached != null && cached.isValid() && cached.containsNode(pipeTile.getPipePos())) {
            return cached;
        }

        WorldItemPipeNet net = (WorldItemPipeNet) pipeTile.getPipeBlock().getWorldPipeNet(pipeTile.getPipeWorld());
        ItemPipeNet found = net.getNetFromPos(pipeTile.getPipePos());

        if (found != null) {
            this.currentPipeNet = new WeakReference<>(found);
        }

        return found;
    }
}
