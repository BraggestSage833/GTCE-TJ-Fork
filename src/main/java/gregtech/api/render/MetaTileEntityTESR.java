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

        if (te.isDynamicRender()) {
            ((IRenderMetaTileEntity) metaTileEntity).renderMetaTileEntityDynamic(x,y,z,partialTicks);
        }

        SideRendererCache cache = metaTileEntity.getSideRendererCache();

        if (cache.hasAnyDynamic()) {
            for (EnumFacing side : EnumFacing.VALUES) {
                if (!cache.isDynamic(side)) {
                    continue;
                }
                CoverBehavior cover = metaTileEntity.getCoverAtSide(side);
                if (cover != null) {
                    ((IRenderMetaTileEntity) cover).renderMetaTileEntityDynamic(x, y, z, partialTicks);
                }
            }
        }

        if (te.isFastRenderer() || cache.hasAnyFast()) {
            renderFastImmediate(te, x, y, z, partialTicks);
        }
    }


    @Override
    public void renderTileEntityFast(MetaTileEntityHolder te, double x, double y, double z, float partialTicks, int destroyStage, float partial, BufferBuilder buffer) {
        renderFastParts(te, x, y, z, partialTicks, buffer);
    }

    private void renderFastParts(MetaTileEntityHolder te, double x, double y, double z, float partialTicks, BufferBuilder buffer) {
        MetaTileEntity mte = te.getMetaTileEntity();
        if (mte == null) {
            return;
        }

        boolean machineFast = te.isFastRenderer();
        SideRendererCache cache = mte.getSideRendererCache();
        if (!machineFast && !cache.hasAnyFast())  {
            return;
        }

        CCRenderState renderState = CCRenderState.instance();
        Matrix4 translation = new Matrix4().translate(x, y, z);

        if (machineFast) {
            renderState.reset();
            renderState.bind(buffer);
            renderState.setBrightness(te.getWorld(), te.getPos());
            ((IFastRenderMetaTileEntity) mte).renderMetaTileEntityFast(renderState, translation, partialTicks);
        }
        if (!cache.hasAnyFast()) {
            return;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            if (!cache.isFast(side)) {
                continue;
            }

            CoverBehavior cover = mte.getCoverAtSide(side);
            if (cover == null) {
                continue;
            }

            renderState.reset();
            renderState.bind(buffer);
            renderState.setBrightness(te.getWorld(), te.getPos().offset(side));
            ((IFastRenderMetaTileEntity) cover).renderMetaTileEntityFast(renderState, translation, partialTicks);
        }
    }

    private void renderFastImmediate(MetaTileEntityHolder te, double x, double y, double z, float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        renderFastParts(te, x, y, z, partialTicks, buffer);
        buffer.setTranslation(0, 0, 0);
        tessellator.draw();
        GlStateManager.enableCull();
        RenderHelper.enableStandardItemLighting();
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
