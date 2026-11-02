package gregtech.api.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ResourceHelper {

    private static final String DIR_FORMAT = "textures/%s.png";
    /**
     * @param modid           the modid of the texture
     * @param textureResource the location of the texture
     * @param format          if the location should be formatted to include "textures/" and ".png"
     * @return if the resource exists
     */
    @SideOnly(Side.CLIENT)
    public static boolean doResourcepacksHaveTexture(@NotNull String modid, @NotNull String textureResource,
                                                     boolean format) {
        if (format) textureResource = String.format(DIR_FORMAT, textureResource);
        return doResourcepacksHaveResource(modid, textureResource);
    }

    /**
     * @param modid    the modid of the texture, formatted with the root dir and file extension
     * @param resource the location of the resource
     * @return if the resource exists
     */
    @SideOnly(Side.CLIENT)
    public static boolean doResourcepacksHaveResource(@NotNull String modid, @NotNull String resource) {
        return doResourcepacksHaveResource(new ResourceLocation(modid, resource));
    }

    /**
     * @param resource the location of the resource, formatted with the root dir and file extension
     * @return if the resource exists
     */
    @SideOnly(Side.CLIENT)
    public static boolean doResourcepacksHaveResource(@NotNull ResourceLocation resource) {
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        try {
            // check if the texture file exists
            IResource ignored = manager.getResource(resource);
            IOUtils.closeQuietly(ignored);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
