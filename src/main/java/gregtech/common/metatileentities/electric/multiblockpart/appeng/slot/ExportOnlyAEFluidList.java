package gregtech.common.metatileentities.electric.multiblockpart.appeng.slot;

import appeng.api.storage.data.IAEFluidStack;
import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraftforge.fluids.FluidStack;

public class ExportOnlyAEFluidList implements IInventoryNotifiable {

    protected final int size;
    protected ExportOnlyAEFluidSlot[] inventory;

    public ExportOnlyAEFluidList(MetaTileEntity holder, int slots) {
        this.size = slots;
        createInventory(holder);
    }

    protected void createInventory(MetaTileEntity holder) {
        this.inventory = new ExportOnlyAEFluidSlot[size];
        for (int i = 0; i < size; i++) {
            this.inventory[i] = new ExportOnlyAEFluidSlot(holder);
        }
    }

    public ExportOnlyAEFluidSlot[] getInventory() {
        return inventory;
    }

    public void clearConfig() {
        for (var slot : inventory) {
            slot.setConfig(null);
            slot.setStock(null);
        }
    }

    public boolean hasStackInConfig(FluidStack stack, boolean checkExternal) {
        if (stack == null) return false;
        for (ExportOnlyAEFluidSlot slot : inventory) {
            IAEFluidStack config = slot.getConfig();
            if (config != null && config.getFluid().equals(stack.getFluid())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAutoPull() {
        return false;
    }

    public boolean isStocking() {
        return false;
    }

    public boolean ownsSlot(ExportOnlyAEFluidSlot testSlot) {
        for (var slot : inventory) {
            if (slot == testSlot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setMetaTileEntity(MetaTileEntity metaTileEntity) {
        for (ExportOnlyAEFluidSlot slot : this.inventory) {
            if (slot instanceof IInventoryNotifiable)
                ((IInventoryNotifiable) slot).setMetaTileEntity(metaTileEntity);
        }
    }
}
