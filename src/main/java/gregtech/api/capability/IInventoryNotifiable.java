package gregtech.api.capability;

import gregtech.api.metatileentity.MetaTileEntity;

public interface IInventoryNotifiable {

    void addMetaTileEntityToNotifiable(MetaTileEntity metaTileEntity);

    void removeMetaTileEntityFromNotifiable(MetaTileEntity metaTileEntity);
}
