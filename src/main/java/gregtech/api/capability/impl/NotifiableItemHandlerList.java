package gregtech.api.capability.impl;

import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

public class NotifiableItemHandlerList extends ItemHandlerList implements IInventoryNotifiable {

    private final List<? extends IItemHandler> itemHandlers;

    public NotifiableItemHandlerList(MetaTileEntity metaTileEntity, List<? extends IItemHandler> itemHandlerList) {
        super(itemHandlerList);
        this.itemHandlers = itemHandlerList;
        this.setMetaTileEntity(metaTileEntity);
    }

    @Override
    public void setMetaTileEntity(MetaTileEntity metaTileEntity) {
        for (IItemHandler itemHandler : this.itemHandlers) {
            if (itemHandler instanceof IInventoryNotifiable)
                ((IInventoryNotifiable) itemHandler).setMetaTileEntity(metaTileEntity);
        }
    }
}
