package gregtech.api.items.toolitem;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.tool.IWireCutterItem;
import gregtech.api.items.metaitem.stats.IItemCapabilityProvider;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class WireCutterItemStat implements IItemCapabilityProvider {

    @Override
    public ICapabilityProvider createProvider(ItemStack itemStack) {
        return new CapabilityProvider(itemStack);
    }

    private static class CapabilityProvider extends AbstractToolItemCapabilityProvider<IWireCutterItem> implements IWireCutterItem {

        public CapabilityProvider(ItemStack itemStack) {
            super(itemStack);
        }

        @Override
        protected Capability<IWireCutterItem> getCapability() {
            return GregtechCapabilities.CAPABILITY_WIRE_CUTTER;
        }
    }
}
