package gregtech.common.gui.widget.appeng.slot;

import appeng.api.storage.data.IAEStack;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.TextFieldWidget;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import gregtech.common.gui.widget.appeng.AEConfigWidget;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.slot.IConfigurableSlot;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.regex.Pattern;

/**
 * @Author GlodBlock
 * @Description The amount set widget for config slot
 * @Date 2023/4/21-21:20
 */
public class AmountSetSlot<T extends IAEStack<T>> extends Widget {

    private int index = -1;
    private final TextFieldWidget amountText;
    private final AEConfigWidget<T> parentWidget;

    public AmountSetSlot(int x, int y, AEConfigWidget<T> widget) {
        super(new Position(x, y), new Size(80, 30));
        this.parentWidget = widget;
        this.amountText = new TextFieldWidget(x + 3, y + 14, 60, 15, false, this::getAmountStr, this::setNewAmount)
                .setValidator(str -> Pattern.compile("-*?[0-9_]*\\*?").matcher(str).matches());
        if (isClientSide())
            this.amountText.setEnableTextBox(false);
    }

    public void setSlotIndex(int slotIndex) {
        this.index = slotIndex;
        this.amountText.setEnableTextBox(slotIndex >= 0);
        writeClientAction(0, buf -> buf.writeVarInt(this.index));
    }

    public TextFieldWidget getText() {
        return this.amountText;
    }

    public String getAmountStr() {
        if (this.index < 0) {
            return "0";
        }
        IConfigurableSlot<T> slot = this.parentWidget.getConfig(this.index);
        if (slot.getConfig() != null) {
            return String.valueOf(slot.getConfig().getStackSize());
        }
        return "0";
    }

    public void setNewAmount(String amount) {
        try {
            long newAmount = Long.parseLong(amount);
            writeUpdateInfo(1, buf -> buf.writeVarLong(newAmount));
        } catch (NumberFormatException ignored) {}
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == 1) {
            writeClientAction(1, buf -> buf.writeVarLong(buffer.readVarLong()));
        }
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        super.handleClientAction(id, buffer);
        if (id == 0) {
            this.index = buffer.readVarInt();;
        } else if (id == 1) {
            if (this.index < 0) {
                return;
            }
            IConfigurableSlot<T> slot = this.parentWidget.getConfig(this.index);
            long newAmt = buffer.readVarLong();
            if (newAmt > 0 && slot.getConfig() != null) {
                slot.getConfig().setStackSize(newAmt);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        super.drawInBackground(mouseX, mouseY, context);
        if (this.index >= 0) {
            Position position = getPosition();
            GuiTextures.BACKGROUND.draw(position.x, position.y, 80, 30);
            drawStringSized("Amount", position.x + 3, position.y + 3, 0x404040, false, 1f, false);
            GuiTextures.DISPLAY.draw(position.x + 3, position.y + 11, 65, 14);
        }
    }
}
