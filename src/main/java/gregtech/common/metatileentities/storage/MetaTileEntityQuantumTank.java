package gregtech.common.metatileentities.storage;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Vector3;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ThermalFluidHandlerItemStack;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.ModularUI.Builder;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.ITieredMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;

public class MetaTileEntityQuantumTank extends MetaTileEntity implements ITieredMetaTileEntity {

    private static final double[] rotations = new double[]{180.0, 0.0, -90.0, 90.0};

    private final int tier;
    private final int maxFluidCapacity;
    private FluidTank fluidTank;
    private final ItemStackHandler containerInventory;
    protected boolean voiding;

    public MetaTileEntityQuantumTank(ResourceLocation metaTileEntityId, int tier, int maxFluidCapacity) {
        super(metaTileEntityId);
        this.tier = tier;
        this.maxFluidCapacity = maxFluidCapacity;
        this.containerInventory = new ItemStackHandler(2);
        initializeInventory();
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    protected void initializeInventory() {
        super.initializeInventory();
        this.fluidTank = new QuantumFluidTank(maxFluidCapacity);
        this.fluidInventory = fluidTank;
        this.importFluids = new FluidTankList(false, fluidTank);
        this.exportFluids = new FluidTankList(false, fluidTank);
    }

    @Override
    public int getActualComparatorValue() {
        FluidTank fluidTank = this.fluidTank;
        int fluidAmount = fluidTank.getFluidAmount();
        int maxCapacity = fluidTank.getCapacity();
        float f = fluidAmount / (maxCapacity * 1.0f);
        return MathHelper.floor(f * 14.0f) + (fluidAmount > 0 ? 1 : 0);
    }

    @Override
    public void update() {
        super.update();

        if (!getWorld().isRemote) {
            fillContainerFromInternalTank(containerInventory, containerInventory, 0, 1);
            fillInternalTankFromFluidContainer(containerInventory, containerInventory, 0, 1);

        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("ContainerInventory", containerInventory.serializeNBT());
        data.setTag("FluidInventory", fluidTank.writeToNBT(new NBTTagCompound()));
        data.setBoolean("IsVoiding", voiding);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.containerInventory.deserializeNBT(data.getCompoundTag("ContainerInventory"));
        this.fluidTank.readFromNBT(data.getCompoundTag("FluidInventory"));
        this.voiding = data.getBoolean("IsVoiding");
    }

    @Override
    public ICapabilityProvider initItemStackCapabilities(ItemStack itemStack) {
        return new ThermalFluidHandlerItemStack(itemStack, maxFluidCapacity, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public void initFromItemStackData(NBTTagCompound itemStack) {
        super.initFromItemStackData(itemStack);
        if (itemStack.hasKey("Fluid", Constants.NBT.TAG_COMPOUND)) {
            this.fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(itemStack.getCompoundTag("Fluid")));
        } else if (itemStack.hasKey("FluidName", Constants.NBT.TAG_COMPOUND)) { // for people that have old super tanks filled with fluid
            this.fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(itemStack.getCompoundTag("FluidName")));
        }
        if (itemStack.getBoolean("IsVoiding")) {
            setVoiding(true);
        }
    }

    @Override
    public void writeItemStackData(NBTTagCompound itemStack) {
        super.writeItemStackData(itemStack);
        FluidStack fluidStack = fluidTank.getFluid();
        if (fluidStack != null && fluidStack.amount > 0) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            fluidStack.writeToNBT(tagCompound);
            itemStack.setTag("Fluid", tagCompound);
        }
        if (this.voiding) {
            itemStack.setBoolean("IsVoiding", true);
        }
    }

    @Override
    protected boolean shouldSerializeInventories() {
        return false;
    }

    @Override
    public void clearMachineInventory(NonNullList<ItemStack> itemBuffer) {
        super.clearMachineInventory(itemBuffer);
        clearInventory(itemBuffer, containerInventory);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityQuantumTank(metaTileEntityId, tier, maxFluidCapacity);
    }

    @Override
    protected FluidTankList createImportFluidHandler() {
        return new FluidTankList(false, fluidTank);
    }

    @Override
    protected FluidTankList createExportFluidHandler() {
        return new FluidTankList(false, fluidTank);
    }

    @Override
    public boolean hasFrontFacing() {
        return false;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        Textures.VOLTAGE_CASINGS[tier].render(renderState, translation, ArrayUtils.add(pipeline,
                new ColourMultiplier(GTUtility.convertRGBtoOpaqueRGBA_CL(getPaintingColorForRendering()))));
        translation.translate(0.5, 0.001, 0.5);
        translation.rotate(Math.toRadians(rotations[getFrontFacing().getIndex() - 2]), new Vector3(0.0, 1.0, 0.0));
        translation.translate(-0.5, 0.0, -0.5);
        Textures.SCREEN.renderSided(EnumFacing.UP, renderState, translation, pipeline);
    }

    @Override
    public Pair<TextureAtlasSprite, Integer> getParticleTexture() {
        return Pair.of(Textures.VOLTAGE_CASINGS[tier].getParticleSprite(), getPaintingColor());
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gregtech.machine.quantum_tank.capacity", maxFluidCapacity));
        NBTTagCompound compound = stack.getTagCompound();
        if (compound != null) {
            if (compound.hasKey("Fluid", Constants.NBT.TAG_COMPOUND)) {
                FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(compound.getCompoundTag("Fluid"));
                if (fluidStack != null) {
                    tooltip.add(I18n.format("gregtech.machine.quantum_tank.tooltip.name", fluidStack.getLocalizedName()));
                    tooltip.add(I18n.format("gregtech.machine.quantum_tank.tooltip.count", fluidStack.amount));
                }
            }
            if (compound.getBoolean("IsVoiding")) {
                tooltip.add(I18n.format("gregtech.machine.quantum_tank.tooltip.voiding_enabled"));
            }
        }
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        Builder builder = ModularUI.defaultBuilder();
        builder.image(7, 16, 81, 55, GuiTextures.DISPLAY);
        TankWidget tankWidget = new TankWidget(fluidTank, 69, 52, 18, 18)
                .setHideTooltip(true).setAlwaysShowFull(true);
        builder.widget(tankWidget);
        builder.label(11, 20, "gregtech.gui.fluid_amount", 0xFFFFFF);
        builder.dynamicLabel(11, 30, tankWidget::getFormattedFluidAmount, 0xFFFFFF);
        builder.dynamicLabel(11, 40, tankWidget::getFluidLocalizedName, 0xFFFFFF);
        return builder.label(6, 6, getMetaFullName())
                .widget(new FluidContainerSlotWidget(containerInventory, 0, 90, 17, false)
                        .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.IN_SLOT_OVERLAY))
                .widget(new ImageWidget(91, 36, 14, 15, GuiTextures.TANK_ICON))
                .widget(new SlotWidget(containerInventory, 1, 90, 54, true, false)
                        .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.OUT_SLOT_OVERLAY))
                .widget(new ToggleButtonWidget(151, 64, 18, 18,
                        GuiTextures.BUTTON_FLUID_VOID, this::isVoiding, this::setVoiding)
                        .setTooltipText("gregtech.gui.fluid_voiding.tooltip")
                        .shouldUseBaseBackground())
                .bindPlayerInventory(entityPlayer.inventory)
                .build(getHolder(), entityPlayer);
    }

    protected boolean isVoiding() {
        return voiding;
    }

    protected void setVoiding(boolean isPartialVoid) {
        this.voiding = isPartialVoid;
        if (!getWorld().isRemote) {
//            writeCustomData(UPDATE_IS_VOIDING, buf -> buf.writeBoolean(this.voiding));
            markDirty();
        }
    }

    private class QuantumFluidTank extends FluidTank  {
        public QuantumFluidTank(int capacity) {
            super(capacity);
        }

        @Override
        public int fillInternal(FluidStack resource, boolean doFill) {
            int accepted = super.fillInternal(resource, doFill);

            // if we couldn't accept "resource", and "resource" is not the same as the stored fluid.
            if (accepted == 0 && !resource.isFluidEqual(getFluid())) {
                return 0;
            }
            return voiding ? resource.amount : accepted;
        }
    }
}
