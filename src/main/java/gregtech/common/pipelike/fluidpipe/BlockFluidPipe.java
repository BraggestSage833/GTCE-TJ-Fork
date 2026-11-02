package gregtech.common.pipelike.fluidpipe;

import com.google.common.base.Preconditions;
import gregtech.api.damagesources.DamageSources;
import gregtech.api.pipenet.block.material.BlockMaterialPipe;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import gregtech.api.unification.material.type.Material;
import gregtech.common.pipelike.fluidpipe.net.FluidPipeNet;
import gregtech.common.pipelike.fluidpipe.net.WorldFluidPipeNet;
import gregtech.common.pipelike.fluidpipe.tile.FluidPipeFluidHandler;
import gregtech.common.pipelike.fluidpipe.tile.TileEntityFluidPipe;
import gregtech.common.pipelike.fluidpipe.tile.TileEntityFluidPipeTickable;
import gregtech.common.render.FluidPipeRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

public class BlockFluidPipe extends BlockMaterialPipe<FluidPipeType, FluidPipeProperties, WorldFluidPipeNet> {

    private final SortedMap<Material, FluidPipeProperties> enabledMaterials = new TreeMap<>();

    public BlockFluidPipe() {
        setHarvestLevel("pickaxe", 1);
    }

    public void addPipeMaterial(Material material, FluidPipeProperties fluidPipeProperties) {
        Preconditions.checkNotNull(material, "material");
        Preconditions.checkNotNull(fluidPipeProperties, "fluidPipeProperties");
        Preconditions.checkArgument(Material.MATERIAL_REGISTRY.getNameForObject(material) != null, "material is not registered");
        this.enabledMaterials.put(material, fluidPipeProperties);
    }

    public Collection<Material> getEnabledMaterials() {
        return Collections.unmodifiableSet(enabledMaterials.keySet());
    }

    @Override
    public Class<FluidPipeType> getPipeTypeClass() {
        return FluidPipeType.class;
    }

    @Override
    public WorldFluidPipeNet getWorldPipeNet(World world) {
        return WorldFluidPipeNet.getWorldPipeNet(world);
    }

    @Override
    protected FluidPipeProperties createProperties(FluidPipeType fluidPipeType, Material material) {
        return fluidPipeType.modifyProperties(enabledMaterials.getOrDefault(material, getFallbackType()));
    }

    @Override
    protected FluidPipeProperties getFallbackType() {
        return enabledMaterials.values().iterator().next();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (Material material : enabledMaterials.keySet()) {
            for (FluidPipeType fluidPipeType : FluidPipeType.values()) {
                items.add(getItem(fluidPipeType, material));
            }
        }
    }


    @Override
    public int getActiveVisualConnections(IPipeTile<FluidPipeType, FluidPipeProperties> selfTile) {
        int activeNodeConnections = 0;
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos offsetPos = selfTile.getPipePos().offset(side);
            TileEntity tileEntity = selfTile.getPipeWorld().getTileEntity(offsetPos);
            if (tileEntity == null || getPipeTileEntity(tileEntity) != null) {
                continue;
            }

            EnumFacing opposite = side.getOpposite();
            IFluidHandler sourceHandler = getRawFluidHandler(selfTile, side);
            IFluidHandler receivedHandler = tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, opposite);
            if (sourceHandler != null && receivedHandler != null) {
                activeNodeConnections |= 1 << side.getIndex();
            }
        }

        return activeNodeConnections;
    }

    @Override
    public int getActiveNodeConnections(IBlockAccess world, BlockPos nodePos, IPipeTile<FluidPipeType, FluidPipeProperties> selfTileEntity) {
        int activeNodeConnections = 0;
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos offsetPos = selfTileEntity.getPipePos().offset(side);
            TileEntity tileEntity = selfTileEntity.getPipeWorld().getTileEntity(offsetPos);
            if (tileEntity == null || getPipeTileEntity(tileEntity) != null) {
                continue;
            }

            EnumFacing opposite = side.getOpposite();
            IFluidHandler sourceHandler = getRawFluidHandler(selfTileEntity, side);
            IFluidHandler receivedHandler = tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, opposite);
            if (sourceHandler != null && receivedHandler != null) {
                activeNodeConnections |= 1 << side.getIndex();
            }
        }

        return activeNodeConnections;
    }

    private IFluidHandler getRawFluidHandler(IPipeTile<FluidPipeType, FluidPipeProperties> pipeTile, EnumFacing side) {
        if (pipeTile instanceof TileEntityPipeBase) {
            return ((TileEntityPipeBase<FluidPipeType, FluidPipeProperties>) pipeTile).getCapabilityInternal(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
        }

        return pipeTile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
    }

    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (worldIn.isRemote || !(entityIn instanceof  EntityLivingBase)) {
            return;
        }

        long now = worldIn.getTotalWorldTime();
        if (now % 20 != 0L) {
            return;
        }

        FluidPipeNet pipeNet = getWorldPipeNet(worldIn).getNetFromPos(pos);
        if (pipeNet == null) {
            return;
        }

        int fluidTemp = pipeNet.getRecentTemp(pos, now);
        if (fluidTemp == FluidPipeNet.NO_RECENT_FLUID) {
            return;
        }

        EntityLivingBase entityLiving = (EntityLivingBase) entityIn;
        if (fluidTemp >= FluidPipeNet.HOT_VALUE) {
            float damageAmount = (fluidTemp - 363) / 40.f;
            entityLiving.attackEntityFrom(DamageSources.getHeatDamage(), damageAmount);
        } else if (fluidTemp <= FluidPipeNet.COLD_VALUE) {
            float damageAmount = fluidTemp / 10.f;
            entityLiving.attackEntityFrom(DamageSources.getFrostDamage(), damageAmount);
        }
    }

    @Override
    public TileEntityPipeBase<FluidPipeType, FluidPipeProperties> createNewTileEntity(boolean supportsTicking) {
        return supportsTicking ? new TileEntityFluidPipeTickable() : new TileEntityFluidPipe();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return FluidPipeRenderer.INSTANCE.getBlockRenderType();
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected Pair<TextureAtlasSprite, Integer> getParticleTexture(World world, BlockPos blockPos) {
        return FluidPipeRenderer.INSTANCE.getParticleTexture((TileEntityFluidPipe) world.getTileEntity(blockPos));
    }
}
