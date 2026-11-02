package gregtech.common.render;

import codechicken.lib.lighting.LightMatrix;
import codechicken.lib.render.BlockRenderer;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.block.BlockRenderingRegistry;
import codechicken.lib.render.block.ICCBlockRenderer;
import codechicken.lib.render.item.IItemRenderer;
import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.util.TransformUtils;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.block.ItemBlockPipe;
import gregtech.api.pipenet.block.material.BlockMaterialPipe;
import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.PipeCoverableImplementation;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import gregtech.api.render.Textures;
import gregtech.api.unification.material.type.Material;
import gregtech.api.util.GTUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@SideOnly(Side.CLIENT)
public abstract class PipeRenderer implements ICCBlockRenderer, IItemRenderer {
    public final ModelResourceLocation modelResourceLocation;
    public final String name;
    private EnumBlockRenderType renderType;
    protected static final ThreadLocal<BlockRenderer.BlockFace> blockFaces = ThreadLocal.withInitial(BlockRenderer.BlockFace::new);

    private static final EnumMap<EnumFacing, EnumMap<Border, EnumFacing>> FACE_BORDER_MAP = new EnumMap<>(
            EnumFacing.class);
    private static final Int2ObjectMap<IVertexOperation[]> RESTRICTOR_MAP = new Int2ObjectOpenHashMap<>();

    public static void initializeRestrictor(TextureMap map) {
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_UP, Border.TOP);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_DOWN, Border.BOTTOM);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_UD, Border.TOP, Border.BOTTOM);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_LEFT, Border.LEFT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_UL, Border.TOP, Border.LEFT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_DL, Border.BOTTOM, Border.LEFT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_NR, Border.TOP, Border.BOTTOM, Border.LEFT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_RIGHT, Border.RIGHT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_UR, Border.TOP, Border.RIGHT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_DR, Border.BOTTOM, Border.RIGHT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_NL, Border.TOP, Border.BOTTOM, Border.RIGHT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_LR, Border.LEFT, Border.RIGHT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_ND, Border.TOP, Border.LEFT, Border.RIGHT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY_NU, Border.BOTTOM, Border.LEFT, Border.RIGHT);
        addRestrictor(Textures.PIPE_BLOCKED_OVERLAY, Border.TOP, Border.BOTTOM, Border.LEFT, Border.RIGHT);
    }

    static {
        FACE_BORDER_MAP.put(EnumFacing.DOWN,
                borderMap(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST));
        FACE_BORDER_MAP.put(EnumFacing.UP,
                borderMap(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST));
        FACE_BORDER_MAP.put(EnumFacing.NORTH,
                borderMap(EnumFacing.UP, EnumFacing.DOWN, EnumFacing.EAST, EnumFacing.WEST));
        FACE_BORDER_MAP.put(EnumFacing.SOUTH,
                borderMap(EnumFacing.UP, EnumFacing.DOWN, EnumFacing.WEST, EnumFacing.EAST));
        FACE_BORDER_MAP.put(EnumFacing.WEST,
                borderMap(EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH));
        FACE_BORDER_MAP.put(EnumFacing.EAST,
                borderMap(EnumFacing.UP, EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.NORTH));
    }


    public PipeRenderer(String name, ModelResourceLocation resourceLocation) {
        this.name = name;
        this.modelResourceLocation = resourceLocation;
    }

    public PipeRenderer(String name, ResourceLocation modelLocation) {
        this(name, new ModelResourceLocation(modelLocation, "normal"));
    }

    public void preInit() {
        renderType = BlockRenderingRegistry.createRenderType(name);
        BlockRenderingRegistry.registerRenderer(renderType,this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public ModelResourceLocation getModelResourceLocation() {
        return modelResourceLocation;
    }

    public EnumBlockRenderType getBlockRenderType() {
        return renderType;
    }

    public void registerIcon(TextureMap texMap) {}

    @SubscribeEvent
    public void onModelsBake(ModelBakeEvent event) {
        event.getModelRegistry().putObject(modelResourceLocation, this);
    }

    public abstract void buildRenderer(PipeRenderContext context, BlockPipe<?,?,?> block,
                                       @Nullable IPipeTile<?,?> tile, IPipeType<?> type,
                                       @Nullable Material material);


    @Override
    public void handleRenderBlockDamage(IBlockAccess world, BlockPos pos, IBlockState state, TextureAtlasSprite sprite, BufferBuilder buffer) {
        CCRenderState renderState = CCRenderState.instance();
        renderState.reset();
        renderState.bind(buffer);
        renderState.setPipeline(new Vector3(new Vec3d(pos)).translation(), new IconTransformation(sprite));
        BlockPipe<?, ?, ?> blockPipe = (BlockPipe<?, ?, ?>) state.getBlock();
        IPipeTile<?, ?> pipeTile = blockPipe.getPipeTileEntity(world, pos);
        if (pipeTile == null) {
            return;
        }

        IPipeType<?> pipeType = pipeTile.getPipeType();
        if (pipeType == null) {
            return;
        }

        float thickness = pipeType.getThickness();
        int unconnectedSides = pipeTile.getUnconnectedSides();
        Cuboid6 baseBox = BlockPipe.getSideBox(null, thickness);
        BlockRenderer.renderCuboid(renderState, baseBox, 0);
        for (EnumFacing renderSide : EnumFacing.VALUES) {
            if ((unconnectedSides & (1 << renderSide.getIndex())) == 0) {
                Cuboid6 sideBox = BlockPipe.getSideBox(renderSide, thickness);
                BlockRenderer.renderCuboid(renderState, sideBox, 0);
            }
        }
    }

    @Override
    public boolean renderBlock(IBlockAccess world, BlockPos pos, IBlockState state, BufferBuilder buffer) {
        CCRenderState renderState = CCRenderState.instance();
        renderState.reset();
        renderState.bind(buffer);
        renderState.setBrightness(world, pos);

        BlockPipe<?, ?, ?> blockPipe = (BlockPipe<?, ?, ?>) state.getBlock();
        IPipeTile<?, ?> pipeTile = blockPipe.getPipeTileEntity(world, pos);

        if (pipeTile == null) {
            return false;
        }

        IPipeType<?> pipeType = pipeTile.getPipeType();
        Material pipeMaterial = pipeTile instanceof TileEntityMaterialPipeBase ?
                ((TileEntityMaterialPipeBase<?, ?>) pipeTile).getPipeMaterial() : null;
        int paintingColor = pipeTile.getInsulationColor();
        int connectedSidesMap = pipeTile.getVisualConnections();
        int blockedConnections = pipeTile.getBlockedConnections();

        if (pipeType != null) {
            BlockRenderLayer renderLayer = MinecraftForgeClient.getRenderLayer();
            boolean[] sideMask = new boolean[EnumFacing.VALUES.length];
            for (EnumFacing side : EnumFacing.VALUES) {
                sideMask[side.getIndex()] = state.shouldSideBeRendered(world, pos, side);
            }
            Textures.RENDER_STATE.set(new CubeRendererState(renderLayer, sideMask, world));
            if (canRenderInLayer(renderLayer)) {
                renderState.lightMatrix.locate(world, pos);
                PipeRenderContext renderContext = new PipeRenderContext(pos, renderState.lightMatrix, connectedSidesMap,
                        blockedConnections, pipeType.getThickness());
                renderContext.color = GTUtility.convertRGBtoOpaqueRGBA_CL(getPipeColor(pipeMaterial, paintingColor));
                buildRenderer(renderContext, blockPipe, pipeTile, pipeType, pipeMaterial);
                if (renderLayer == BlockRenderLayer.CUTOUT) {
                    renderPipeBlock(renderState, renderContext);
                } else {
                    renderOtherLayers(renderLayer, renderState, renderContext);
                }
            }


            PipeCoverableImplementation pipeCover = pipeTile.getCoverableImplementation();
            pipeCover.renderCovers(renderState, new Matrix4().translate(pos.getX(), pos.getY(), pos.getZ()), renderLayer);
            Textures.RENDER_STATE.remove();
        }
        return true;
    }

    @Override
    public void renderBrightness(IBlockState state, float brightness) {

    }

    @Override
    public void registerTextures(TextureMap map) {

    }

    @Override
    public void renderItem(ItemStack stack, ItemCameraTransforms.TransformType transformType) {
       if (!(stack.getItem() instanceof ItemBlockPipe)) {
           return;
       }
        CCRenderState renderState = CCRenderState.instance();
        GlStateManager.enableBlend();
        renderState.reset();
        renderState.startDrawing(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
        BlockPipe<?,?,?> fluidBlock = (BlockPipe<?, ?, ?>) ((ItemBlockPipe<?, ?>) stack.getItem()).getBlock();
        IPipeType<?> pipeType = fluidBlock.getItemPipeType(stack);

        Material material = fluidBlock instanceof BlockMaterialPipe blockMaterialPipe ? blockMaterialPipe.getItemMaterial(stack) : null;

        if (pipeType != null) {
            PipeRenderContext context = new PipeRenderContext(12, 0, pipeType.getThickness());
            context.color = GTUtility.convertRGBtoOpaqueRGBA_CL(getPipeColor(material, IPipeTile.DEFAULT_INSULATION_COLOR));
            buildRenderer(context, fluidBlock,null,pipeType,material);
            renderPipeBlock(renderState, context);
        }

        renderState.draw();
        GlStateManager.disableBlend();
    }


    public void renderPipeBlock(CCRenderState renderState, PipeRenderContext renderContext) {
        Cuboid6 cuboid6 = BlockPipe.getSideBox(null, renderContext.pipeThickness);
        if ((renderContext.connections & 63) == 0) {
            for (EnumFacing renderedSide : EnumFacing.VALUES) {
                renderOpenFace(renderState, renderContext, renderedSide, cuboid6);
            }
        } else {
            for (EnumFacing renderedSide : EnumFacing.VALUES) {
                if ((renderContext.connections & 1 << renderedSide.getIndex()) == 0) {
                    int oppositeIndex = renderedSide.getOpposite().getIndex();
                    if ((renderContext.connections & 1 << oppositeIndex) > 0 &&
                            (renderContext.connections & 63 & ~(1 << oppositeIndex)) == 0) { 
                        renderOpenFace(renderState, renderContext, renderedSide, cuboid6);
                    } else {
                        renderPipeSide(renderState, renderContext, renderedSide, cuboid6);
                    }
                } else {
                    renderPipeCube(renderState, renderContext, renderedSide);
                }
            }
        }
    }


    protected void renderPipeCube(CCRenderState renderState, PipeRenderContext renderContext, EnumFacing side) {
        Cuboid6 cuboid = BlockPipe.getSideBox(side, renderContext.pipeThickness);
        boolean doRenderBlockedOverlay = (renderContext.blockedConnections & (1 << side.getIndex())) > 0;
        for (EnumFacing renderedSide : EnumFacing.VALUES) {
            if (renderedSide.getAxis() != side.getAxis()) {
                renderPipeSide(renderState, renderContext, renderedSide, cuboid);
                if (doRenderBlockedOverlay) {
                    // render blocked connections
                    renderFace(renderState, renderContext.blockedOverlay, renderedSide, cuboid);
                }
            }
        }
        if ((renderContext.connections & 1 << (6 + side.getIndex())) > 0) {
            renderPipeSide(renderState, renderContext, side, cuboid);
        } else {
            if ((renderContext.connections & 1 << (12 + side.getIndex())) > 0) {
                cuboid = BlockPipe.getCoverSideBox(side, renderContext.pipeThickness);
            }
            renderOpenFace(renderState, renderContext, side, cuboid);
        }
    }

    protected void renderOpenFace(CCRenderState renderState, PipeRenderContext renderContext, EnumFacing side,
                                  Cuboid6 cuboid6) {
        for (IVertexOperation[] vertexOperations : renderContext.openFaceRenderer) {
            renderFace(renderState, vertexOperations, side, cuboid6);
        }
    }

    protected void renderPipeSide(CCRenderState renderState, PipeRenderContext renderContext, EnumFacing side,
                                  Cuboid6 cuboid6) {
        for (IVertexOperation[] vertexOperations : renderContext.pipeSideRenderer) {
            renderFace(renderState, vertexOperations, side, cuboid6);
        }

        int blockedConnections = renderContext.getBlockedConnections();
        int connections = renderContext.getConnections();
        if (blockedConnections != 0) {
            int borderMask = 0;
            for (Border border : Border.VALUES) {
                EnumFacing borderSide = getSideAtBorder(side, border);
                if (TileEntityPipeBase.isSideBlocked(blockedConnections, borderSide) &&
                        TileEntityPipeBase.isConnected(connections, borderSide)) {
                    // only render when the side is blocked *and* connected
                    borderMask |= border.mask;
                }
            }
            if (borderMask != 0) {
                IVertexOperation[] pipeline = ArrayUtils.addAll(renderContext.getBaseVertexOperation(),
                        RESTRICTOR_MAP.get(borderMask));
                renderFace(renderState, pipeline, side, cuboid6);
            }
        }

    }

    protected void renderFace(CCRenderState renderState, IVertexOperation[] pipeline, EnumFacing side, Cuboid6 cuboid6) {
        BlockRenderer.BlockFace blockFace = blockFaces.get();
        blockFace.loadCuboidFace(cuboid6, side.getIndex());
        renderState.setPipeline(blockFace, 0, blockFace.verts.length, pipeline);
        renderState.render();
    }


    protected void renderOtherLayers(BlockRenderLayer layer, CCRenderState renderState,
                                     PipeRenderContext renderContext) {}

    @Override
    public IModelState getTransforms() {
        return TransformUtils.DEFAULT_BLOCK;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    protected boolean canRenderInLayer(BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    private static int getPipeColor(Material material, int paintingColor) {
        if (paintingColor == IPipeTile.DEFAULT_INSULATION_COLOR) {
            return material == null ? 0xFFFFFF :  material.materialRGB;
        }
        return paintingColor;
    }


    public abstract TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material);

    public Pair<TextureAtlasSprite, Integer> getParticleTexture(IPipeTile<?, ?> pipeTile) {
        TextureAtlasSprite missingTexture = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();

        if (pipeTile == null) {
            return Pair.of(missingTexture, 0xFFFFFF);
        }
        IPipeType<?> pipeType = pipeTile.getPipeType();
        Material material = pipeTile instanceof TileEntityMaterialPipeBase ?
                ((TileEntityMaterialPipeBase<?, ?>) pipeTile).getPipeMaterial() : null;
        if (pipeType == null) {
            return Pair.of(missingTexture, 0xFFFFFF);
        }
        TextureAtlasSprite atlasSprite = getParticleTexture(pipeType, material);
        int pipeColor = getPipeColor(material, pipeTile.getInsulationColor());
        return Pair.of(atlasSprite, pipeColor);
    }


    public static class PipeRenderContext {

        private final BlockPos pos;
        private final LightMatrix lightMatrix;
        protected final List<IVertexOperation[]> openFaceRenderer = new ArrayList<>();
        protected final List<IVertexOperation[]> pipeSideRenderer = new ArrayList<>();
        private final IVertexOperation[] blockedOverlay;
        private final float pipeThickness;
        private int color;
        private final int connections;
        private final int blockedConnections;

        public PipeRenderContext(BlockPos pos, LightMatrix lightMatrix, int connections, int blockedConnections,
                                 float thickness) {
            this.pos = pos;
            this.lightMatrix = lightMatrix;
            this.connections = connections;
            this.blockedConnections = blockedConnections;
            this.pipeThickness = thickness;
            if (pos != null && lightMatrix != null) {
                blockedOverlay = new IVertexOperation[] { new Translation(pos), lightMatrix,
                        new IconTransformation(Textures.PIPE_BLOCKED_OVERLAY) };
            } else {
                blockedOverlay = new IVertexOperation[] { new IconTransformation(Textures.PIPE_BLOCKED_OVERLAY) };
            }
        }

        public PipeRenderContext(int connections, int blockedConnections, float thickness) {
            this(null, null, connections, blockedConnections, thickness);
        }

        public PipeRenderContext addOpenFaceRender(IVertexOperation... vertexOperations) {
            return addOpenFaceRender(true, vertexOperations);
        }

        public PipeRenderContext addOpenFaceRender(boolean applyDefaultColor, IVertexOperation... vertexOperations) {
            IVertexOperation[] baseVertexOperation = getBaseVertexOperation();
            baseVertexOperation = ArrayUtils.addAll(baseVertexOperation, vertexOperations);
            if (applyDefaultColor) {
                baseVertexOperation = ArrayUtils.addAll(baseVertexOperation, getColorOperation());
            }
            openFaceRenderer.add(baseVertexOperation);
            return this;
        }

        public PipeRenderContext addSideRender(IVertexOperation... vertexOperations) {
            return addSideRender(true, vertexOperations);
        }

        public PipeRenderContext addSideRender(boolean applyDefaultColor, IVertexOperation... vertexOperations) {
            IVertexOperation[] baseVertexOperation = getBaseVertexOperation();
            baseVertexOperation = ArrayUtils.addAll(baseVertexOperation, vertexOperations);
            if (applyDefaultColor) {
                baseVertexOperation = ArrayUtils.addAll(baseVertexOperation, getColorOperation());
            }
            pipeSideRenderer.add(baseVertexOperation);
            return this;
        }

        public ColourMultiplier getColorOperation() {
            return new ColourMultiplier(color);
        }

        protected IVertexOperation[] getBaseVertexOperation() {
            if (pos == null) {
                return lightMatrix == null ? new IVertexOperation[0] : new IVertexOperation[] { lightMatrix };
            }
            return lightMatrix == null ? new IVertexOperation[] { new Translation(pos) } :
                    new IVertexOperation[] { new Translation(pos), lightMatrix };
        }

        public int getConnections() {
            return connections;
        }

        public int getBlockedConnections() {
            return blockedConnections;
        }

        public List<IVertexOperation[]> getPipeSideRenderer() {
            return pipeSideRenderer;
        }

        public List<IVertexOperation[]> getOpenFaceRenderer() {
            return openFaceRenderer;
        }

        public float getPipeThickness() {
            return pipeThickness;
        }
    }

    private static void addRestrictor(TextureAtlasSprite sprite, Border... borders) {
        int mask = 0;
        for (Border border : borders) {
            mask |= border.mask;
        }
        RESTRICTOR_MAP.put(mask, new IVertexOperation[] { new IconTransformation(sprite) });
    }

    protected static EnumFacing getSideAtBorder(EnumFacing side, Border border) {
        return FACE_BORDER_MAP.get(side).get(border);
    }

    private static EnumMap<Border, EnumFacing> borderMap(EnumFacing topSide, EnumFacing bottomSide, EnumFacing leftSide,
                                                         EnumFacing rightSide) {
        EnumMap<Border, EnumFacing> sideMap = new EnumMap<>(Border.class);
        sideMap.put(Border.TOP, topSide);
        sideMap.put(Border.BOTTOM, bottomSide);
        sideMap.put(Border.LEFT, leftSide);
        sideMap.put(Border.RIGHT, rightSide);
        return sideMap;
    }


    public enum Border {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT;

        public static final Border[] VALUES = values();

        public final int mask;

        Border() {
            mask = 1 << this.ordinal();
        }
    }

}
