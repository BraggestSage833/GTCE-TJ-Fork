package gregtech.api.gui.widgets;

import com.mojang.realmsclient.gui.ChatFormatting;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.igredient.IIngredientSlot;
import gregtech.api.gui.resources.RenderUtil;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TankWidget extends Widget implements IIngredientSlot {

    public final IFluidTank fluidTank;

    public int fluidRenderOffset = 1;
    private boolean hideTooltip;
    private boolean alwaysShowFull;

    private boolean allowClickFilling;
    private boolean allowClickEmptying;

    private TextureArea[] backgroundTexture;
    private TextureArea overlayTexture;

    private FluidStack lastFluidInTank;
    private int lastTankCapacity;

    public TankWidget(IFluidTank fluidTank, int x, int y, int width, int height) {
        super(new Position(x, y), new Size(width, height));
        this.fluidTank = fluidTank;
    }

    public TankWidget setHideTooltip(boolean hideTooltip) {
        this.hideTooltip = hideTooltip;
        return this;
    }

    public TankWidget setAlwaysShowFull(boolean alwaysShowFull) {
        this.alwaysShowFull = alwaysShowFull;
        return this;
    }

    public TankWidget setBackgroundTexture(TextureArea... backgroundTexture) {
        this.backgroundTexture = backgroundTexture;
        return this;
    }

    public TankWidget setOverlayTexture(TextureArea overlayTexture) {
        this.overlayTexture = overlayTexture;
        return this;
    }

    public TankWidget setFluidRenderOffset(int fluidRenderOffset) {
        this.fluidRenderOffset = fluidRenderOffset;
        return this;
    }

    public TankWidget setContainerClicking(boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
        if (!(fluidTank instanceof IFluidHandler))
            throw new IllegalStateException("Container IO is only supported for fluid tanks that implement IFluidHandler");
        this.allowClickFilling = allowClickContainerFilling;
        this.allowClickEmptying = allowClickContainerEmptying;
        return this;
    }

    @Override
    public Object getIngredientOverMouse(int mouseX, int mouseY) {
        if (isMouseOverElement(mouseX, mouseY)) {
            return lastFluidInTank;
        }
        return null;
    }

    public String getFormattedFluidAmount() {
        return String.format("%,d", lastFluidInTank == null ? 0 : lastFluidInTank.amount);
    }

    public String getFluidLocalizedName() {
        return lastFluidInTank == null ? "" : lastFluidInTank.getLocalizedName();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        Position pos = getPosition();
        Size size = getSize();
        if (backgroundTexture != null) {
            for (TextureArea textureArea : backgroundTexture) {
                textureArea.draw(pos.x, pos.y, size.width, size.height);
            }
        }
        //do not draw fluids if they are handled by JEI - it draws them itself
        if (lastFluidInTank != null && lastFluidInTank.amount > 0 && !gui.isJEIHandled) {
            GlStateManager.disableBlend();
            RenderUtil.drawFluidForGui(lastFluidInTank, alwaysShowFull ? lastFluidInTank.amount : lastTankCapacity,
                    pos.x + fluidRenderOffset, pos.y + fluidRenderOffset,
                    size.width - fluidRenderOffset, size.height - fluidRenderOffset);

            if (alwaysShowFull && !hideTooltip) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(0.5, 0.5, 1);

                String s = TextFormattingUtil.formatLongToCompactString(lastFluidInTank.amount, 4) + "L";

                FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
                fontRenderer.drawStringWithShadow(s, (pos.x + (size.width / 3)) * 2 - fontRenderer.getStringWidth(s) + 21, (pos.y + (size.height / 3) + 6) * 2, 0xFFFFFF);
                GlStateManager.popMatrix();
            }
            GlStateManager.enableBlend();
            GlStateManager.color(1.0f, 1.0f, 1.0f);
        }
        if (overlayTexture != null) {
            overlayTexture.draw(pos.x, pos.y, size.width, size.height);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInForeground(int mouseX, int mouseY) {
        if (!hideTooltip && !gui.isJEIHandled && isMouseOverElement(mouseX, mouseY)) {
            List<String> tooltips = new ArrayList<>();
            if (lastFluidInTank != null) {
                Fluid fluid = lastFluidInTank.getFluid();
                tooltips.add(fluid.getLocalizedName(lastFluidInTank));

                // Add chemical formula tooltip
                String formula = FluidTooltipUtil.getFluidTooltip(lastFluidInTank);
                if (formula != null && !formula.isEmpty())
                    tooltips.add(ChatFormatting.GRAY.toString() + formula);

                tooltips.add(I18n.format("gregtech.fluid.amount", lastFluidInTank.amount, lastTankCapacity));
                tooltips.add(I18n.format("gregtech.fluid.temperature", fluid.getTemperature(lastFluidInTank)));
                tooltips.add(I18n.format(fluid.isGaseous(lastFluidInTank) ? "gregtech.fluid.state_gas" : "gregtech.fluid.state_liquid"));
            } else {
                tooltips.add(I18n.format("gregtech.fluid.empty"));
                tooltips.add(I18n.format("gregtech.fluid.amount", 0, lastTankCapacity));
            }
            if (allowClickFilling) {
                tooltips.add(""); //add empty line to separate things
                tooltips.add(I18n.format("gregtech.fluid.click_to_fill"));
                tooltips.add(I18n.format("gregtech.fluid.click_to_fill.shift"));
            }
            if (allowClickEmptying) {
                tooltips.add(""); //add empty line to separate things
                tooltips.add(I18n.format("gregtech.fluid.click_to_empty"));
                tooltips.add(I18n.format("gregtech.fluid.click_to_empty.shift"));
            }
            drawHoveringText(ItemStack.EMPTY, tooltips, 300, mouseX, mouseY);
            GlStateManager.color(1.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public void detectAndSendChanges() {
        FluidStack fluidStack = fluidTank.getFluid();
        if (fluidTank.getCapacity() != lastTankCapacity) {
            this.lastTankCapacity = fluidTank.getCapacity();
            writeUpdateInfo(0, buffer -> buffer.writeVarInt(lastTankCapacity));
        }
        if (fluidStack == null && lastFluidInTank != null) {
            this.lastFluidInTank = null;
            writeUpdateInfo(1, buffer -> {
            });
        } else if (fluidStack != null) {
            if (!fluidStack.isFluidEqual(lastFluidInTank)) {
                this.lastFluidInTank = fluidStack.copy();
                NBTTagCompound fluidStackTag = fluidStack.writeToNBT(new NBTTagCompound());
                writeUpdateInfo(2, buffer -> buffer.writeCompoundTag(fluidStackTag));
            } else if (fluidStack.amount != lastFluidInTank.amount) {
                this.lastFluidInTank.amount = fluidStack.amount;
                writeUpdateInfo(3, buffer -> buffer.writeVarInt(lastFluidInTank.amount));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        if (id == 0) {
            this.lastTankCapacity = buffer.readVarInt();
        } else if (id == 1) {
            this.lastFluidInTank = null;
        } else if (id == 2) {
            NBTTagCompound fluidStackTag;
            try {
                fluidStackTag = buffer.readCompoundTag();
            } catch (IOException ignored) {
                return;
            }
            this.lastFluidInTank = FluidStack.loadFluidStackFromNBT(fluidStackTag);
        } else if (id == 3 && lastFluidInTank != null) {
            this.lastFluidInTank.amount = buffer.readVarInt();
        }

        if (id == 4) {
            try {
                ItemStack stack = buffer.readItemStack();
                this.gui.entityPlayer.inventory.setItemStack(stack);
            } catch (IOException e) {
                GTLog.logger.info(e.getMessage());
            }
        }
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        super.handleClientAction(id, buffer);
        if (id == 1) {
            boolean isShiftKeyDown = buffer.readBoolean();
            int button = buffer.readInt();
            ItemStack clickStack = this.tryClickContainer(isShiftKeyDown, button);
            if (!clickStack.isEmpty()) {
                this.writeUpdateInfo(4, buf -> buf.writeItemStack(clickStack));
            }
        }
    }

    private ItemStack tryClickContainer(boolean isShiftKeyDown, int button) {
        EntityPlayer player = this.gui.entityPlayer;
        ItemStack currentStack = player.inventory.getItemStack();
        if (!currentStack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null))
            return ItemStack.EMPTY;
        int maxAttempts = isShiftKeyDown ? currentStack.getCount() : 1;

        if (button == 1 && this.allowClickFilling && this.fluidTank.getFluidAmount() > 0) {
            boolean performedFill = false;
            FluidStack initialFluid = this.fluidTank.getFluid();
            for (int i = 0; i < maxAttempts; i++) {
                FluidActionResult result = FluidUtil.tryFillContainer(currentStack,
                        (IFluidHandler) this.fluidTank, Integer.MAX_VALUE, null, false);
                if (!result.isSuccess()) break;
                ItemStack remainingStack = result.getResult();
                if (!remainingStack.isEmpty() && currentStack.getCount() > 1 && !player.inventory.addItemStackToInventory(remainingStack))
                    break; //do not continue if we can't add resulting container into inventory
                FluidActionResult fillResult = FluidUtil.tryFillContainer(currentStack, (IFluidHandler) this.fluidTank, Integer.MAX_VALUE, null, true);
                ItemStack fillStack = fillResult.getResult();
                if (currentStack.getCount() > 1) {
                    currentStack.shrink(1);
                } else currentStack = fillStack;
                performedFill = true;
            }
            if (performedFill) {
                SoundEvent soundevent = initialFluid.getFluid().getFillSound(initialFluid);
                player.world.playSound(null, player.posX, player.posY + 0.5, player.posZ,
                        soundevent, SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.inventory.setItemStack(currentStack);
                return currentStack;
            }
        }

        if (button == 0 && this.allowClickEmptying) {
            boolean performedEmptying = false;
            for (int i = 0; i < maxAttempts; i++) {
                FluidActionResult result = FluidUtil.tryEmptyContainer(currentStack,
                        (IFluidHandler) this.fluidTank, Integer.MAX_VALUE, null, false);
                if (!result.isSuccess()) break;
                ItemStack remainingStack = result.getResult();
                if (!remainingStack.isEmpty() && currentStack.getCount() > 1 && !player.inventory.addItemStackToInventory(remainingStack))
                    break; //do not continue if we can't add resulting container into inventory
                FluidActionResult emptyResult = FluidUtil.tryEmptyContainer(currentStack, (IFluidHandler) this.fluidTank, Integer.MAX_VALUE, null, true);
                ItemStack emptyStack = emptyResult.getResult();
                if (currentStack.getCount() > 1)
                    currentStack.shrink(1);
                else currentStack = emptyStack;
                performedEmptying = true;
            }
            FluidStack filledFluid = this.fluidTank.getFluid();
            if (performedEmptying) {
                SoundEvent soundevent = filledFluid.getFluid().getEmptySound(filledFluid);
                player.world.playSound(null, player.posX, player.posY + 0.5, player.posZ,
                        soundevent, SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.inventory.setItemStack(currentStack);
                return currentStack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            ItemStack stack = this.gui.entityPlayer.inventory.getItemStack();
            if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                boolean isShiftKeyDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
                this.writeClientAction(1, buffer -> {
                    buffer.writeBoolean(isShiftKeyDown);
                    buffer.writeInt(button);
                });
                this.playButtonClickSound();
                return true;
            }
        }
        return false;
    }
}
