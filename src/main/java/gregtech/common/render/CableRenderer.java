package gregtech.common.render;

import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.uv.IconTransformation;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.type.Material;
import gregtech.api.util.GTUtility;
import gregtech.common.pipelike.cable.Insulation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class CableRenderer extends PipeRenderer {
    public static final CableRenderer INSTANCE = new CableRenderer();
    private final TextureAtlasSprite[] insulationTextures = new TextureAtlasSprite[6];
    private TextureAtlasSprite wireTexture;


    private CableRenderer() {
        super("gt_cable", GTUtility.gregtechID("cable"));
    }

    @Override
    public void registerIcon(TextureMap texMap) {
        ResourceLocation wireLocation = GTUtility.gregtechID("blocks/cable/wire");
        this.wireTexture = texMap.registerSprite(wireLocation);

        for (int i = 0; i< insulationTextures.length; i++) {
            ResourceLocation location = GTUtility.gregtechID("blocks/cable/insulation_" + i);
            this.insulationTextures[i] = texMap.registerSprite(location);
        }
    }

    @Override
    public void buildRenderer(PipeRenderContext context, BlockPipe<?, ?, ?> block, @Nullable IPipeTile<?, ?> tile, IPipeType<?> type, @Nullable Material material) {
        if (material == null || !(type instanceof Insulation)) {
            return;
        }

        int insulationLevel = ((Insulation) type).insulationLevel;
        IVertexOperation wireRender = new IconTransformation(wireTexture);
        ColourMultiplier wireColor = new ColourMultiplier(
                GTUtility.convertRGBtoOpaqueRGBA_CL(material.materialRGB));
        ColourMultiplier insulationColor = new ColourMultiplier(GTUtility.convertRGBtoOpaqueRGBA_CL(0x404040));
        if (tile != null) {
            insulationColor.colour = GTUtility.convertRGBtoOpaqueRGBA_CL(tile.getInsulationColor());
        }


        if (insulationLevel != -1) {
            if ((context.getConnections() & 63) == 0) {
                context.addOpenFaceRender(false, new IconTransformation(insulationTextures[5]), insulationColor);
                return;
            }

            context.addOpenFaceRender(false, wireRender, wireColor)
                    .addOpenFaceRender(false, new IconTransformation(insulationTextures[insulationLevel]),
                            insulationColor)
                    .addSideRender(false, new IconTransformation(insulationTextures[5]), insulationColor);
        } else {
            context.addOpenFaceRender(false, wireRender, wireColor)
                    .addSideRender(false, wireRender, wireColor);
        }
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material) {
        return null;
    }

    @Override
    public Pair<TextureAtlasSprite, Integer> getParticleTexture(IPipeTile<?, ?> pipeTile) {
        TextureAtlasSprite missingTexture = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();

        if (pipeTile == null) {
            return Pair.of(missingTexture, 0xFFFFFF);
        }
        IPipeType<?> pipeType = pipeTile.getPipeType();
        if (!(pipeType instanceof Insulation)) {
            return Pair.of(missingTexture, 0xFFFFFF);
        }
        Material material = pipeTile instanceof TileEntityMaterialPipeBase ?
                ((TileEntityMaterialPipeBase<?, ?>) pipeTile).getPipeMaterial() : null;

        TextureAtlasSprite atlasSprite;
        int particleColor;
        int insulationLevel = ((Insulation) pipeType).insulationLevel;
        if (insulationLevel == -1) {
            atlasSprite = wireTexture;
            particleColor = material == null ? 0xFFFFFF : material.materialRGB;
        } else {
            atlasSprite = insulationTextures[5];
            particleColor = pipeTile.getInsulationColor();
        }
        return Pair.of(atlasSprite, particleColor);
    }

}
