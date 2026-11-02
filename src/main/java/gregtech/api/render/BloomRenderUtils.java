package gregtech.api.render;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import github.kasuminova.lumenized.common.config.LumenizedConfig;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.client.utils.BloomEffectUtil;
import gregtech.common.render.CubeRendererState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Random;

public class BloomRenderUtils {
    public static boolean renderBlock(IBlockAccess world, BlockPos pos, IBlockState state, BufferBuilder buffer) {
        MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(world, pos);
        if (metaTileEntity == null) {
            return false;
        }

        CCRenderState renderState = CCRenderState.instance();
        renderState.reset();
        renderState.bind(buffer);
        Matrix4 translation = new Matrix4().translate(pos.getX(), pos.getY(), pos.getZ());
        BlockRenderLayer renderLayer = MinecraftForgeClient.getRenderLayer();
        boolean bloomPass = renderLayer != null && renderLayer == BloomEffectUtil.getBloomLayer();
        int startVertices = buffer.getVertexCount();

        Textures.RENDER_STATE.set(new CubeRendererState(renderLayer, CubeRendererState.PASS_MASK, world));

        if (bloomPass || metaTileEntity.canRenderInLayer(renderLayer)) {
            renderState.lightMatrix.locate(world, pos);
            IVertexOperation[] pipeline = new IVertexOperation[]{renderState.lightMatrix};
            metaTileEntity.renderMetaTileEntity(renderState, translation.copy(), pipeline);
        }

        Matrix4 coverTranslation = translation.copy();
        metaTileEntity.renderCovers(renderState, coverTranslation, renderLayer);

        if (metaTileEntity.isFragile() && renderLayer == BlockRenderLayer.CUTOUT) {
            TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
            Random posRand = new Random(MathHelper.getPositionRandom(pos));
            int destroyStage = posRand.nextInt(10);
            TextureAtlasSprite atlasSprite = textureMap.getAtlasSprite("minecraft:blocks/destroy_stage_" + destroyStage);

            for (EnumFacing face : EnumFacing.VALUES) {
                Textures.renderFace(renderState, translation, new IVertexOperation[0], face, Cuboid6.full, atlasSprite);
            }
        }
        Textures.RENDER_STATE.remove();

        return !bloomPass || startVertices > 0;
    }

    public static void draw(IVertexOperation[] pipeline, CCRenderState renderState, Matrix4 translation, EnumFacing side, TextureAtlasSprite sprite) {
        if (!LumenizedConfig.emissiveTexturesBloom) {
            return;
        }

        IVertexOperation[] lightPipeline = ArrayUtils.add(pipeline, new LightMapOperation(240, 240));
        Textures.renderFace(renderState, translation, lightPipeline, side, Cuboid6.full, sprite,
                BloomEffectUtil.getEffectiveBloomLayer());
    }

    public static boolean isBloomPass(CubeRendererState s) {
        return s != null && s.layer != null && s.layer == BloomEffectUtil.getBloomLayer();
    }

}
