package gregtech.common.render;

import codechicken.lib.vec.uv.IconTransformation;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.type.Material;
import gregtech.api.util.GTUtility;
import gregtech.common.pipelike.item.ItemPipeType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.jetbrains.annotations.Nullable;


public class ItemPipeRenderer extends PipeRenderer{
    public static final ItemPipeRenderer INSTANCE = new ItemPipeRenderer();


    private ItemPipeRenderer() {
        super("gt_item_pipe", GTUtility.gregtechID("item_pipe"));
    }


    @Override
    public void buildRenderer(PipeRenderContext context, BlockPipe<?, ?, ?> blockPipe, IPipeTile<?, ?> pipeTile,
                              IPipeType<?> pipeType, @Nullable Material material) {
        if (material == null || !(pipeType instanceof ItemPipeType)) {
            return;
        }

        FluidItemPipeTextureHolder.PipeTextureInfo info = FluidItemPipeTextureHolder.getTexture(((ItemPipeType) pipeType).name);
        context.addOpenFaceRender(new IconTransformation(info.inTexture))
                .addSideRender(new IconTransformation(info.sideTexture));

    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material) {
        return FluidItemPipeTextureHolder.getTexture(((ItemPipeType) pipeType).name).sideTexture;
    }

}
