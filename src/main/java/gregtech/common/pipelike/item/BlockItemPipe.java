package gregtech.common.pipelike.item;

import com.google.common.base.Preconditions;
import gregtech.api.pipenet.block.material.BlockMaterialPipe;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import gregtech.api.unification.material.type.Material;
import gregtech.common.pipelike.item.net.ItemPipeNet;
import gregtech.common.pipelike.item.net.WorldItemPipeNet;
import gregtech.common.pipelike.item.tile.TileEntityItemPipe;
import gregtech.common.pipelike.item.tile.TileEntityItemPipeTickable;
import gregtech.common.render.ItemPipeRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

public class BlockItemPipe extends BlockMaterialPipe<ItemPipeType, ItemPipeProperties, WorldItemPipeNet> {

    private final SortedMap<Material, ItemPipeProperties> enabledMaterials = new TreeMap<>();

    public BlockItemPipe() {
        setHarvestLevel("wrench", 1);
    }

    public void addPipeMaterial(Material material, ItemPipeProperties itemPipeProperties) {
        Preconditions.checkNotNull(material, "material");
        Preconditions.checkNotNull(itemPipeProperties, "itemPipeProperties");
        Preconditions.checkArgument(Material.MATERIAL_REGISTRY.getNameForObject(material) != null, "material is not registered");
        this.enabledMaterials.put(material, itemPipeProperties);
    }


    public Collection<Material> getEnabledMaterials() {
        return Collections.unmodifiableSet(enabledMaterials.keySet());
    }

    @Override
    public Class<ItemPipeType> getPipeTypeClass() {
        return ItemPipeType.class;
    }

    @Override
    public WorldItemPipeNet getWorldPipeNet(World world) {
        return WorldItemPipeNet.getWorldPipeNet(world);
    }

    @Override
    protected ItemPipeProperties createProperties(ItemPipeType itemPipeType, Material material) {
        return itemPipeType.modifyProperties(enabledMaterials.getOrDefault(material, getFallbackType()));
    }

    @Override
    protected ItemPipeProperties getFallbackType() {
        return enabledMaterials.values().iterator().next();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (Material material : enabledMaterials.keySet()) {
            for (ItemPipeType itemPipeType : ItemPipeType.values()) {
                items.add(getItem(itemPipeType, material));
            }
        }
    }


    private int getInventoryConnections(IPipeTile<ItemPipeType, ItemPipeProperties> pipe) {
        int connections = 0;

        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos offsetPos = pipe.getPipePos().offset(side);
            TileEntity neighbor = pipe.getPipeWorld().getTileEntity(offsetPos);

            if (neighbor == null || getPipeTileEntity(neighbor) != null) {
                continue;
            }

            if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) {
                connections |= 1 << side.getIndex();
            }
        }

        return connections;
    }

    @Override
    public int getActiveVisualConnections(IPipeTile<ItemPipeType, ItemPipeProperties> selfTile) {
        return getInventoryConnections(selfTile);
    }

    @Override
    public int getActiveNodeConnections(IBlockAccess world, BlockPos nodePos, IPipeTile<ItemPipeType, ItemPipeProperties> selfTileEntity) {
        return getInventoryConnections(selfTileEntity);
    }

    @Override
    public TileEntityPipeBase<ItemPipeType, ItemPipeProperties> createNewTileEntity(boolean supportsTicking) {
        return supportsTicking ? new TileEntityItemPipeTickable() : new TileEntityItemPipe();
    }

    @Override
    protected void onActiveModeChange(World world, BlockPos pos, boolean isActiveNow, boolean isInitialChange) {
        ItemPipeNet net = getWorldPipeNet(world).getNetFromPos(pos);
        if (net != null) {
            net.invalidateRoutes();
        }


        TileEntityItemPipe oldTileEntity = (TileEntityItemPipe) world.getTileEntity(pos);
        if (!(oldTileEntity instanceof TileEntityItemPipeTickable) && isActiveNow) {
            TileEntityItemPipeTickable newTileEntity = new TileEntityItemPipeTickable();
            newTileEntity.transferDataFrom(oldTileEntity);
            newTileEntity.setActive(true);
            world.setTileEntity(pos, newTileEntity);
        } else if (oldTileEntity instanceof TileEntityItemPipeTickable) {
            ((TileEntityItemPipeTickable) oldTileEntity).setActive(isActiveNow);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return ItemPipeRenderer.INSTANCE.getBlockRenderType();
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected Pair<TextureAtlasSprite, Integer> getParticleTexture(World world, BlockPos blockPos) {
        return ItemPipeRenderer.INSTANCE.getParticleTexture((TileEntityItemPipe) world.getTileEntity(blockPos));
    }
}
