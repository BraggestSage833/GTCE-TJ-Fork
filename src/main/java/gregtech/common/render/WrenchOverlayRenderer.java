package gregtech.common.render;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.vec.Vector3;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.cover.ICoverable.PrimaryBoxData;
import gregtech.api.gui.GuiTextures;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.common.items.behaviors.CoverPlaceBehavior;
import gregtech.common.items.behaviors.CrowbarBehaviour;
import gregtech.common.pipelike.cable.tile.TileEntityCable;
import gregtech.common.pipelike.fluidpipe.tile.TileEntityFluidPipe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class WrenchOverlayRenderer {

    private static float r, g, b;

    @SubscribeEvent
    public static void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        EntityPlayer player = event.getPlayer();
        World world = player.world;
        RayTraceResult target = event.getTarget();

        if (target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return; //magically, draw block highlight is called not only for blocks (see forge issues)
        }

        BlockPos pos = target.getBlockPos();
        IBlockState blockState = world.getBlockState(pos);
        TileEntity tileEntity = world.getTileEntity(pos);
        ItemStack heldItem = player.getHeldItem(EnumHand.MAIN_HAND);

        if (tileEntity != null && shouldDrawOverlayForItem(heldItem, tileEntity)
                && (tileEntity instanceof IPipeTile || useGridForRayTraceResult(target))) {
            EnumFacing facing = target.sideHit;
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.glLineWidth(3.0F);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);

            if (world.getWorldBorder().contains(pos)) {
                double d3 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) event.getPartialTicks();
                double d4 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) event.getPartialTicks();
                double d5 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) event.getPartialTicks();
                AxisAlignedBB box = blockState.getSelectedBoundingBox(world, pos).grow(0.002D).offset(-d3, -d4, -d5);
                RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 0.4F);
                drawOverlayLines(facing, box);
                drawHoverHighlight(facing, box, target.hitVec.subtract(d3, d4, d5));
                if (tileEntity instanceof IPipeTile<?, ?> pipeTile) {
                    drawConnections(pipeTile, facing, box);
                }
            }

            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();

            event.setCanceled(true);
        }
    }

    public static boolean useGridForRayTraceResult(RayTraceResult result) {
        if(result instanceof CuboidRayTraceResult) {
            CuboidRayTraceResult traceResult = (CuboidRayTraceResult) result;
            //use grid only for center hit or for primary box with placement grid enabled
            if(traceResult.cuboid6.data == null) {
                return true; //default is true
            } else if(traceResult.cuboid6.data instanceof PrimaryBoxData) {
                PrimaryBoxData primaryBoxData = (PrimaryBoxData) traceResult.cuboid6.data;
                return primaryBoxData.usePlacementGrid;
            } else return false; //otherwise, do not use grid
        }
        //also use it for default collision blocks
        return true;
    }

    public static boolean shouldDrawOverlayForItem(ItemStack itemStack, TileEntity tileEntity) {
        if(tileEntity instanceof MetaTileEntityHolder &&
            itemStack.hasCapability(GregtechCapabilities.CAPABILITY_WRENCH, null)) {
            return true;
        }
        if (tileEntity instanceof IPipeTile) {
            BlockPipe<?, ?, ?> pipeBlock = ((IPipeTile<?, ?>) tileEntity).getPipeBlock();
            if (pipeBlock != null && itemStack.hasCapability(pipeBlock.getConnectionToggleCapability(), null)) {
                return true;
            }
        }
        if(tileEntity.hasCapability(GregtechTileCapabilities.CAPABILITY_COVERABLE, null)) {
            if(itemStack.hasCapability(GregtechCapabilities.CAPABILITY_SCREWDRIVER, null)) {
                return true;
            }
            if (itemStack.getItem() instanceof MetaItem) {
                MetaItem<?> metaItem = (MetaItem<?>) itemStack.getItem();
                MetaItem<?>.MetaValueItem valueItem = metaItem.getItem(itemStack);
                if (valueItem != null) {
                    List<IItemBehaviour> behaviourList = valueItem.getBehaviours();
                    return behaviourList.stream().anyMatch(it ->
                        it instanceof CoverPlaceBehavior || it instanceof CrowbarBehaviour);
                }
            }
        }
        return false;
    }

    private static void drawOverlayLines(EnumFacing facing, AxisAlignedBB box) {
        r = g = 0.2F + (float) Math.sin((System.currentTimeMillis() % (Math.PI * 800)) / 800) / 2;
        b = 1f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);

        Vector3 topRight = new Vector3(box.maxX, box.maxY, box.maxZ);
        Vector3 bottomRight = new Vector3(box.maxX, box.minY, box.maxZ);
        Vector3 bottomLeft = new Vector3(box.minX, box.minY, box.maxZ);
        Vector3 topLeft = new Vector3(box.minX, box.maxY, box.maxZ);
        Vector3 shift = new Vector3(0.25, 0, 0);
        Vector3 shiftVert = new Vector3(0, 0.25, 0);

        Vector3 cubeCenter = new Vector3(box.getCenter());


        topRight.subtract(cubeCenter);
        bottomRight.subtract(cubeCenter);
        bottomLeft.subtract(cubeCenter);
        topLeft.subtract(cubeCenter);

        switch (facing) {
            case WEST: {
                topRight.rotate(Math.PI / 2, Vector3.down);
                bottomRight.rotate(Math.PI / 2, Vector3.down);
                bottomLeft.rotate(Math.PI / 2, Vector3.down);
                topLeft.rotate(Math.PI / 2, Vector3.down);
                shift.rotate(Math.PI / 2, Vector3.down);
                shiftVert.rotate(Math.PI / 2, Vector3.down);
                break;
            }
            case EAST: {
                topRight.rotate(-Math.PI / 2, Vector3.down);
                bottomRight.rotate(-Math.PI / 2, Vector3.down);
                bottomLeft.rotate(-Math.PI / 2, Vector3.down);
                topLeft.rotate(-Math.PI / 2, Vector3.down);
                shift.rotate(-Math.PI / 2, Vector3.down);
                shiftVert.rotate(-Math.PI / 2, Vector3.down);
                break;
            }
            case NORTH: {
                topRight.rotate(Math.PI, Vector3.down);
                bottomRight.rotate(Math.PI, Vector3.down);
                bottomLeft.rotate(Math.PI, Vector3.down);
                topLeft.rotate(Math.PI, Vector3.down);
                shift.rotate(Math.PI, Vector3.down);
                shiftVert.rotate(Math.PI, Vector3.down);
                break;
            }
            case UP: {
                Vector3 side = new Vector3(1, 0, 0);
                topRight.rotate(-Math.PI / 2, side);
                bottomRight.rotate(-Math.PI / 2, side);
                bottomLeft.rotate(-Math.PI / 2, side);
                topLeft.rotate(-Math.PI / 2, side);
                shift.rotate(-Math.PI / 2, side);
                shiftVert.rotate(-Math.PI / 2, side);
                break;
            }
            case DOWN: {
                Vector3 side = new Vector3(1, 0, 0);
                topRight.rotate(Math.PI / 2, side);
                bottomRight.rotate(Math.PI / 2, side);
                bottomLeft.rotate(Math.PI / 2, side);
                topLeft.rotate(Math.PI / 2, side);
                shift.rotate(Math.PI / 2, side);
                shiftVert.rotate(Math.PI / 2, side);
                break;
            }
        }

        topRight.add(cubeCenter);
        bottomRight.add(cubeCenter);
        bottomLeft.add(cubeCenter);
        topLeft.add(cubeCenter);

        // straight top bottom lines
        startLine(buffer, topRight.copy().add(shift.copy().negate()));
        endLine(buffer, bottomRight.copy().add(shift.copy().negate()));

        startLine(buffer, bottomLeft.copy().add(shift));
        endLine(buffer, topLeft.copy().add(shift));

        // straight side to side lines
        startLine(buffer, topLeft.copy().add(shiftVert.copy().negate()));
        endLine(buffer, topRight.copy().add(shiftVert.copy().negate()));

        startLine(buffer, bottomLeft.copy().add(shiftVert));
        endLine(buffer, bottomRight.copy().add(shiftVert));

        tessellator.draw();
    }

    private static void drawConnections(IPipeTile<?, ?> pipeTile, EnumFacing facing, AxisAlignedBB box) {
        Vector3 right = unitOnFace(1, 0, 0, facing);
        Vector3 up = unitOnFace(0, 1, 0, facing);
        Vector3 normal = unitOnFace(0, 0, 1, facing);

        EnumFacing rightSide = EnumFacing.getFacingFromVector((float) right.x, (float) right.y, (float) right.z);
        EnumFacing upSide = EnumFacing.getFacingFromVector((float) up.x, (float) up.y, (float) up.z);

        double halfNormal = 0.5 * (Math.abs(normal.x) * (box.maxX - box.minX)
                + Math.abs(normal.y) * (box.maxY - box.minY)
                + Math.abs(normal.z) * (box.maxZ - box.minZ));
        Vector3 faceCenter = new Vector3(box.getCenter()).add(normal.copy().multiply(halfNormal));

        boolean backConnected = pipeTile.isConnected(facing.getOpposite());

        GlStateManager.enableTexture2D();   // the handler disabled it for the lines
        GlStateManager.color(1F, 1F, 1F, 1F);

        ResourceLocation texture = getConnectionTexture(pipeTile);
        if (texture == null) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        // center = front
        addIcon(buffer, faceCenter, right, up, 0, 0, 0.15, pipeTile.isConnected(facing));

        // edges
        addIcon(buffer, faceCenter, right, up, 0.375, 0, 0.07, pipeTile.isConnected(rightSide));
        addIcon(buffer, faceCenter, right, up, -0.375, 0, 0.07, pipeTile.isConnected(rightSide.getOpposite()));
        addIcon(buffer, faceCenter, right, up, 0, 0.375, 0.07, pipeTile.isConnected(upSide));
        addIcon(buffer, faceCenter, right, up, 0, -0.375, 0.07, pipeTile.isConnected(upSide.getOpposite()));

        // corners = back (all four toggle the same connection)
        addIcon(buffer, faceCenter, right, up, 0.375, 0.375, 0.07, backConnected);
        addIcon(buffer, faceCenter, right, up, -0.375, 0.375, 0.07, backConnected);
        addIcon(buffer, faceCenter, right, up, 0.375, -0.375, 0.07, backConnected);
        addIcon(buffer, faceCenter, right, up, -0.375, -0.375, 0.07, backConnected);

        tessellator.draw();   // one draw call for all 9 icons
        GlStateManager.disableTexture2D();
    }

    private static Vector3 unitOnFace(double x, double y, double z, EnumFacing facing) {
        Vector3 vec = new Vector3(x, y, z);
        switch (facing) {
            case WEST:
                vec.rotate(Math.PI / 2, Vector3.down);
                break;
            case EAST:
                vec.rotate(-Math.PI / 2, Vector3.down);
                break;
            case NORTH:
                vec.rotate(Math.PI, Vector3.down);
                break;
            case UP:
                vec.rotate(-Math.PI / 2, new Vector3(1, 0, 0));
                break;
            case DOWN:
                vec.rotate(Math.PI / 2, new Vector3(1, 0, 0));
                break;
            default:
                break;
        }
        return vec;
    }

    private static void addIcon(BufferBuilder buffer, Vector3 faceCenter, Vector3 right, Vector3 up,
                                double offsetRight, double offsetUp, double half, boolean connected) {
        Vector3 c = faceCenter.copy()
                .add(right.copy().multiply(offsetRight))
                .add(up.copy().multiply(offsetUp));
        Vector3 r = right.copy().multiply(half);
        Vector3 u = up.copy().multiply(half);

        double u0 = connected ? 0.5 : 0.0;
        double u1 = u0 + 0.5;

        Vector3 a = c.copy().subtract(r).subtract(u);
        Vector3 b = c.copy().add(r).subtract(u);
        Vector3 d = c.copy().add(r).add(u);
        Vector3 e = c.copy().subtract(r).add(u);

        buffer.pos(a.x, a.y, a.z).tex(u0, 1).color(1F, 1F, 1F, 1F).endVertex();
        buffer.pos(b.x, b.y, b.z).tex(u1, 1).color(1F, 1F, 1F, 1F).endVertex();
        buffer.pos(d.x, d.y, d.z).tex(u1, 0).color(1F, 1F, 1F, 1F).endVertex();
        buffer.pos(e.x, e.y, e.z).tex(u0, 0).color(1F, 1F, 1F, 1F).endVertex();
    }

    private static ResourceLocation getConnectionTexture(IPipeTile<?, ?> pipeTile) {
        if (pipeTile instanceof TileEntityCable) {
            return GuiTextures.TOOL_WIRE_CONNECTIONS.imageLocation;
        }
        if (pipeTile instanceof TileEntityFluidPipe) {
            return GuiTextures.TOOL_PIPE_CONNECTIONS.imageLocation;
        }
        return null;
    }

    private static final double GRID_INSET = 0.25;       // same value the grid lines use
    private static final float HIGHLIGHT_ALPHA = 0.35F;

    private static void drawHoverHighlight(EnumFacing facing, AxisAlignedBB box, Vec3d hit) {
        Vector3 right = unitOnFace(1, 0, 0, facing);
        Vector3 up = unitOnFace(0, 1, 0, facing);
        Vector3 normal = unitOnFace(0, 0, 1, facing);

        double hw = halfExtent(right, box);
        double hh = halfExtent(up, box);
        double hn = halfExtent(normal, box);
        Vector3 c = new Vector3(box.getCenter()).add(normal.copy().multiply(hn));   // face center

        // hit point in face-local coordinates
        double dx = (hit.x - c.x) * right.x + (hit.y - c.y) * right.y + (hit.z - c.z) * right.z;
        double dy = (hit.x - c.x) * up.x    + (hit.y - c.y) * up.y    + (hit.z - c.z) * up.z;

        // the grid lines sit at +-(half size - inset), same as in drawOverlayLines
        double tx = hw - GRID_INSET;
        double ty = hh - GRID_INSET;

        int col = dx < -tx ? -1 : dx > tx ? 1 : 0;
        int row = dy < -ty ? -1 : dy > ty ? 1 : 0;

        double x0 = col == -1 ? -hw : col == 0 ? -tx : tx;
        double x1 = col == -1 ? -tx : col == 0 ?  tx : hw;
        double y0 = row == -1 ? -hh : row == 0 ? -ty : ty;
        double y1 = row == -1 ? -ty : row == 0 ?  ty : hh;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        highlightVertex(buffer, c, right, up, x0, y0);
        highlightVertex(buffer, c, right, up, x1, y0);
        highlightVertex(buffer, c, right, up, x1, y1);
        highlightVertex(buffer, c, right, up, x0, y1);
        tessellator.draw();
    }

    private static void highlightVertex(BufferBuilder buffer, Vector3 c, Vector3 right, Vector3 up,
                                        double p, double q) {
        buffer.pos(c.x + right.x * p + up.x * q,
                        c.y + right.y * p + up.y * q,
                        c.z + right.z * p + up.z * q)
                .color(r, g, b, HIGHLIGHT_ALPHA).endVertex();
    }

    private static double halfExtent(Vector3 axis, AxisAlignedBB box) {
        return 0.5 * (Math.abs(axis.x) * (box.maxX - box.minX)
                + Math.abs(axis.y) * (box.maxY - box.minY)
                + Math.abs(axis.z) * (box.maxZ - box.minZ));
    }

    private static void startLine(BufferBuilder buffer, Vector3 vec) {
        buffer.pos(vec.x, vec.y, vec.z).color(r, g, b, 0.0F).endVertex();
    }

    private static void endLine(BufferBuilder buffer, Vector3 vec) {
        buffer.pos(vec.x, vec.y, vec.z).color(r, g, b, 0.5F).endVertex();
    }

}
