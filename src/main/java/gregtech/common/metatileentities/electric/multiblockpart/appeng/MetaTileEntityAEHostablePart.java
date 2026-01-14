package gregtech.common.metatileentities.electric.multiblockpart.appeng;

import appeng.api.AEApi;
import appeng.api.networking.GridFlags;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.BaseActionSource;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import gregtech.api.capability.IControllable;
import gregtech.api.items.toolitem.ToolMetaItem;
import gregtech.common.ConfigHolder;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityMultiblockPart;
import gregtech.common.tools.ToolWireCutter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import static gregtech.api.capability.GregtechDataCodes.*;

public abstract class MetaTileEntityAEHostablePart<T extends IAEStack<T>> extends MetaTileEntityMultiblockPart implements IControllable {

    private final Class<? extends IStorageChannel<T>> storageChannel;
    private AENetworkProxy aeProxy;
    private int meUpdateTick;
    protected boolean isOnline;
    private boolean allowExtraConnections;
    protected boolean meStatusChanged = false;
    protected boolean workingEnabled = true;

    public MetaTileEntityAEHostablePart(ResourceLocation metaTileEntityId, int tier, Class<? extends IStorageChannel<T>> storageChannel) {
        super(metaTileEntityId, tier);
        this.meUpdateTick = 0;
        this.storageChannel = storageChannel;
        this.allowExtraConnections = false;
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote) {
            this.meUpdateTick++;
        }
    }

    /**
     * ME hatch will try to put its buffer back to me system when removal.
     * So there is no need to drop them.
     */
    public void clearMachineInventory(@NotNull List<@NotNull ItemStack> itemBuffer) {}

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        if (this.aeProxy != null) {
            buf.writeBoolean(true);
            NBTTagCompound proxy = new NBTTagCompound();
            this.aeProxy.writeToNBT(proxy);
            buf.writeCompoundTag(proxy);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeInt(this.meUpdateTick);
        buf.writeBoolean(this.isOnline);
        buf.writeBoolean(this.allowExtraConnections);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        if (buf.readBoolean()) {
            NBTTagCompound nbtTagCompound;
            try {
                nbtTagCompound = buf.readCompoundTag();
            } catch (IOException ignored) {
                nbtTagCompound = null;
            }

            if (this.aeProxy != null && nbtTagCompound != null) {
                this.aeProxy.readFromNBT(nbtTagCompound);
            }
        }
        this.meUpdateTick = buf.readInt();
        this.isOnline = buf.readBoolean();
        this.allowExtraConnections = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == UPDATE_ONLINE_STATUS) {
            boolean isOnline = buf.readBoolean();
            if (this.isOnline != isOnline) {
                this.isOnline = isOnline;
                scheduleRenderUpdate();
            }
        } else if (dataId == WORKING_ENABLED) {
            this.workingEnabled = buf.readBoolean();
            this.scheduleRenderUpdate();
        } else if (dataId == ALLOW_EXTRA_CONNECTIONS) {
            this.allowExtraConnections = buf.readBoolean();
            this.scheduleRenderUpdate();
        }
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        this.workingEnabled = workingEnabled;
        World world = this.getWorld();
        if (world != null && !world.isRemote) {
            writeCustomData(WORKING_ENABLED, buf -> buf.writeBoolean(workingEnabled));
            this.markDirty();
        }
    }

    @NotNull
    @Override
    public AECableType getCableConnectionType(@NotNull AEPartLocation part) {
        if (part.getFacing() != this.frontFacing && !this.allowExtraConnections) {
            return AECableType.NONE;
        }
        return AECableType.SMART;
    }

    @Nullable
    @Override
    public AENetworkProxy getProxy() {
        if (this.aeProxy == null) {
            return this.aeProxy = this.createProxy();
        }
        if (!this.aeProxy.isReady() && this.getWorld() != null) {
            this.aeProxy.onReady();
        }
        return this.aeProxy;
    }

    @Override
    public void setFrontFacing(EnumFacing frontFacing) {
        super.setFrontFacing(frontFacing);
        updateConnectableSides();
    }

    @Override
    public void gridChanged() {}

    /**
     * Get the me network connection status, updating it if on serverside.
     *
     * @return the updated status.
     */
    public boolean updateMEStatus() {
        if (!getWorld().isRemote) {
            boolean isOnline = this.aeProxy != null && this.aeProxy.isActive() && this.aeProxy.isPowered();
            if (this.isOnline != isOnline) {
                writeCustomData(UPDATE_ONLINE_STATUS, buf -> buf.writeBoolean(isOnline));
                this.isOnline = isOnline;
                this.meStatusChanged = true;
            } else {
                this.meStatusChanged = false;
            }
        }
        return this.isOnline;
    }

    protected boolean shouldSyncME() {
        return this.meUpdateTick % ConfigHolder.updateIntervals == 0;
    }

    protected IActionSource getActionSource() {
        if (this.getHolder() instanceof IActionHost) {
            return new MachineSource(this.getHolder());
        }
        return new BaseActionSource();
    }

    @Nullable
    private AENetworkProxy createProxy() {
        if (this.getHolder() instanceof IGridProxyable) {
            AENetworkProxy proxy = new AENetworkProxy(this.getHolder(), "mte_proxy", this.getStackForm(), true);
            proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            proxy.setIdlePowerUsage(ConfigHolder.meHatchEnergyUsage);
            proxy.setValidSides(getConnectableSides());
            return proxy;
        }
        return null;
    }

    @NotNull
    protected IStorageChannel<T> getStorageChannel() {
        return AEApi.instance().storage().getStorageChannel(storageChannel);
    }

    @Nullable
    protected IMEMonitor<T> getMonitor() {
        AENetworkProxy proxy = getProxy();
        if (proxy == null) return null;

        IStorageChannel<T> channel = getStorageChannel();

        try {
            return proxy.getStorage().getInventory(channel);
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    public EnumSet<EnumFacing> getConnectableSides() {
        return this.allowExtraConnections ? EnumSet.allOf(EnumFacing.class) : EnumSet.of(getFrontFacing());
    }

    public void updateConnectableSides() {
        if (this.aeProxy != null) {
            this.aeProxy.setValidSides(getConnectableSides());
        }
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        ItemStack stack = playerIn.getHeldItem(hand);
        Item item = stack.getItem();
        if (item instanceof ToolMetaItem<?>) {
            ToolMetaItem<?>.MetaToolValueItem toolValueItem = ((ToolMetaItem<?>)item).getItem(stack);
            if (toolValueItem.getToolStats() instanceof ToolWireCutter) {
                if (!playerIn.isCreative())
                    ((ToolMetaItem<?>) item).damageItem(stack, 1, false);
                this.allowExtraConnections = !this.allowExtraConnections;
                this.updateConnectableSides();
                if (!getWorld().isRemote) {
                    playerIn.sendStatusMessage(new TextComponentTranslation(this.allowExtraConnections ? "gregtech.machine.me.extra_connections.enabled" : "gregtech.machine.me.extra_connections.disabled"), true);
                    this.writeCustomData(ALLOW_EXTRA_CONNECTIONS, buf -> buf.writeBoolean(this.allowExtraConnections));
                    this.markDirty();
                }
                return true;
            }
        }
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        this.updateConnectableSides();
        this.markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("AllowExtraConnections", this.allowExtraConnections);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.allowExtraConnections = data.getBoolean("AllowExtraConnections");
    }
}
