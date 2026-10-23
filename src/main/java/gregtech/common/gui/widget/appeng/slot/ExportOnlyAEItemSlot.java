package gregtech.common.gui.widget.appeng.slot;

import appeng.api.storage.data.IAEItemStack;
import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.slot.ExportOnlyAESlot;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.stack.WrappedItemStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ExportOnlyAEItemSlot extends ExportOnlyAESlot<IAEItemStack> implements IItemHandlerModifiable, IInventoryNotifiable {

    protected final List<MetaTileEntity> metaTileEntitiesToNotify = new ObjectArrayList<>();

    public ExportOnlyAEItemSlot(IAEItemStack config, IAEItemStack stock) {
        super(config, stock);
    }

    public ExportOnlyAEItemSlot() {
        super();
    }

    public void setTrigger(Consumer<Integer> trigger) {
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey(CONFIG_TAG)) {
            this.config = WrappedItemStack.fromNBT(nbt.getCompoundTag(CONFIG_TAG));
        }
        if (nbt.hasKey(STOCK_TAG)) {
            this.stock = WrappedItemStack.fromNBT(nbt.getCompoundTag(STOCK_TAG));
        }
    }

    @Override
    public ExportOnlyAEItemSlot copy() {
        return new ExportOnlyAEItemSlot(
                this.config == null ? null : this.config.copy(),
                this.stock == null ? null : this.stock.copy());
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {}

    @Override
    public int getSlots() {
        return 1;
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == 0 && this.stock != null) {
            return this.stock.getDefinition();
        }
        return ItemStack.EMPTY;
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return stack;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot == 0 && this.stock != null) {
            int extracted = (int) Math.min(this.stock.getStackSize(), amount);
            ItemStack result = this.stock.createItemStack();
            result.setCount(extracted);
            if (!simulate) {
                this.stock.decStackSize(extracted);
                if (this.stock.getStackSize() == 0) {
                    this.stock = null;
                }
            }
            this.notifyMetaTileEntities();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public IAEItemStack requestStack() {
        IAEItemStack result = super.requestStack();
        if (result instanceof WrappedItemStack) {
            return ((WrappedItemStack) result).getAEStack();
        } else {
            return result;
        }
    }

    @Override
    public IAEItemStack exceedStack() {
        IAEItemStack result = super.exceedStack();
        if (result instanceof WrappedItemStack) {
            return ((WrappedItemStack) result).getAEStack();
        } else {
            return result;
        }
    }

    @Override
    public void addStack(IAEItemStack stack) {
        if (this.stock == null) {
            this.stock = WrappedItemStack.fromItemStack(stack.createItemStack());
        } else {
            this.stock.add(stack);
        }
        this.notifyMetaTileEntities();
    }

    @Override
    public void setStack(IAEItemStack stack) {
        ItemStack stockItem, itemToStock = null;
        if (this.stock == null && stack == null) {
            return;
        } else if (stack == null) {
            this.stock = null;
        } else if (this.stock == null || (stockItem = this.stock.createItemStack()).getItem() != (itemToStock = stack.createItemStack()).getItem() ||
                stockItem.getMetadata() != itemToStock.getMetadata() || stockItem.getTagCompound() != itemToStock.getTagCompound()) {
            // TODO NBT comparison not yet supported. GT machines don't handle NBT very well anyways.
            this.stock = WrappedItemStack.fromItemStack(itemToStock != null ? itemToStock : stack.createItemStack());
        } else if (stockItem.getCount() != itemToStock.getCount()) {
            this.stock.setStackSize(stack.getStackSize());
        } else return;
        this.notifyMetaTileEntities();
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    protected void notifyMetaTileEntities() {
        for (MetaTileEntity metaTileEntity : this.metaTileEntitiesToNotify)
            metaTileEntity.setInventoryNotified(true);
    }

    @Override
    public void addMetaTileEntityToNotifiable(MetaTileEntity metaTileEntity) {
        this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    @Override
    public void removeMetaTileEntityFromNotifiable(MetaTileEntity metaTileEntity) {
        this.metaTileEntitiesToNotify.remove(metaTileEntity);
    }
}
