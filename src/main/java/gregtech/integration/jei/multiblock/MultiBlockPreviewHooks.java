package gregtech.integration.jei.multiblock;

import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import net.minecraft.world.IBlockAccess;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public final class MultiBlockPreviewHooks {
    private static final List<BiConsumer<IBlockAccess, MultiblockControllerBase>> SCENE_LISTENERS = new CopyOnWriteArrayList<>();

    public static void addSceneListener(BiConsumer<IBlockAccess, MultiblockControllerBase> listener)  {
        SCENE_LISTENERS.add(listener);
    }

    static void fireSceneBuilt(IBlockAccess world, MultiblockControllerBase controller) {
        for (BiConsumer<IBlockAccess, MultiblockControllerBase> listener : SCENE_LISTENERS) {
            listener.accept(world, controller);
        }
    }
}
