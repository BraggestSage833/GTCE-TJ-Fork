package gregtech.common.covers;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.ICoverable;
import gregtech.api.render.Textures;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class CoverSolarPanel extends CoverBehavior implements ITickable {

    private final int EUt;


    public CoverSolarPanel(ICoverable coverHolder, EnumFacing attachedSide, int EUt) {
        super(coverHolder, attachedSide);
        this.EUt = EUt;
    }

    @Override
    public boolean canAttach() {
        return coverHolder.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null) != null && attachedSide == EnumFacing.UP;
    }

    @Override
    public void renderCover(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline, Cuboid6 plateBox, BlockRenderLayer layer) {
        if(EUt == Integer.MAX_VALUE){
            Textures.SOLAR_PANEL_SUPRA.renderSided(attachedSide, plateBox, renderState, pipeline, translation);
        }
        else {
            Textures.SOLAR_PANEL.renderSided(attachedSide, plateBox, renderState, pipeline, translation);
        }
    }

    @Override
    public void update() {
        World world = coverHolder.getWorld();
        BlockPos blockPos = coverHolder.getPos().up();
        if (canSeeSunClearly(world, blockPos) || world.provider.getDimension() == -1) {
            IEnergyContainer energyContainer = coverHolder.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null);
            if(energyContainer != null) {
                energyContainer.acceptEnergyFromNetwork(null, EUt, 1);
            }
        }
    }

    private boolean canSeeSunClearly(World world, BlockPos blockPos) {
        if (!world.canSeeSky(blockPos)) {
            return false;
        }
        if (world.isRaining()) {
            Biome biome = world.getBiome(blockPos);
            if (biome.canRain() || biome.getEnableSnow()) {
                return false;
            }
        }
        return world.isDaytime();
    }
}
