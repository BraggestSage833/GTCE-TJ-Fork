package gregtech.api.render;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.texture.TextureUtils.IIconRegister;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.GTValues;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class OrientedOverlayRenderer implements IIconRegister {

    private final static String EMISSIVE = "_emissive";
    public enum OverlayFace {
        FRONT, BACK, TOP, BOTTOM, SIDE;

        public static OverlayFace bySide(EnumFacing side, EnumFacing frontFacing) {
            if (side == frontFacing) {
                return FRONT;
            } else if (side.getOpposite() == frontFacing) {
                return BACK;
            } else if (side == EnumFacing.UP) {
                return TOP;
            } else if (side == EnumFacing.DOWN) {
                return BOTTOM;
            } else return SIDE;
        }
    }

    private final String basePath;
    private OverlayFace[] faces;

    @SideOnly(Side.CLIENT)
    private Map<OverlayFace, ActivePredicate> sprites;

    @SideOnly(Side.CLIENT)
    private static class ActivePredicate {


        private final TextureAtlasSprite normalSprite;
        private final TextureAtlasSprite activeSprite;
        private final TextureAtlasSprite normalEmissiveSprite;
        private final TextureAtlasSprite activeEmissiveSprite;

        public ActivePredicate(TextureAtlasSprite normalSprite, TextureAtlasSprite activeSprite,
                               TextureAtlasSprite normalEmissiveSprite, TextureAtlasSprite activeEmissiveSprite) {
            this.normalSprite = normalSprite;
            this.activeSprite = activeSprite;
            this.normalEmissiveSprite = normalEmissiveSprite;
            this.activeEmissiveSprite = activeEmissiveSprite;
        }

        public TextureAtlasSprite getSprite(boolean active) {
            return active ? activeSprite : normalSprite;
        }

        public TextureAtlasSprite getEmissiveSprite(boolean active) {
            return active ? activeEmissiveSprite : normalEmissiveSprite;
        }
    }

    public OrientedOverlayRenderer(String basePath, OverlayFace... faces) {
        this.basePath = basePath;
        this.faces = faces;
        Textures.iconRegisters.add(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(TextureMap textureMap) {
        this.sprites = new HashMap<>();
        boolean bloom = MetaTileEntityRenderer.useBloom();

        for (OverlayFace overlayFace : faces) {
            String faceName = overlayFace.name().toLowerCase();

            String normalPath = String.format("blocks/%s/overlay_%s", basePath, faceName);
            String activePath = String.format("blocks/%s/overlay_%s_active", basePath, faceName);

            TextureAtlasSprite normalSprite = textureMap.registerSprite(new ResourceLocation(GTValues.MODID, normalPath));
            TextureAtlasSprite activeSprite = textureMap.registerSprite(new ResourceLocation(GTValues.MODID, activePath));

            TextureAtlasSprite normalEmissive = null;
            TextureAtlasSprite activeEmissive = null;

            if (bloom) {
                String normalEmissivePath = normalPath + EMISSIVE;
                String activeEmissivePath = activePath + EMISSIVE;

                if (ResourceHelper.doResourcepacksHaveTexture(GTValues.MODID, normalEmissivePath, true)) {
                    normalEmissive = textureMap.registerSprite(new ResourceLocation(GTValues.MODID, normalEmissivePath));
                }

                if (ResourceHelper.doResourcepacksHaveTexture(GTValues.MODID, activeEmissivePath, true)) {
                    activeEmissive = textureMap.registerSprite(new ResourceLocation(GTValues.MODID, activeEmissivePath));
                }
            }

            sprites.put(overlayFace, new ActivePredicate(normalSprite, activeSprite, normalEmissive, activeEmissive));
        }
    }



    @SideOnly(Side.CLIENT)
    public void render(CCRenderState renderState, Matrix4 translation, IVertexOperation[] ops, Cuboid6 bounds, EnumFacing frontFacing, boolean isActive) {
        boolean bloom = MetaTileEntityRenderer.useBloom();

        for (EnumFacing renderSide : EnumFacing.VALUES) {
            OverlayFace overlayFace = OverlayFace.bySide(renderSide, frontFacing);
            ActivePredicate predicate = sprites.get(overlayFace);
            if (predicate == null) {
                continue;
            }

            Textures.renderFace(renderState, translation, ops, renderSide, bounds, predicate.getSprite(isActive));

            TextureAtlasSprite emissiveSprite = bloom ? predicate.getEmissiveSprite(isActive) : null;

            if (emissiveSprite != null) {
                BloomRenderUtils.draw(ops, renderState, translation, renderSide, emissiveSprite);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void render(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline, EnumFacing frontFacing, boolean isActive) {
        render(renderState, translation, pipeline, Cuboid6.full, frontFacing, isActive);
    }
}
