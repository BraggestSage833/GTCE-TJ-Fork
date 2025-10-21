package gregtech.common.metatileentities.electric.multiblockpart.appeng;

import appeng.api.config.Actionable;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.render.Textures;
import gregtech.common.gui.widget.appeng.AEFluidConfigWidget;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.slot.ExportOnlyAEFluidList;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.slot.ExportOnlyAEFluidSlot;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.stack.WrappedFluidStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.IFluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static gregtech.common.items.MetaItems.TOOL_DATA_STICK;

public class MetaTileEntityMEInputHatch extends MetaTileEntityAEHostablePart<IAEFluidStack> implements IMultiblockAbilityPart<IFluidTank> {

    public final static String FLUID_BUFFER_TAG = "FluidTanks";
    public final static String WORKING_TAG = "WorkingEnabled";
    private final static int CONFIG_SIZE = 16;
    protected ExportOnlyAEFluidList aeFluidHandler;

    public MetaTileEntityMEInputHatch(ResourceLocation metaTileEntityId) {
        this(metaTileEntityId, GTValues.EV);
    }

    protected MetaTileEntityMEInputHatch(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier, IFluidStorageChannel.class);
    }

    protected ExportOnlyAEFluidList getAEFluidHandler() {
        if (aeFluidHandler == null) {
            aeFluidHandler = new ExportOnlyAEFluidList(this, CONFIG_SIZE, this.getController());
        }
        return aeFluidHandler;
    }

    @Override
    protected void initializeInventory() {
        getAEFluidHandler(); // initialize it
        super.initializeInventory();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMEInputHatch(metaTileEntityId);
    }

    @Override
    protected FluidTankList createImportFluidHandler() {
        return new FluidTankList(false, getAEFluidHandler().getInventory());
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote && this.workingEnabled && this.shouldSyncME() && updateMEStatus()) {
            syncME();
        }
    }

    protected void syncME() {
        IMEMonitor<IAEFluidStack> monitor = getMonitor();
        if (monitor == null) return;

        for (ExportOnlyAEFluidSlot aeTank : this.getAEFluidHandler().getInventory()) {
            // Try to clear the wrong fluid
            IAEFluidStack exceedFluid = aeTank.exceedStack();
            if (exceedFluid != null) {
                long total = exceedFluid.getStackSize();
                IAEFluidStack notInserted = monitor.injectItems(exceedFluid, Actionable.MODULATE,
                        this.getActionSource());
                if (notInserted != null && notInserted.getStackSize() > 0) {
                    aeTank.drain((int) (total - notInserted.getStackSize()), true);
                    continue;
                } else {
                    aeTank.drain((int) total, true);
                }
            }
            // Fill it
            IAEFluidStack reqFluid = aeTank.requestStack();
            if (reqFluid != null) {
                IAEFluidStack extracted = monitor.extractItems(reqFluid, Actionable.MODULATE, this.getActionSource());
                if (extracted != null) {
                    aeTank.addStack(extracted);
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
        IMEMonitor<IAEFluidStack> monitor = getMonitor();
        if (monitor == null) return;

        for (ExportOnlyAEFluidSlot aeTank : this.getAEFluidHandler().getInventory()) {
            IAEFluidStack stock = aeTank.getStock();
            if (stock instanceof WrappedFluidStack) {
                stock = ((WrappedFluidStack) stock).getAEStack();
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

        builder.widget(new ToggleButtonWidget(151, 5, 17, 17, GuiTextures.BUTTON_GT_LOGO, this::isWorkingEnabled, this::setWorkingEnabled));

        // Config slots
        builder.widget(new AEFluidConfigWidget(7, 25, this.getAEFluidHandler()));

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
        NBTTagList tanks = new NBTTagList();
        for (int i = 0; i < CONFIG_SIZE; i++) {
            ExportOnlyAEFluidSlot tank = this.getAEFluidHandler().getInventory()[i];
            NBTTagCompound tankTag = new NBTTagCompound();
            tankTag.setInteger("slot", i);
            tankTag.setTag("tank", tank.serializeNBT());
            tanks.appendTag(tankTag);
        }
        data.setTag(FLUID_BUFFER_TAG, tanks);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey(WORKING_TAG)) {
            this.workingEnabled = data.getBoolean(WORKING_TAG);
        }
        if (data.hasKey(FLUID_BUFFER_TAG, 9)) {
            NBTTagList tanks = (NBTTagList) data.getTag(FLUID_BUFFER_TAG);
            for (NBTBase nbtBase : tanks) {
                NBTTagCompound tankTag = (NBTTagCompound) nbtBase;
                ExportOnlyAEFluidSlot tank = this.getAEFluidHandler().getInventory()[tankTag.getInteger("slot")];
                tank.deserializeNBT(tankTag.getCompoundTag("tank"));
            }
        }
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (this.shouldRenderOverlay()) {
            for (EnumFacing facing : this.getConnectableSides())
                Textures.ME_INPUT_HATCH_OVERLAY.render(renderState, translation, pipeline, facing, this.isOnline);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gregtech.machine.fluid_hatch.import.tooltip"));
        tooltip.add(I18n.format("gregtech.machine.me.fluid_import.tooltip"));
        tooltip.add(I18n.format("gregtech.machine.me_import_fluid_hatch.configs.tooltip"));
        tooltip.add(I18n.format("gregtech.machine.me.copy_paste.tooltip"));
        tooltip.add(I18n.format("gregtech.machine.me.extra_connections.tooltip"));
        tooltip.add(I18n.format("gregtech.universal.enabled"));
    }

    @Override
    public MultiblockAbility<IFluidTank> getAbility() {
        return MultiblockAbility.IMPORT_FLUIDS;
    }

    @Override
    public void registerAbilities(List<IFluidTank> abilityList) {
        abilityList.addAll(Arrays.asList(this.getAEFluidHandler().getInventory()));
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
        tag.setTag("MEInputHatch", writeConfigToTag());
        dataStick.setTagCompound(tag);
        dataStick.setTranslatableName("gregtech.machine.me.fluid_import.data_stick.name");
        player.sendStatusMessage(new TextComponentTranslation("gregtech.machine.me.import_copy_settings"), true);
    }

    protected NBTTagCompound writeConfigToTag() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound configStacks = new NBTTagCompound();
        tag.setTag("ConfigStacks", configStacks);
        for (int i = 0; i < CONFIG_SIZE; i++) {
            var slot = this.aeFluidHandler.getInventory()[i];
            IAEFluidStack config = slot.getConfig();
            if (config == null) {
                continue;
            }
            NBTTagCompound stackNbt = new NBTTagCompound();
            config.writeToNBT(stackNbt);
            configStacks.setTag(Integer.toString(i), stackNbt);
        }
        return tag;
    }

    public final void onDataStickPaste(EntityPlayer player, ItemStack dataStick) {
        NBTTagCompound tag = dataStick.getTagCompound();
        if (tag == null || !tag.hasKey("MEInputHatch")) {
            return;
        }
        readConfigFromTag(tag.getCompoundTag("MEInputHatch"));
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
                    this.aeFluidHandler.getInventory()[i].setConfig(WrappedFluidStack.fromNBT(configTag));
                } else {
                    this.aeFluidHandler.getInventory()[i].setConfig(null);
                }
            }
        }
    }
}
