package gregtech.common.gui.widget.appeng;

import appeng.api.storage.data.IAEItemStack;
import gregtech.common.gui.widget.SlotScrollableWidgetGroup;
import gregtech.common.gui.widget.appeng.slot.AEItemConfigSlot;
import gregtech.common.gui.widget.appeng.slot.ExportOnlyAEItemList;
import gregtech.common.gui.widget.appeng.slot.ExportOnlyAEItemSlot;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.slot.IConfigurableSlot;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.stack.WrappedItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

public class AEItemConfigWidget extends AEConfigWidget<IAEItemStack> {

    final ExportOnlyAEItemList itemList;

    public AEItemConfigWidget(int x, int y, ExportOnlyAEItemList itemList) {
        super(x, y, itemList.getInventory(), itemList.isStocking());
        this.itemList = itemList;
    }

    @Override
    @SuppressWarnings("unchecked")
    void init() {
        this.displayList = new IConfigurableSlot[this.config.length];
        this.cached = new IConfigurableSlot[this.config.length];
        final SlotScrollableWidgetGroup scrollableWidgetGroup = new SlotScrollableWidgetGroup(0, 0, 166, 72, 4)
                .setScrollWidth(4);
        for (int i = 0; i < this.config.length; i++) {
            this.displayList[i] = new ExportOnlyAEItemSlot();
            this.cached[i] = new ExportOnlyAEItemSlot();
            scrollableWidgetGroup.addWidget(new AEItemConfigSlot(18 * (i % 4), 18 * (i / 4), this, i));
        }
        this.addWidget(scrollableWidgetGroup);
    }

    public boolean hasStackInConfig(ItemStack stack) {
        return itemList.hasStackInConfig(stack, true);
    }

    public boolean isAutoPull() {
        return itemList.isAutoPull();
    }

    @Override
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == UPDATE_ID) {
            int size = buffer.readVarInt();
            for (int i = 0; i < size; i++) {
                int index = buffer.readVarInt();
                IConfigurableSlot<IAEItemStack> slot = this.displayList[index];
                if (buffer.readBoolean()) {
                    slot.setConfig(WrappedItemStack.fromPacket(buffer));
                } else {
                    slot.setConfig(null);
                }
                if (buffer.readBoolean()) {
                    slot.setStock(WrappedItemStack.fromPacket(buffer));
                } else {
                    slot.setStock(null);
                }
            }
        }
    }
}
