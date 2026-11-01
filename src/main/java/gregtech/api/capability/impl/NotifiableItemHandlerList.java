package gregtech.api.capability.impl;

import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

/**
 * Meant to hold multiple {@link NotifiableItemStackHandler} or any other item handler that implements {@link IInventoryNotifiable}
 */
public class NotifiableItemHandlerList extends ItemHandlerList implements IInventoryNotifiable {

    private final List<? extends IItemHandler> itemHandlers;

    public NotifiableItemHandlerList(MetaTileEntity metaTileEntity, List<? extends IItemHandler> itemHandlerList) {
        super(itemHandlerList);
        this.itemHandlers = itemHandlerList;
        this.addMetaTileEntityToNotifiable(metaTileEntity);
    }

    @Override
    public void addMetaTileEntityToNotifiable(MetaTileEntity metaTileEntity) {
        for (IItemHandler itemHandler : this.itemHandlers) {
            if (itemHandler instanceof IInventoryNotifiable)
                ((IInventoryNotifiable) itemHandler).addMetaTileEntityToNotifiable(metaTileEntity);
        }
    }

    @Override
    public void removeMetaTileEntityFromNotifiable(MetaTileEntity metaTileEntity) {
        for (IItemHandler itemHandler : this.itemHandlers) {
            if (itemHandler instanceof IInventoryNotifiable)
                ((IInventoryNotifiable) itemHandler).removeMetaTileEntityFromNotifiable(metaTileEntity);
        }
    }
}
