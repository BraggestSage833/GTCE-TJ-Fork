package gregtech.api.capability.impl;

import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class NotifiableItemStackHandler extends ItemStackHandler implements IInventoryNotifiable {

    protected final List<MetaTileEntity> metaTileEntitiesToNotify = new ObjectArrayList<>();

    public NotifiableItemStackHandler(MetaTileEntity metaTileEntity, int slots) {
        super(slots);
        if (metaTileEntity != null)
            this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    @Override
    protected void onContentsChanged(int slot) {
        for (MetaTileEntity metaTileEntity : this.metaTileEntitiesToNotify)
            metaTileEntity.setInventoryNotified(true);
    }

    @Override
    public void addMetaTileEntityToNotifiable(MetaTileEntity metaTileEntity) {
        if (metaTileEntity != null)
            this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    @Override
    public void removeMetaTileEntityFromNotifiable(MetaTileEntity metaTileEntity) {
        this.metaTileEntitiesToNotify.remove(metaTileEntity);
    }
}
