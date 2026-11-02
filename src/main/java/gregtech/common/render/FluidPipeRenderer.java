package gregtech.common.render;


import codechicken.lib.vec.uv.IconTransformation;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.type.Material;
import gregtech.api.util.GTUtility;
import gregtech.common.pipelike.fluidpipe.FluidPipeType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;


import javax.annotation.Nullable;

public class FluidPipeRenderer extends PipeRenderer {

    public static final FluidPipeRenderer INSTANCE = new FluidPipeRenderer();

    private FluidPipeRenderer() {
        super("gt_fluid_pipe",GTUtility.gregtechID("fluid_pipe"));
    }


    @Override
    public void buildRenderer(PipeRenderContext context, BlockPipe<?,?,?> blockPipe, IPipeTile<?,?> pipeTile,
                              IPipeType<?> pipeType, @Nullable Material material) {
        if (material == null || !(pipeType instanceof FluidPipeType)) {
            return;
        }

        FluidItemPipeTextureHolder.PipeTextureInfo info = FluidItemPipeTextureHolder.getTexture(((FluidPipeType) pipeType).name);
        context.addOpenFaceRender(new IconTransformation(info.inTexture))
                .addSideRender(new IconTransformation(info.sideTexture));
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @org.jetbrains.annotations.Nullable Material material) {
        return FluidItemPipeTextureHolder.getTexture(((FluidPipeType) pipeType).name).sideTexture;
    }

}
