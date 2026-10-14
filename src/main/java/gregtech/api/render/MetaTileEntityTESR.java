package gregtech.api.render;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Matrix4;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.metatileentity.IFastRenderMetaTileEntity;
import gregtech.api.metatileentity.IRenderMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class MetaTileEntityTESR extends TileEntitySpecialRenderer<MetaTileEntityHolder> {

    @Override
    public void render(MetaTileEntityHolder te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        MetaTileEntity metaTileEntity = te.getMetaTileEntity();

        if (metaTileEntity == null) {
            return;
        }

        if (!te.isDynamicRender()) {
            ((IRenderMetaTileEntity) metaTileEntity).renderMetaTileEntityDynamic(x,y,z,partialTicks);
        }

        SideRendererCache cache = metaTileEntity.getSideRendererCache();

        if (!cache.hasAnyRenderer()) {
            return;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            if (cache.isDynamic(side)) {
                ((IRenderMetaTileEntity) metaTileEntity.getCoverAtSide(side)).renderMetaTileEntityDynamic(x, y, z, partialTicks);
            }
        }
    }


    @Override
    public void renderTileEntityFast(MetaTileEntityHolder te, double x, double y, double z, float partialTicks, int destroyStage, float partial, BufferBuilder buffer) {
        MetaTileEntity metaTileEntity = te.getMetaTileEntity();

        if (metaTileEntity == null) {
            return;
        }

        CCRenderState renderState = CCRenderState.instance();

        renderState.reset();
        renderState.bind(buffer);
        renderState.setBrightness(te.getWorld(),te.getPos());

        Matrix4 translation = new Matrix4().translate(x,y,z);

        if (te.isFastRenderer()) {
            ((IFastRenderMetaTileEntity) metaTileEntity).renderMetaTileEntityFast(renderState,translation,partialTicks);
        }

        SideRendererCache cache = metaTileEntity.getSideRendererCache();

        if (!cache.hasAnyRenderer()) {
            return;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            if (cache.isFast(side)) {
                renderState.reset();
                renderState.bind(buffer);
                renderState.setBrightness(te.getWorld(), te.getPos().offset(side));
                CoverBehavior cover = metaTileEntity.getCoverAtSide(side);
                ((IFastRenderMetaTileEntity) cover).renderMetaTileEntityFast(renderState,translation,partialTicks);
            }
        }
    }

    @Override
    public boolean isGlobalRenderer(MetaTileEntityHolder te) {
        MetaTileEntity metaTileEntity = te.getMetaTileEntity();

        if (metaTileEntity == null) {
            return false;
        }

        if (te.isDynamicRender() && ((IRenderMetaTileEntity) metaTileEntity).isGlobalRenderer()) {
            return true;
        }

        if (te.isFastRenderer() && ((IFastRenderMetaTileEntity) metaTileEntity).isGlobalRenderer()) {
            return true;
        }

        SideRendererCache cache = metaTileEntity.getSideRendererCache();

        if (!cache.hasAnyRenderer()) {
            return false;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            SideRendererCache.RendererType type = cache.get(side);

            if (type == SideRendererCache.RendererType.DYNAMIC) {
                if (((IRenderMetaTileEntity) metaTileEntity.getCoverAtSide(side)).isGlobalRenderer()) {
                    return true;
                }
            } else if (type == SideRendererCache.RendererType.FAST) {
                if (((IFastRenderMetaTileEntity) metaTileEntity.getCoverAtSide(side)).isGlobalRenderer()) {
                    return true;
                }
            }
        }

        return false;
    }
}
