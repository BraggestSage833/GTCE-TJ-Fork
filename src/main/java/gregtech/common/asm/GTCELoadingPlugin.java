package gregtech.common.asm;

import com.google.common.collect.ImmutableList;
import gregtech.api.GTValues;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.Name;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import zone.rong.mixinbooter.ILateMixinLoader;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Name("GTCELoadingPlugin")
@MCVersion(ForgeVersion.mcVersion)
@TransformerExclusions("gregtech.common.asm.")
@SortingIndex(1001)
public class GTCELoadingPlugin implements IFMLLoadingPlugin, ILateMixinLoader {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {"gregtech.common.asm.GTCETransformer"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }


    @Override
    public List<String> getMixinConfigs() {
        final ImmutableList.Builder<String> configs = ImmutableList.builder();
        //configs.add("mixins.tj.gregtech.json", "mixins.tj.gregicality.json");
        if (GTValues.isModLoaded(GTValues.MODID_TOP)) {
            configs.add("mixins.gregtech.theoneprobe.json");
        }
        return configs.build();
    }
}
