package gregtech.common.metatileentities.storage;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.google.common.base.Preconditions;
import gregtech.api.GTValues;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.*;
import gregtech.api.gui.widgets.tab.ItemTabInfo;
import gregtech.api.items.toolitem.ToolMetaItem;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.render.Textures;
import gregtech.api.util.Position;
import gregtech.common.gui.widget.CraftingSlotWidget;
import gregtech.common.gui.widget.ItemListGridWidget;
import gregtech.common.gui.widget.MemorizedRecipeWidget;
import gregtech.common.inventory.IItemList;
import gregtech.common.inventory.itemsource.ItemSourceList;
import gregtech.common.inventory.itemsource.sources.InventoryItemSource;
import gregtech.common.inventory.itemsource.sources.TileItemSource;
import gregtech.common.metatileentities.electric.multiblockpart.appeng.MetaTileEntityAEHostablePart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MetaTileEntityMECraftingStation extends MetaTileEntityAEHostablePart<IAEItemStack> {

    private final MEItemStackHandler meInventory = new MEItemStackHandler(this::getMonitor, this::getActionSource);

    private final ItemStackHandler internalInventory = new ItemStackHandler(18);

    private final ItemStackHandler craftingGrid = new ItemStackHandler(9) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final ItemStackHandler toolInventory = new ItemStackHandler(9) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (!(stack.getItem() instanceof ToolMetaItem) &&
                    !(stack.getItem() instanceof ItemTool) &&
                    !(stack.isItemStackDamageable())) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }
    };

    private final CraftingRecipeMemory recipeMemory = new CraftingRecipeMemory(9);
    private CraftingRecipeResolver recipeResolver = null;
    private int itemsCrafted = 0;

    public MetaTileEntityMECraftingStation(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, GTValues.MV, IItemStorageChannel.class);

    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMECraftingStation(this.metaTileEntityId);
    }

    @Override
    public EnumSet<EnumFacing> getConnectableSides() {
        return EnumSet.of(EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST);
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote) {
            if (recipeResolver == null) {
                this.createRecipeResolver();
                this.updateConnectableSides();
            } else getRecipeResolver().update();
            if (this.getOffsetTimer() % 100 == 0) {
                this.updateMEStatus();
                this.refreshME();
            }
        }
    }

    private void refreshME() {
        IMEMonitor<IAEItemStack> monitor = this.getMonitor();
        if (monitor == null) {
            this.clearMEInventory();
            return;
        }
        int i = 0;
        this.meInventory.setSize(monitor.getStorageList().size());
        this.recipeResolver.getItemSourceList().addItemHandler(new TileItemSource(this.getWorld(), this.getPos().up(), EnumFacing.DOWN));
        for (IAEItemStack stack : monitor.getStorageList()) {
            if (i >= this.meInventory.getSlots()) break;
            if (stack.getStackSize() == 0) continue;
            this.meInventory.setStackInSlot(i++, stack.createItemStack());
        }
    }

    @Override
    public void clearMachineInventory(NonNullList<ItemStack> itemBuffer) {
        super.clearMachineInventory(itemBuffer);
        clearInventory(itemBuffer, internalInventory);
        clearInventory(itemBuffer, toolInventory);
    }

    private void clearMEInventory() {
        for (int i = 0; i < this.meInventory.getSlots(); i++)
            this.meInventory.setStackInSlot(i, ItemStack.EMPTY);
    }

    private void createRecipeResolver() {
        this.recipeResolver = new CraftingRecipeResolver(getWorld(), craftingGrid, recipeMemory);
        this.recipeResolver.setItemsCrafted(itemsCrafted);
        ItemSourceList itemSourceList = this.recipeResolver.getItemSourceList();
        itemSourceList.addItemHandler(InventoryItemSource.direct(getWorld(), toolInventory, -2));
        itemSourceList.addItemHandler(InventoryItemSource.direct(getWorld(), internalInventory, -1));
        itemSourceList.addItemHandler(new TileItemSource(this.getWorld(), this.getPos().up(), EnumFacing.DOWN));
    }

    private CraftingRecipeResolver getRecipeResolver() {
        Preconditions.checkState(getWorld() != null, "getRecipeResolver called too early");
        return recipeResolver;
    }

    @Override
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BORDERED_BACKGROUND, 176, 221)
                .bindPlayerInventory(entityPlayer.inventory, 140);
        builder.label(5, 5, getMetaFullName());

        TabGroup tabGroup = new TabGroup(TabGroup.TabLocation.HORIZONTAL_TOP_LEFT, Position.ORIGIN);
        tabGroup.addTab(new ItemTabInfo("gregtech.machine.workbench.tab.workbench", new ItemStack(Blocks.CRAFTING_TABLE)), createWorkbenchTab());
        tabGroup.addTab(new ItemTabInfo("gregtech.machine.workbench.tab.item_list", new ItemStack(Blocks.CHEST)), createItemListTab());
        builder.widget(tabGroup);

        return builder.build(getHolder(), entityPlayer);
    }

    private AbstractWidgetGroup createWorkbenchTab() {
        WidgetGroup widgetGroup = new WidgetGroup();
        CraftingRecipeResolver recipeResolver = getRecipeResolver();

        widgetGroup.addWidget(new ImageWidget(88 - 13, 44 - 13, 26, 26, GuiTextures.SLOT));
        widgetGroup.addWidget(new CraftingSlotWidget(recipeResolver, 0, 88 - 9, 44 - 9));

        //crafting grid
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                widgetGroup.addWidget(new PhantomSlotWidget(craftingGrid, j + i * 3, 8 + j * 18, 17 + i * 18).setBackgroundTexture(GuiTextures.SLOT));
            }
        }
        Supplier<String> textSupplier = () -> Integer.toString(recipeResolver.getItemsCrafted());
        widgetGroup.addWidget(new SimpleTextWidget(88, 44 + 20, "", textSupplier));

        Consumer<Widget.ClickData> clearAction = (clickData) -> recipeResolver.clearCraftingGrid();
        widgetGroup.addWidget(new ClickButtonWidget(8 + 18 * 3 + 1, 17, 8, 8, "", clearAction).setButtonTexture(GuiTextures.BUTTON_CLEAR_GRID));

        widgetGroup.addWidget(new ImageWidget(168 - 18 * 3, 44 - 18 * 3 / 2, 18 * 3, 18 * 3, TextureArea.fullImage("textures/gui/base/darkened_slot.png")));
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                widgetGroup.addWidget(new MemorizedRecipeWidget(recipeMemory, j + i * 3, craftingGrid, 168 - 18 * 3 / 2 - 27 + j * 18, 44 - 27 + i * 18));
            }
        }
        //tool inventory
        for (int i = 0; i < 9; i++) {
            widgetGroup.addWidget(new SlotWidget(toolInventory, i, 8 + i * 18, 76).setBackgroundTexture(GuiTextures.SLOT, GuiTextures.TOOL_SLOT_OVERLAY));
        }
        //internal inventory
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 9; ++j) {
                widgetGroup.addWidget(new SlotWidget(internalInventory, j + i * 9, 8 + j * 18, 99 + i * 18).setBackgroundTexture(GuiTextures.SLOT));
            }
        }
        return widgetGroup;
    }

    private AbstractWidgetGroup createItemListTab() {
        WidgetGroup widgetGroup = new WidgetGroup();
        widgetGroup.addWidget(new LabelWidget(5, 20, "gregtech.machine.workbench.storage_note_1"));
        widgetGroup.addWidget(new LabelWidget(5, 30, "gregtech.machine.workbench.storage_note_2"));
        CraftingRecipeResolver recipeResolver = getRecipeResolver();
        IItemList itemList = recipeResolver == null ? null : recipeResolver.getItemSourceList();
        widgetGroup.addWidget(new ItemListGridWidget(2, 45, 9, 5, itemList));
        return widgetGroup;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.CRAFTING_TABLE.render(renderState, translation, pipeline, Cuboid6.full, this.getFrontFacing());
        for (EnumFacing facing : this.getConnectableSides())
            Textures.ME_INPUT_BUS_OVERLAY.render(renderState, translation, pipeline, facing, this.isOnline);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("CraftingGridInventory", craftingGrid.serializeNBT());
        data.setTag("ToolInventory", toolInventory.serializeNBT());
        data.setTag("InternalInventory", internalInventory.serializeNBT());
        data.setInteger("ItemsCrafted", recipeResolver == null ? itemsCrafted : recipeResolver.getItemsCrafted());
        data.setTag("RecipeMemory", recipeMemory.serializeNBT());
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.craftingGrid.deserializeNBT(data.getCompoundTag("CraftingGridInventory"));
        this.toolInventory.deserializeNBT(data.getCompoundTag("ToolInventory"));
        this.internalInventory.deserializeNBT(data.getCompoundTag("InternalInventory"));
        this.itemsCrafted = data.getInteger("ItemsCrafted");
        this.recipeMemory.deserializeNBT(data.getCompoundTag("RecipeMemory"));
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.meInventory);
        return super.getCapability(capability, side);
    }

    private static class MEItemStackHandler extends ItemStackHandler {

        private final Supplier<IMEMonitor<IAEItemStack>> monitor;
        private final Supplier<IActionSource> source;

        public MEItemStackHandler(Supplier<IMEMonitor<IAEItemStack>> monitor, Supplier<IActionSource> source) {
            super(45);
            this.monitor = monitor;
            this.source = source;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            IAEItemStack insert = AEItemStack.fromItemStack(stack);
            if (insert == null || this.monitor.get() == null)
                return stack;
            insert = this.monitor.get().injectItems(insert, simulate ? Actionable.SIMULATE : Actionable.MODULATE, this.source.get());
            return insert != null ? insert.createItemStack() : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            IAEItemStack extract = AEItemStack.fromItemStack(this.getStackInSlot(slot));
            if (extract == null || this.monitor.get() == null)
                return ItemStack.EMPTY;
            extract.setStackSize(amount);
            extract = this.monitor.get().extractItems(extract, simulate ? Actionable.SIMULATE : Actionable.MODULATE, this.source.get());
            return extract != null ? extract.createItemStack() : ItemStack.EMPTY;
        }
    }
}
