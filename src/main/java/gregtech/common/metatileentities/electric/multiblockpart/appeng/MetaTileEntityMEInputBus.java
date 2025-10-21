package gregtech.common.metatileentities.electric.multiblockpart.appeng;

import appeng.api.config.Actionable;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.widget.GhostCircuitWidget;
import gregtech.common.gui.widget.appeng.AEItemConfigWidget;
import gregtech.common.gui.widget.appeng.slot.ExportOnlyAEItemList;
import gregtech.common.gui.widget.appeng.slot.ExportOnlyAEItemSlot;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.stack.WrappedItemStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static gregtech.common.items.MetaItems.TOOL_DATA_STICK;

public class MetaTileEntityMEInputBus extends MetaTileEntityAEHostablePart<IAEItemStack> implements IMultiblockAbilityPart<IItemHandlerModifiable> {

    public final static String ITEM_BUFFER_TAG = "ItemSlots";
    public final static String WORKING_TAG = "WorkingEnabled";
    private final static int CONFIG_SIZE = 16;
    protected ExportOnlyAEItemList aeItemHandler;
    protected ItemStackHandler circuitInventory = new ItemStackHandler(1);
    protected ItemStackHandler extraSlotInventory;
    private ItemHandlerList actualImportItems;

    public MetaTileEntityMEInputBus(ResourceLocation metaTileEntityId) {
        this(metaTileEntityId, GTValues.EV);
    }

    protected MetaTileEntityMEInputBus(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier, IItemStorageChannel.class);
    }

    protected ExportOnlyAEItemList getAEItemHandler() {
        if (aeItemHandler == null) {
            aeItemHandler = new ExportOnlyAEItemList(this, CONFIG_SIZE, this.getController());
        }
        return aeItemHandler;
    }

    @Override
    protected void initializeInventory() {
        super.initializeInventory();
        this.aeItemHandler = getAEItemHandler();
        this.extraSlotInventory = new ItemStackHandler(1);
        this.actualImportItems = new ItemHandlerList(Arrays.asList(this.aeItemHandler, this.extraSlotInventory));
        this.importItems = this.actualImportItems;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMEInputBus(metaTileEntityId);
    }

    public IItemHandlerModifiable getImportItems() {
        return this.actualImportItems;
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote && this.workingEnabled && updateMEStatus() && shouldSyncME()) {
            syncME();
        }
    }

    protected void syncME() {
        IMEMonitor<IAEItemStack> monitor = getMonitor();
        if (monitor == null) return;

        for (ExportOnlyAEItemSlot aeSlot : this.getAEItemHandler().getInventory()) {
            // Try to clear the wrong item
            IAEItemStack exceedItem = aeSlot.exceedStack();
            if (exceedItem != null) {
                long total = exceedItem.getStackSize();
                IAEItemStack notInserted = monitor.injectItems(exceedItem, Actionable.MODULATE, this.getActionSource());
                if (notInserted != null && notInserted.getStackSize() > 0) {
                    aeSlot.extractItem(0, (int) (total - notInserted.getStackSize()), false);
                    continue;
                } else {
                    aeSlot.extractItem(0, (int) total, false);
                }
            }
            // Fill it
            IAEItemStack reqItem = aeSlot.requestStack();
            if (reqItem != null) {
                IAEItemStack extracted = monitor.extractItems(reqItem, Actionable.MODULATE, this.getActionSource());
                if (extracted != null) {
                    aeSlot.addStack(extracted);
                }
            }
        }
    }

    @Override
    public void onRemoval() {
        flushInventory();
        super.onRemoval();
    }

    protected void flushInventory() {
        IMEMonitor<IAEItemStack> monitor = getMonitor();
        if (monitor == null) return;

        for (ExportOnlyAEItemSlot aeSlot : this.getAEItemHandler().getInventory()) {
            IAEItemStack stock = aeSlot.getStock();
            if (stock instanceof WrappedItemStack) {
                stock = ((WrappedItemStack) stock).getAEStack();
            }
            if (stock != null) {
                monitor.injectItems(stock, Actionable.MODULATE, this.getActionSource());
            }
        }
    }

    @Override
    protected final ModularUI createUI(EntityPlayer player) {
        ModularUI.Builder builder = createUITemplate(player);
        return builder.build(this.getHolder(), player);
    }

    protected ModularUI.Builder createUITemplate(EntityPlayer player) {
        ModularUI.Builder builder = ModularUI
                .builder(GuiTextures.BACKGROUND, 176, 18 + 18 * 4 + 94)
                .label(10, 5, getMetaFullName());
        // ME Network status
        builder.dynamicLabel(10, 15, () -> this.isOnline ?
                        I18n.format("gregtech.gui.me_network.online") :
                        I18n.format("gregtech.gui.me_network.offline"),
                0x404040);

        builder.widget(new ToggleButtonWidget(151, 5, 18, 18, GuiTextures.BUTTON_GT_LOGO, this::isWorkingEnabled, this::setWorkingEnabled));
        // Config slots
        builder.widget(new AEItemConfigWidget(7, 25, this.getAEItemHandler()));

        // Ghost circuit slot
        builder.widget(new GhostCircuitWidget(this.circuitInventory, 7 + 18 * 4, 25 + 18 * 3));

        // Extra slot
        builder.widget(new SlotWidget(extraSlotInventory, 0, 7 + 18 * 4, 25 + 18 * 2)
                .setBackgroundTexture(GuiTextures.SLOT));

        // Arrow image
        builder.image(7 + 18 * 4, 25 + 18, 18, 18, GuiTextures.ARROW_DOUBLE);

        builder.bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 18 + 18 * 4 + 12);
        return builder;
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.workingEnabled;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE) {
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(workingEnabled);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.workingEnabled = buf.readBoolean();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean(WORKING_TAG, this.workingEnabled);
        NBTTagList slots = new NBTTagList();
        for (int i = 0; i < CONFIG_SIZE; i++) {
            ExportOnlyAEItemSlot slot = this.getAEItemHandler().getInventory()[i];
            NBTTagCompound slotTag = new NBTTagCompound();
            slotTag.setInteger("slot", i);
            slotTag.setTag("stack", slot.serializeNBT());
            slots.appendTag(slotTag);
        }
        data.setTag(ITEM_BUFFER_TAG, slots);
        data.setTag("GhostCircuit", this.circuitInventory.serializeNBT());
        GTUtility.writeItems(this.extraSlotInventory, "ExtraInventory", data);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey(WORKING_TAG)) {
            this.workingEnabled = data.getBoolean(WORKING_TAG);
        }
        if (data.hasKey(ITEM_BUFFER_TAG, 9)) {
            NBTTagList slots = (NBTTagList) data.getTag(ITEM_BUFFER_TAG);
            for (NBTBase nbtBase : slots) {
                NBTTagCompound slotTag = (NBTTagCompound) nbtBase;
                ExportOnlyAEItemSlot slot = this.getAEItemHandler().getInventory()[slotTag.getInteger("slot")];
                slot.deserializeNBT(slotTag.getCompoundTag("stack"));
            }
        }
        GTUtility.readItems(this.extraSlotInventory, "ExtraInventory", data);
        this.importItems = createImportItemHandler();
        if (data.hasKey("GhostCircuit"))
            this.circuitInventory.deserializeNBT(data.getCompoundTag("GhostCircuit"));
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (this.shouldRenderOverlay()) {
            for (EnumFacing facing : this.getConnectableSides())
                Textures.ME_INPUT_BUS_OVERLAY.render(renderState, translation, pipeline, facing, this.isOnline);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gregtech.machine.item_bus.import.tooltip"));
        tooltip.add(I18n.format("gregtech.machine.me.item_import.tooltip"));
        tooltip.add(I18n.format("gregtech.machine.me_import_item_hatch.configs.tooltip"));
        tooltip.add(I18n.format("gregtech.machine.me.copy_paste.tooltip"));
        tooltip.add(I18n.format("gregtech.machine.me.extra_connections.tooltip"));
        tooltip.add(I18n.format("gregtech.universal.enabled"));
    }

    @Override
    public MultiblockAbility<IItemHandlerModifiable> getAbility() {
        return MultiblockAbility.IMPORT_ITEMS;
    }

    @Override
    public void registerAbilities(List<IItemHandlerModifiable> abilityList) {
        abilityList.add(this.circuitInventory);
        abilityList.add(this.actualImportItems);
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        ItemStack stack = playerIn.getHeldItem(hand);
        if (stack.isItemEqual(TOOL_DATA_STICK.getStackForm())) {
            if (!this.getWorld().isRemote)
                if (playerIn.isSneaking())
                    this.onDataStickCopy(playerIn, stack);
                else this.onDataStickPaste(playerIn, stack);
            return true;
        }
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    public final void onDataStickCopy(EntityPlayer player, ItemStack dataStick) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("MEInputBus", writeConfigToTag());
        dataStick.setTagCompound(tag);
        dataStick.setTranslatableName("gregtech.machine.me.item_import.data_stick.name");
        player.sendStatusMessage(new TextComponentTranslation("gregtech.machine.me.import_copy_settings"), true);
    }

    protected NBTTagCompound writeConfigToTag() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound configStacks = new NBTTagCompound();
        tag.setTag("ConfigStacks", configStacks);
        for (int i = 0; i < CONFIG_SIZE; i++) {
            var slot = this.aeItemHandler.getInventory()[i];
            IAEItemStack config = slot.getConfig();
            if (config == null) {
                continue;
            }
            NBTTagCompound stackNbt = new NBTTagCompound();
            config.getDefinition().writeToNBT(stackNbt);
            configStacks.setTag(Integer.toString(i), stackNbt);
        }
        tag.setTag("GhostCircuit", this.circuitInventory.serializeNBT());
        return tag;
    }

    public final void onDataStickPaste(EntityPlayer player, ItemStack dataStick) {
        NBTTagCompound tag = dataStick.getTagCompound();
        if (tag == null || !tag.hasKey("MEInputBus")) {
            return;
        }
        readConfigFromTag(tag.getCompoundTag("MEInputBus"));
        syncME();
        player.sendStatusMessage(new TextComponentTranslation("gregtech.machine.me.import_paste_settings"), true);
    }

    protected void readConfigFromTag(NBTTagCompound tag) {
        if (tag.hasKey("ConfigStacks")) {
            NBTTagCompound configStacks = tag.getCompoundTag("ConfigStacks");
            for (int i = 0; i < CONFIG_SIZE; i++) {
                String key = Integer.toString(i);
                if (configStacks.hasKey(key)) {
                    NBTTagCompound configTag = configStacks.getCompoundTag(key);
                    this.aeItemHandler.getInventory()[i].setConfig(WrappedItemStack.fromNBT(configTag));
                } else {
                    this.aeItemHandler.getInventory()[i].setConfig(null);
                }
            }
        }
       if (tag.hasKey("GhostCircuit"))
           this.circuitInventory.deserializeNBT(tag.getCompoundTag("GhostCircuit"));
    }
}
