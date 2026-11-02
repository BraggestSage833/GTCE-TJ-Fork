package gregtech.common.pipelike.item.tile;

import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;

import gregtech.common.pipelike.item.ItemPipeProperties;
import gregtech.common.pipelike.item.ItemPipeType;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;


public class TileEntityItemPipe extends TileEntityMaterialPipeBase<ItemPipeType, ItemPipeProperties> {

    private final ItemPipeItemHandler[] sideHandlers = new ItemPipeItemHandler[7];


    @Override
    public Class<ItemPipeType> getPipeTypeClass() {
        return ItemPipeType.class;
    }

    @Override
    public boolean supportsTicking() {
        return false;
    }

    protected ItemPipeItemHandler getItemHandler(@Nullable EnumFacing side) {
        int index = side == null ? 6 : side.getIndex();
        if (sideHandlers[index] == null) {
            sideHandlers[index] = new ItemPipeItemHandler(this, side);
        }

        return sideHandlers[index];
    }

    @Nullable
    @Override
    public <T> T getCapabilityInternal(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(getItemHandler(facing));
        }

        return super.getCapabilityInternal(capability, facing);
    }
}
