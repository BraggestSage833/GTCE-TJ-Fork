package gregtech.common.render;

import gregtech.api.GTValues;
import gregtech.common.pipelike.fluidpipe.FluidPipeType;
import gregtech.common.pipelike.item.ItemPipeType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class FluidItemPipeTextureHolder {
    private static final Map<String, PipeTextureInfo> pipeTextures = new HashMap<>();


    public static class PipeTextureInfo {
        public final TextureAtlasSprite inTexture;
        public final TextureAtlasSprite sideTexture;

        public PipeTextureInfo(TextureAtlasSprite inTexture, TextureAtlasSprite sideTexture) {
            this.inTexture = inTexture;
            this.sideTexture = sideTexture;
        }
    }

    public static void registerIcon(TextureMap map) {
        pipeTextures.clear();
        for (FluidPipeType type : FluidPipeType.values()) {
            register(map, type.name);
        }
        for (ItemPipeType type : ItemPipeType.values()) {
            register(map, type.name);
        }
    }

    private static void register(TextureMap map, String name) {
        if (pipeTextures.containsKey(name)) {
            return;
        }

        ResourceLocation in = new ResourceLocation(GTValues.MODID, String.format("blocks/pipe/pipe_%s_in", name));
        ResourceLocation side = new ResourceLocation(GTValues.MODID, String.format("blocks/pipe/pipe_%s_side", name));
        pipeTextures.put(name, new PipeTextureInfo(map.registerSprite(in), map.registerSprite(side)));
    }

    public static PipeTextureInfo getTexture(String name) {
        return pipeTextures.get(name);
    }
}
