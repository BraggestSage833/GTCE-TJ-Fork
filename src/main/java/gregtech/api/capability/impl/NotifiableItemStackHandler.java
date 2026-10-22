package gregtech.api.capability.impl;

import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class NotifiableItemStackHandler extends ItemStackHandler implements IInventoryNotifiable {

    protected MetaTileEntity metaTileEntity;

    public NotifiableItemStackHandler(MetaTileEntity metaTileEntity, int slots) {
        super(slots);
        this.metaTileEntity = metaTileEntity;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (!stack.isEmpty())
            this.metaTileEntity.setInventoryNotified(true);
        super.setStackInSlot(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        this.metaTileEntity.setInventoryNotified(true);
    }

    @Override
    public void setMetaTileEntity(MetaTileEntity metaTileEntity) {
        this.metaTileEntity = metaTileEntity;
    }
}
