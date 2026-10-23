package gregtech.common.metatileentities.electric.multiblockpart.appeng.slot;

import appeng.api.storage.data.IAEFluidStack;
import gregtech.api.capability.IInventoryNotifiable;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.stack.WrappedFluidStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class ExportOnlyAEFluidSlot extends ExportOnlyAESlot<IAEFluidStack> implements IFluidTank, IFluidHandler, IInventoryNotifiable {

    protected final List<MetaTileEntity> metaTileEntitiesToNotify = new ObjectArrayList<>();
    private MetaTileEntity holder;

    public ExportOnlyAEFluidSlot(MetaTileEntity holder, IAEFluidStack config, IAEFluidStack stock) {
        super(config, stock);
        this.holder = holder;
    }

    public ExportOnlyAEFluidSlot(MetaTileEntity holder) {
        this(holder, null, null);
    }

    public ExportOnlyAEFluidSlot() {
        super();
    }

    @Override
    public IAEFluidStack requestStack() {
        IAEFluidStack result = super.requestStack();
        if (result instanceof WrappedFluidStack) {
            return ((WrappedFluidStack) result).getAEStack();
        } else {
            return result;
        }
    }

    @Override
    public IAEFluidStack exceedStack() {
        IAEFluidStack result = super.exceedStack();
        if (result instanceof WrappedFluidStack) {
            return ((WrappedFluidStack) result).getAEStack();
        } else {
            return result;
        }
    }

    @Override
    public void addStack(IAEFluidStack stack) {
        if (this.stock == null) {
            this.stock = WrappedFluidStack.fromFluidStack(stack.getFluidStack());
        } else {
            this.stock.add(stack);
        }
        this.notifyMetaTileEntities();
    }

    @Override
    public void setStack(IAEFluidStack stack) {
        if (this.stock == null && stack == null) {
            return;
        } else if (stack == null) {
            this.stock = null;
        } else if (this.stock == null || this.stock.getFluid() != stack.getFluid()) {
            this.stock = WrappedFluidStack.fromFluidStack(stack.getFluidStack());
        } else if (this.stock.getStackSize() != stack.getStackSize()) {
            this.stock.setStackSize(stack.getStackSize());
        } else return;
        this.notifyMetaTileEntities();
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey(CONFIG_TAG)) {
            this.config = WrappedFluidStack.fromNBT(nbt.getCompoundTag(CONFIG_TAG));
        }
        if (nbt.hasKey(STOCK_TAG)) {
            this.stock = WrappedFluidStack.fromNBT(nbt.getCompoundTag(STOCK_TAG));
        }
    }

    @Nullable
    @Override
    public FluidStack getFluid() {
        if (this.stock != null && this.stock instanceof WrappedFluidStack) {
            return ((WrappedFluidStack) this.stock).getDelegate();
        }
        return null;
    }

    @Override
    public int getFluidAmount() {
        return this.stock != null ? (int) this.stock.getStackSize() : 0;
    }

    @Override
    public int getCapacity() {
        // Its capacity is always 0.
        return 0;
    }

    @Override
    public FluidTankInfo getInfo() {
        return new FluidTankInfo(this);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[] {
                new FluidTankProperties(this.getFluid(), 0)
        };
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        return 0;
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (this.getFluid() != null && this.getFluid().isFluidEqual(resource)) {
            return this.drain(resource.amount, doDrain);
        }
        return null;
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (this.stock == null) {
            return null;
        }
        int drained = (int) Math.min(this.stock.getStackSize(), maxDrain);
        FluidStack result = new FluidStack(this.stock.getFluid(), drained);
        if (doDrain) {
            this.stock.decStackSize(drained);
            if (this.stock.getStackSize() == 0) {
                this.stock = null;
            }
            this.notifyMetaTileEntities();
        }
        return result;
    }

    @Override
    public ExportOnlyAEFluidSlot copy() {
        return new ExportOnlyAEFluidSlot(
                this.holder,
                this.config == null ? null : this.config.copy(),
                this.stock == null ? null : this.stock.copy());
    }

    protected MetaTileEntity getHolder() {
        return holder;
    }

    protected void notifyMetaTileEntities() {
        for (MetaTileEntity metaTileEntity : this.metaTileEntitiesToNotify)
            metaTileEntity.setInventoryNotified(true);
    }

    @Override
    public void addMetaTileEntityToNotifiable(MetaTileEntity metaTileEntity) {
        this.metaTileEntitiesToNotify.add(metaTileEntity);
    }

    @Override
    public void removeMetaTileEntityFromNotifiable(MetaTileEntity metaTileEntity) {
        this.metaTileEntitiesToNotify.remove(metaTileEntity);
    }
}
