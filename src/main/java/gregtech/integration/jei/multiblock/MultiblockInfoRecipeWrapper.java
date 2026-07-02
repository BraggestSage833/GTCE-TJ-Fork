package gregtech.integration.jei.multiblock;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import gregtech.api.GTValues;
import gregtech.api.gui.GuiTextures;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.render.scene.SceneRenderCallback;
import gregtech.api.render.scene.WorldSceneRenderer;
import gregtech.api.util.BlockInfo;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemStackKey;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.integration.jei.multiblock.channel.ChannelDescription;
import gregtech.integration.jei.multiblock.channel.ChannelState;
import gregtech.integration.jei.multiblock.channel.PlaceholderBlockRegistry;
import gregtech.integration.jei.multiblock.channel.StructureChannels;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.gui.recipes.RecipeLayout;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.vecmath.Vector3f;
import java.util.*;
import java.util.Map.Entry;

import static java.lang.Math.max;

public class MultiblockInfoRecipeWrapper implements IRecipeWrapper, SceneRenderCallback {
    private static final int MAX_PARTS = 30;
    private static final int PARTS_HEIGHT = 29;
    private final int SLOT_SIZE = 18;
    private final int SLOTS_PER_ROW = 10;
    private final int ICON_SIZE = 20;
    private final int RIGHT_PADDING = 6;

    private static class MBPattern {
        final WorldSceneRenderer sceneRenderer;
        final List<ItemStack> parts;

        public MBPattern(final WorldSceneRenderer sceneRenderer, final List<ItemStack> parts) {
            this.sceneRenderer = sceneRenderer;
            this.parts = parts;
        }
    }

    private final MultiblockInfoPage infoPage;

    private WorldSceneRenderer renderer = null;
    private List<ItemStack> baseParts;
    private List<ChannelDescription> channels;
    private final ChannelState channelState = new ChannelState();
    private Map<BlockPos, BlockInfo> placeholderBlocks = new HashMap<>();
    private BlockPos controllerPos = null;
    private int currentExtent = 0;

    private final Map<GuiButton, Runnable> buttons = new HashMap<>();
    private RecipeLayout recipeLayout;
    private final List<ItemStack> allItemStackInputs = new ArrayList<>();
    private final ItemStack controllerStack;

    private int layerXIndex;
    private int layerYIndex;
    private int layerZIndex;
    private int currentChannelIndex = 0;
    private int lastMouseX;
    private int lastMouseY;
    private float panX;
    private float panY;
    private float rotationYaw;
    private float rotationPitch;
    private float zoom;
    private boolean isCameraFree;
    private final boolean hasVoltagePages;

    private GuiButton buttonPreviousPattern;
    private GuiButton buttonNextPattern;
    private GuiButton nextLayerXButton;
    private GuiButton nextLayerYButton;
    private GuiButton nextLayerZButton;
    private GuiButton cameraModeButton;

    private IDrawable slot;
    private IDrawable infoIcon;

    private ItemStack tooltipBlockStack;

    public MultiblockInfoRecipeWrapper(MultiblockInfoPage infoPage) {
        this.infoPage = infoPage;
        this.controllerStack = infoPage.getController().getStackForm();
        HashSet<ItemStackKey> drops = new HashSet<>();
        drops.add(new ItemStackKey(controllerStack));
        currentExtent = infoPage.getController().getMinExtent();
        MultiblockShapeInfo shapeInfo = infoPage.getMatchingShapes(currentExtent);

        MBPattern pattern = initializePattern(shapeInfo, drops);
        this.renderer = pattern.sceneRenderer;
        this.baseParts = pattern.parts;

        this.channels = new ArrayList<>();
        for (StructureChannels ch : StructureChannels.values()) {
            if (ChannelDescription.has(ch.get())) {
                this.channels.add(ChannelDescription.get(ch.get()));
            }
        }

        this.hasVoltagePages = shapeInfo.isTiered();

        drops.forEach(it -> allItemStackInputs.add(it.getItemStack()));
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(ItemStack.class, allItemStackInputs);
        ingredients.setOutput(ItemStack.class, controllerStack);
    }

    public MultiblockInfoPage getInfoPage() {
        return infoPage;
    }

    public void setRecipeLayout(RecipeLayout layout, IGuiHelper guiHelper) {
        currentChannelIndex = 0;
        currentExtent = infoPage.getController().getMinExtent();
        this.recipeLayout = layout;

        this.slot = guiHelper.drawableBuilder(GuiTextures.SLOT.imageLocation, 0, 0, SLOT_SIZE, SLOT_SIZE).setTextureSize(SLOT_SIZE, SLOT_SIZE).build();
        this.infoIcon = guiHelper.drawableBuilder(GuiTextures.INFO_ICON.imageLocation, 0, 0, ICON_SIZE, ICON_SIZE).setTextureSize(ICON_SIZE, ICON_SIZE).build();

        IDrawable border = layout.getRecipeCategory().getBackground();
        preparePlaceForParts(border.getHeight());
        this.buttons.clear();
        this.nextLayerXButton = new GuiButton(0, border.getWidth() - (ICON_SIZE + RIGHT_PADDING), 30, ICON_SIZE, ICON_SIZE, "");
        this.nextLayerYButton = new GuiButton(0, border.getWidth() - (ICON_SIZE + RIGHT_PADDING), 50, ICON_SIZE, ICON_SIZE, "");
        this.nextLayerZButton = new GuiButton(0, border.getWidth() - (ICON_SIZE + RIGHT_PADDING), 70, ICON_SIZE, ICON_SIZE, "");
        this.buttonPreviousPattern = new GuiButton(0, border.getWidth() - ((2 * ICON_SIZE) + RIGHT_PADDING + 1), 90, ICON_SIZE, ICON_SIZE, "<");
        this.buttonNextPattern = new GuiButton(0, border.getWidth() - (ICON_SIZE + RIGHT_PADDING), 90, ICON_SIZE, ICON_SIZE, ">");
        this.cameraModeButton = new GuiButton(0, border.getWidth() - ((2 * ICON_SIZE) + RIGHT_PADDING + 1), 70, ICON_SIZE, ICON_SIZE, this.isCameraFree ? "↺" : "↔");

        this.buttons.put(nextLayerXButton, () -> setNextLayerX(Mouse.isButtonDown(0) ? 1 : Mouse.isButtonDown(1) ? -1 : 0));
        this.buttons.put(nextLayerYButton, () -> setNextLayerY(Mouse.isButtonDown(0) ? 1 : Mouse.isButtonDown(1) ? -1 : 0));
        this.buttons.put(nextLayerZButton, () -> setNextLayerZ(Mouse.isButtonDown(0) ? 1 : Mouse.isButtonDown(1) ? -1 : 0));
        this.buttons.put(buttonPreviousPattern, () -> switchChannel(-1));
        this.buttons.put(buttonNextPattern, () -> switchChannel(1));
        this.buttons.put(cameraModeButton, this::setCameraFree);

        this.buttonPreviousPattern.visible = this.hasVoltagePages;
        this.buttonNextPattern.visible = this.hasVoltagePages;
        this.buttonPreviousPattern.enabled = this.hasVoltagePages;
        this.buttonNextPattern.enabled = this.hasVoltagePages;

        this.panX = 0.0f;
        this.panY = 0.0f;
        this.zoom = infoPage.getDefaultZoom();
        this.rotationYaw = -45.0f;
        this.rotationPitch = 0.0f;

        this.layerXIndex = -1;
        this.layerYIndex = -1;
        this.layerZIndex = -1;
        this.nextLayerXButton.displayString = "X:A";
        this.nextLayerYButton.displayString = "Y:A";
        this.nextLayerZButton.displayString = "Z:A";
        rebuildScene();
        updateParts();
    }

    public WorldSceneRenderer getCurrentRenderer() {
        return renderer;
    }

    private void setCameraFree() {
        this.isCameraFree = !this.isCameraFree;
        this.cameraModeButton.displayString = this.isCameraFree ? "↺" : "↔";
    }

    public int getLayerYIndex() {
        return layerYIndex;
    }

    private void setNextLayerX(int newLayer) {
        WorldSceneRenderer renderer = getCurrentRenderer();
        int width = (int) renderer.getSize().getX() - 1;
        newLayer = this.layerXIndex + newLayer;
        if (newLayer > width) {
            newLayer = -1;
        } else if (newLayer < -1) {
            newLayer = width;
        }
        this.layerXIndex = max(-1, newLayer);
        this.nextLayerXButton.displayString = "X:" + (layerXIndex == -1 ? "A" : Integer.toString(layerXIndex + 1));
        renderer.markGeometryDirty();
    }

    private void setNextLayerY(int newLayer) {
        WorldSceneRenderer renderer = getCurrentRenderer();
        int height = (int) renderer.getSize().getY() - 1;
        newLayer = this.layerYIndex + newLayer;
        if (newLayer > height) {
            newLayer = -1;
        } else if (newLayer < -1) {
            newLayer = height;
        }
        this.layerYIndex = max(-1, newLayer);
        this.nextLayerYButton.displayString = "Y:" + (layerYIndex == -1 ? "A" : Integer.toString(layerYIndex + 1));
        renderer.markGeometryDirty();
    }

    private void setNextLayerZ(int newLayer) {
        WorldSceneRenderer renderer = getCurrentRenderer();
        int depth = (int) renderer.getSize().getZ() - 1;
        newLayer = this.layerZIndex + newLayer;
        if (newLayer > depth) {
            newLayer = -1;
        } else if (newLayer < -1) {
            newLayer = depth;
        }
        this.layerZIndex = max(-1, newLayer);
        this.nextLayerZButton.displayString = "Z:" + (layerZIndex == -1 ? "A" : Integer.toString(layerZIndex + 1));
        renderer.markGeometryDirty();
    }

    private void rebuildScene() {
        HashSet<ItemStackKey> drops = new HashSet<>();
        drops.add(new ItemStackKey(controllerStack));

        placeholderBlocks = new HashMap<>();

        MultiblockShapeInfo shapeInfo = infoPage.getMatchingShapes(currentExtent);
        MBPattern pattern = initializePattern(shapeInfo, drops);
        this.renderer = pattern.sceneRenderer;
        this.baseParts = pattern.parts;

        controllerPos = null;
        findControllerPos(renderer.getBlockInfoMap());

        updateParts();
    }

    private void switchChannel(int amount) {
        int maxIndex = 14;
        int newIndex = max(0, Math.min(currentChannelIndex + amount, maxIndex));

        if (currentChannelIndex == newIndex) return;

        this.buttonPreviousPattern.enabled = newIndex > 0;
        this.buttonNextPattern.enabled = newIndex < maxIndex;

        currentChannelIndex = newIndex;


        for (StructureChannels ch : StructureChannels.values()) {
            channelState.set(ch, newIndex);
        }

        MultiblockControllerBase controller = infoPage.getController();
        if (controller.getMaxExtent() != 1) {
            int minExtent = controller.getMinExtent();
            int maxExtent = controller.getMaxExtent();
            currentExtent = Math.min(maxExtent, minExtent + currentChannelIndex);

            rebuildScene();

            this.layerXIndex = -1;
            this.layerYIndex = -1;
            this.layerZIndex = -1;
            this.nextLayerXButton.displayString = "X:A";
            this.nextLayerYButton.displayString = "Y:A";
            this.nextLayerZButton.displayString = "Z:A";
        } else {
            rebuildScene();
            triggerStructureCheck(renderer.world);
        }
    }

    private void triggerStructureCheck(WorldSceneRenderer.TrackedDummyWorld world) {
        if (controllerPos == null) return;
        TileEntity te = world.getTileEntity(controllerPos);
        if (te instanceof MetaTileEntityHolder) {
            MetaTileEntity mte = ((MetaTileEntityHolder) te).getMetaTileEntity();
            if (mte instanceof MultiblockControllerBase) {
                MultiblockControllerBase ctrl = (MultiblockControllerBase) mte;
                ctrl.invalidateStructure();
                ctrl.checkStructurePatternJEI();
            }
        }
    }

    private void preparePlaceForParts(int recipeHeight) {
        IGuiItemStackGroup itemStackGroup = recipeLayout.getItemStacks();
        for (int i = 0; i < MAX_PARTS; ++i)
            itemStackGroup.init(i, true, SLOT_SIZE * i - (SLOT_SIZE * SLOTS_PER_ROW) * (i / SLOTS_PER_ROW), recipeHeight - PARTS_HEIGHT + SLOT_SIZE * (i / SLOTS_PER_ROW));
    }

    private void updateParts() {
        IGuiItemStackGroup itemStackGroup = recipeLayout.getItemStacks();
        int limit = Math.min(baseParts.size(), MAX_PARTS);
        for (int i = 0; i < limit; ++i)
            itemStackGroup.set(i, baseParts.get(i));
        for (int i = baseParts.size(); i < limit; ++i)
            itemStackGroup.set(i, (ItemStack) null);
    }

    private boolean shouldDisplayBlock(BlockPos pos) {
        if (this.layerXIndex == -1 && getLayerYIndex() == -1 && this.layerZIndex == -1)
            return true;
        WorldSceneRenderer renderer = getCurrentRenderer();
        int relativeWidth = pos.getX() - (int) renderer.world.getMinPos().getX();
        int relativeHeight = pos.getY() - (int) renderer.world.getMinPos().getY();
        int relativeDepth = pos.getZ() - (int) renderer.world.getMinPos().getZ();
        return (this.layerXIndex == -1 || this.layerXIndex == relativeWidth) && (getLayerYIndex() == -1 || relativeHeight == getLayerYIndex()) && (this.layerZIndex == -1 || this.layerZIndex == relativeDepth);
    }

    @Override
    public void preRenderScene(WorldSceneRenderer renderer) {
        Vector3f size = renderer.getSize();
        Vector3f minPos = renderer.world.getMinPos();
        minPos = new Vector3f(minPos);
        minPos.add(new Vector3f(0.0f, -1.0f, 0.5f));

        GlStateManager.scale(zoom, zoom, zoom);
        GlStateManager.translate(panX, panY, 0);
        GlStateManager.translate(-minPos.x, -minPos.y, -minPos.z);
        Vector3 centerPosition = new Vector3(size.x / 2.0f, size.y / 2.0f, size.z / 2.0f);
        GlStateManager.translate(centerPosition.x, centerPosition.y, centerPosition.z);
        GlStateManager.scale(2.0, 2.0, 2.0);
        GlStateManager.translate(-centerPosition.x, -centerPosition.y, -centerPosition.z);
        GlStateManager.translate(minPos.x, minPos.y, minPos.z);

        GlStateManager.translate(centerPosition.x, centerPosition.y, centerPosition.z);
        GlStateManager.rotate(rotationYaw, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(rotationPitch, 0.0f, 0.0f, 1.0f);
        GlStateManager.translate(-centerPosition.x, -centerPosition.y, -centerPosition.z);

        if (layerYIndex >= 0) {
            GlStateManager.translate(0.0, -layerYIndex + 1, 0.0);
        }
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        WorldSceneRenderer renderer = getCurrentRenderer();
        int sceneHeight = recipeHeight - PARTS_HEIGHT;

        renderer.render(recipeLayout.getPosX(), recipeLayout.getPosY(), recipeWidth, sceneHeight, 0xC6C6C6);
        drawMultiblockName(recipeWidth);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        this.infoIcon.draw(minecraft, recipeWidth - (ICON_SIZE + RIGHT_PADDING), 9);

        if (this.hasVoltagePages) {
            GuiTextures.DISPLAY.draw(recipeWidth - (ICON_SIZE + ICON_SIZE + RIGHT_PADDING), 110, 40, 20);
            String text = (this.currentChannelIndex == 9 ? TextFormatting.DARK_RED.toString() : GTUtility.TIER_COLOR[this.currentChannelIndex]) + GTValues.VN2[this.currentChannelIndex];
            Minecraft.getMinecraft().fontRenderer.drawString(text, recipeWidth - 30 - (GTValues.VN2[this.currentChannelIndex].length() > 2 ? 4 : 0), 116, 0xFFFFFF);
        }

        for (int i = 0; i < MAX_PARTS; ++i) {
            this.slot.draw(minecraft, SLOT_SIZE * i - (SLOTS_PER_ROW * SLOT_SIZE) * (i / SLOTS_PER_ROW), sceneHeight + SLOT_SIZE * (i / SLOTS_PER_ROW));
        }

        for (GuiButton button : buttons.keySet()) {
            button.drawButton(minecraft, mouseX, mouseY, 0.0f);
        }

        drawHoveringInformationText(minecraft, infoPage.informationText(), mouseX, mouseY);

        this.tooltipBlockStack = null;
        BlockPos pos = renderer.getLastHitBlock();
        boolean insideView = mouseX >= 0 && mouseY >= 0 && mouseX < recipeWidth && mouseY < sceneHeight;
        boolean leftClickHeld = Mouse.isButtonDown(0);
        boolean rightClickHeld = Mouse.isButtonDown(1);
        boolean isHoldingShift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);

        if (insideView) {
            if (leftClickHeld) {
                int mouseDeltaY = mouseY - this.lastMouseY;
                int mouseDeltaX = mouseX - this.lastMouseX;
                if (this.isCameraFree) {
                    this.rotationPitch += mouseDeltaY * 2.0f;
                    this.rotationYaw += mouseDeltaX * 2.0f;
                } else if (isHoldingShift) {
                    this.rotationPitch += mouseDeltaY * 2.0f;
                } else {
                    this.rotationYaw += mouseDeltaX * 2.0f;
                }
            } else if (rightClickHeld) {
                int mouseDeltaY = mouseY - lastMouseY;
                if (isHoldingShift) {
                    this.zoom *= Math.pow(1.05d, -mouseDeltaY);
                } else {
                    int mouseDeltaX = mouseX - lastMouseX;
                    this.panX -= mouseDeltaX / 2.0f;
                    this.panY -= mouseDeltaY / 2.0f;
                }
            }
        }

        if (!(leftClickHeld || rightClickHeld) && pos != null && !renderer.world.isAirBlock(pos)) {
            IBlockState blockState = renderer.world.getBlockState(pos);
            RayTraceResult result = new CuboidRayTraceResult(new Vector3(0.5, 0.5, 0.5).add(pos), pos, EnumFacing.UP, new IndexedCuboid6(null, Cuboid6.full), 1.0);
            ItemStack itemStack = blockState.getBlock().getPickBlock(blockState, result, renderer.world, pos, minecraft.player);
            if (itemStack != null && !itemStack.isEmpty()) {
                this.tooltipBlockStack = itemStack;
            }
        }

        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    @SideOnly(Side.CLIENT)
    protected void drawHoveringInformationText(Minecraft minecraft, List<String> tooltip, int mouseX, int mouseY) {
        int minX = recipeLayout.getRecipeCategory().getBackground().getWidth();
        int[] yRange = new int[]{9, 29};
        int[] xRange = new int[]{minX - (ICON_SIZE + RIGHT_PADDING), minX - RIGHT_PADDING};
        if (isMouseWithinRange(yRange, xRange, mouseY, mouseX)) {
            GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, 176, 176, -1, minecraft.fontRenderer);
        }
    }

    private boolean isMouseWithinRange(int[] yRange, int[] xRange, int mouseY, int mouseX) {
        return (yRange[0] < mouseY && mouseY < yRange[1] && xRange[0] < mouseX && mouseX < xRange[1]);
    }

    private void drawMultiblockName(int recipeWidth) {
        String localizedName = I18n.format(infoPage.getController().getMetaFullName());
        GTUtility.drawCenteredSizedText(recipeWidth / 2, 0, localizedName, 0x333333, 1.3);
    }

    @Override
    public boolean handleClick(Minecraft minecraft, int mouseX, int mouseY, int mouseButton) {
        for (Entry<GuiButton, Runnable> button : buttons.entrySet()) {
            if (button.getKey().mousePressed(minecraft, mouseX, mouseY)) {
                button.getValue().run();
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        if (tooltipBlockStack != null && !tooltipBlockStack.isEmpty() && !Mouse.isButtonDown(0)) {
            Minecraft minecraft = Minecraft.getMinecraft();
            ITooltipFlag flag = minecraft.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL;
            List<String> tooltip = tooltipBlockStack.getTooltip(minecraft.player, flag);
            EnumRarity rarity = tooltipBlockStack.getRarity();
            for (int k = 0; k < tooltip.size(); ++k) {
                if (k == 0) {
                    tooltip.set(k, rarity.color + tooltip.get(k));
                } else {
                    tooltip.set(k, TextFormatting.GRAY + tooltip.get(k));
                }
            }
            Map<ItemStack, List<ITextComponent>> blockTooltipMap = infoPage.getBlockTooltipMap();
            if (blockTooltipMap.containsKey(tooltipBlockStack)) {
                List<ITextComponent> tooltips = blockTooltipMap.get(tooltipBlockStack);
                for (int i = 0; i < tooltips.size(); i++) {
                    tooltip.add(i + 1, tooltips.get(i).getFormattedText());
                }
            }
            return tooltip;
        }
        return Collections.emptyList();
    }

    public void addBlockTooltips(int slotIndex, boolean input, ItemStack itemStack, List<String> tooltip) {
        Map<ItemStack, List<ITextComponent>> blockTooltipMap = infoPage.getBlockTooltipMap();
        if (blockTooltipMap.containsKey(itemStack)) {
            List<ITextComponent> tooltips = blockTooltipMap.get(itemStack);
            for (int i = 0; i < tooltips.size(); i++) {
                tooltip.add(i + 1, tooltips.get(i).getFormattedText());
            }
        }
    }

    private static class PartInfo {
        final ItemStackKey itemStackKey;
        boolean isController = false;
        boolean isTile = false;
        final int blockId;
        int amount = 0;

        PartInfo(final ItemStackKey itemStackKey, final BlockInfo blockInfo) {
            this.itemStackKey = itemStackKey;
            this.blockId = Block.getIdFromBlock(blockInfo.getBlockState().getBlock());
            TileEntity tileEntity = blockInfo.getTileEntity();
            if (tileEntity != null) {
                this.isTile = true;
                MetaTileEntity mte = ((MetaTileEntityHolder) tileEntity).getMetaTileEntity();
                if (mte instanceof MultiblockControllerBase)
                    this.isController = true;
            }
        }

        ItemStack getItemStack() {
            ItemStack result = this.itemStackKey.getItemStack();
            result.setCount(this.amount);
            return result;
        }
    }

    private static void gatherBlockDrops(World world, Map<BlockPos, BlockInfo> blocks, Set<ItemStackKey> drops, Map<ItemStackKey, PartInfo> partsMap) {
        NonNullList<ItemStack> dropsList = NonNullList.create();
        for (Entry<BlockPos, BlockInfo> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            IBlockState blockState = world.getBlockState(pos);
            NonNullList<ItemStack> blockDrops = NonNullList.create();
            blockState.getBlock().getDrops(blockDrops, world, pos, blockState, 0);
            dropsList.addAll(blockDrops);

            for (ItemStack itemStack : blockDrops) {
                ItemStackKey itemStackKey = new ItemStackKey(itemStack);
                PartInfo partInfo = partsMap.get(itemStackKey);
                if (partInfo == null) {
                    partInfo = new PartInfo(itemStackKey, entry.getValue());
                    partsMap.put(itemStackKey, partInfo);
                }
                ++partInfo.amount;
            }
        }
        for (ItemStack itemStack : dropsList) {
            drops.add(new ItemStackKey(itemStack));
        }
    }

    private Map<BlockPos, BlockInfo> createBlockMap(MultiblockShapeInfo shapeInfo) {
        Map<BlockPos, BlockInfo> blockMap = new HashMap<>();
        this.placeholderBlocks = new HashMap<>();
        BlockInfo[][][] blocks = shapeInfo.getBlocks();

        int coilTier = Math.min(channelState.get(StructureChannels.COIL), 16);
        int voltageTier = Math.min(channelState.get(StructureChannels.VOLTAGE), 14);

        for (int z = 0; z < blocks.length; z++) {
            BlockInfo[][] aisle = blocks[z];
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int x = 0; x < column.length; x++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockInfo blockInfo = column[x];

                    if (blockInfo.getBlockState() == null && blockInfo.getPlaceHolderType() == null) continue;

                    if (blockInfo.getPlaceHolderType() == null) {
                        blockMap.put(blockPos, blockInfo);
                        continue;
                    }

                    placeholderBlocks.put(blockPos, blockInfo);

                    EnumFacing facing = EnumFacing.NORTH;
                    TileEntity originalTe = blockInfo.getTileEntity();
                    if (originalTe instanceof MetaTileEntityHolder) {
                        facing = ((MetaTileEntityHolder) originalTe).getMetaTileEntity().getFrontFacing();
                    }

                    PlaceholderBlockRegistry.PlaceholderContext context = new PlaceholderBlockRegistry.PlaceholderContext(
                            voltageTier, coilTier, facing, blockPos, infoPage.getController().getMinTier());

                    BlockInfo resolved = PlaceholderBlockRegistry.resolve(blockInfo.getPlaceHolderType(), context);
                    if (resolved == null) continue;
                    blockMap.put(blockPos, resolved);
                }
            }
        }
        return blockMap;
    }

    private void findControllerPos(Map<BlockPos, BlockInfo> blockMap) {
        for (Map.Entry<BlockPos, BlockInfo> entry : blockMap.entrySet()) {
            TileEntity te = entry.getValue().getTileEntity();
            if (te instanceof MetaTileEntityHolder) {
                MetaTileEntity mte = ((MetaTileEntityHolder) te).getMetaTileEntity();
                if (mte instanceof MultiblockControllerBase) {
                    controllerPos = entry.getKey();
                    break;
                }
            }
        }
    }

    private MBPattern initializePattern(MultiblockShapeInfo shapeInfo, Set<ItemStackKey> blockDrops) {
        Map<BlockPos, BlockInfo> blockMap = createBlockMap(shapeInfo);

        findControllerPos(blockMap);

        WorldSceneRenderer worldSceneRenderer = new WorldSceneRenderer(blockMap);
        worldSceneRenderer.world.updateEntities();
        HashMap<ItemStackKey, PartInfo> partsMap = new HashMap<>();
        gatherBlockDrops(worldSceneRenderer.world, blockMap, blockDrops, partsMap);
        worldSceneRenderer.setRenderCallback(this);
        worldSceneRenderer.setRenderFilter(this::shouldDisplayBlock);
        ArrayList<PartInfo> partInfos = new ArrayList<>(partsMap.values());
        partInfos.sort((one, two) -> {
            if (one.isController) return -1;
            if (two.isController) return +1;
            if (one.isTile && !two.isTile) return -1;
            if (two.isTile && !one.isTile) return +1;
            if (one.blockId != two.blockId) return two.blockId - one.blockId;
            return two.amount - one.amount;
        });
        ArrayList<ItemStack> parts = new ArrayList<>();
        for (PartInfo partInfo : partInfos) {
            parts.add(partInfo.getItemStack());
        }

        return new MBPattern(worldSceneRenderer, parts);
    }
}
