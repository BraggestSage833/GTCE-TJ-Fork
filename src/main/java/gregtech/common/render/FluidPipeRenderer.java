package gregtech.common.render;


import codechicken.lib.vec.uv.IconTransformation;
import gregtech.api.GTValues;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.type.Material;
import gregtech.api.util.GTUtility;
import gregtech.common.pipelike.fluidpipe.FluidPipeType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class FluidPipeRenderer extends PipeRenderer {

    public static final FluidPipeRenderer INSTANCE = new FluidPipeRenderer();
    private final Map<FluidPipeType, PipeTextureInfo> pipeTextures = new HashMap<>();


    private static class PipeTextureInfo {
        public final TextureAtlasSprite inTexture;
        public final TextureAtlasSprite sideTexture;

        public PipeTextureInfo(TextureAtlasSprite inTexture, TextureAtlasSprite sideTexture) {
            this.inTexture = inTexture;
            this.sideTexture = sideTexture;
        }
    }

    private FluidPipeRenderer() {
        super("gt_fluid_pipe",GTUtility.gregtechID("fluid_pipe"));
    }

    @Override
    public void registerIcon(TextureMap map) {
        for (FluidPipeType fluidPipeType : FluidPipeType.values()) {
            ResourceLocation inLocation = new ResourceLocation(GTValues.MODID, String.format("blocks/pipe/pipe_%s_in", fluidPipeType.name));
            ResourceLocation sideLocation = new ResourceLocation(GTValues.MODID, String.format("blocks/pipe/pipe_%s_side", fluidPipeType.name));

            TextureAtlasSprite inTexture = map.registerSprite(inLocation);
            TextureAtlasSprite sideTexture = map.registerSprite(sideLocation);
            this.pipeTextures.put(fluidPipeType, new PipeTextureInfo(inTexture, sideTexture));
        }
    }

    @Override
    public void buildRenderer(PipeRenderContext context, BlockPipe<?,?,?> blockPipe, IPipeTile<?,?> pipeTile,
                              IPipeType<?> pipeType, @Nullable Material material) {
        if (material == null || !(pipeType instanceof FluidPipeType)) {
            return;
        }

        context.addOpenFaceRender(new IconTransformation(pipeTextures.get(pipeType).inTexture))
                .addSideRender(new IconTransformation(pipeTextures.get(pipeType).sideTexture));
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @org.jetbrains.annotations.Nullable Material material) {
        return pipeTextures.get((FluidPipeType) pipeType).sideTexture;
    }

}
