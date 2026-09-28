package gregtech.integration.jei.multiblock.channel;


import gregtech.api.GTValues;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.util.BlockInfo;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.Function;


/**
 * Defines a JEI preview placeholder and how to render it.
 * Supports simple variants or custom resolvers.
 * New placeholders are created with create() at the point of use.
 */

public final class PlaceholderType {
    private static final Map<String, PlaceholderType> REGISTRY = new LinkedHashMap<>();

    private final String id;
    private volatile PlaceholderResolver resolver;
    private volatile Set<Channel> dependentChannels = Collections.emptySet();

    private PlaceholderType(String id) {
        this.id = id;
    }

    public static PlaceholderType create(String id) {
        return REGISTRY.computeIfAbsent(id, PlaceholderType::new);
    }

    public static PlaceholderType get(String id) {
        return REGISTRY.get(id);
    }

    public static Collection<PlaceholderType> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public String getID() {
        return id;
    }

    public void registerResolver(PlaceholderResolver resolver) {
        //this.resolver = resolver;
        registerResolver(resolver, new Channel[0]);
    }

    public void registerResolver(PlaceholderResolver resolver, Channel... channels) {
        this.resolver = resolver;
        this.dependentChannels = channels.length == 0 ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(Arrays.asList(channels)));
    }

    public Set<Channel> getDependentChannels() {
        return dependentChannels;
    }


    public int getEffectiveChannelMax(Channel channel, int channelMax) {
        PlaceholderResolver current = this.resolver;
        if (current == null || channelMax <= 1) {
            return 1;
        }

        ChannelState probe = new ChannelState();
        probe.set(channel, 1);
        BlockInfo previous = current.create(new PlaceholderContext(probe, EnumFacing.NORTH,BlockPos.ORIGIN));
        int lastChange = 1;

        for (int value = 2; value <= channelMax; value++) {
            probe.set(channel,value);
            BlockInfo resolved = current.create(new PlaceholderContext(probe, EnumFacing.NORTH, BlockPos.ORIGIN));
            if (!sameOutput(previous, resolved)) {
                lastChange = value;
            }
            previous = resolved;
        }

        return lastChange;
    }

    private static boolean sameOutput(BlockInfo a, BlockInfo b) {
        if (a == null || b == null) {
            return a==b;
        }

        if (!Objects.equals(a.getBlockState(),b.getBlockState())) {
            return false;
        }

        TileEntity ta = a.getTileEntity();
        TileEntity tb = b.getTileEntity();

        if (ta instanceof MetaTileEntityHolder && tb instanceof MetaTileEntityHolder) {
            MetaTileEntity ma = ((MetaTileEntityHolder) ta).getMetaTileEntity();
            MetaTileEntity mb = ((MetaTileEntityHolder) tb).getMetaTileEntity();
            if (ma == null || mb == null) {
                return ma == mb;
            }
            return ma.metaTileEntityId.equals(mb.metaTileEntityId);
        }
        return (ta == null) == (tb == null);
    }


    public <E> void registerVariant(Channel channel, E[] variants,
                                    Function<E, ItemStack> itemProvider,
                                    Function<E, IBlockState> stateProvider) {
        registerVariant(channel,variants,itemProvider, stateProvider,1);
    }

    public <E> void registerVariant(Channel channel, E[] variants,
                                    Function<E, ItemStack> itemProvider,
                                    Function<E, IBlockState> stateProvider,
                                    int offset) {
        int counter = 1;
        for (E variant : variants) {
            if (itemProvider != null) {
                channel.registerIndicator(itemProvider.apply(variant),counter);
            }
            counter++;
        }
        int variantCount = variants.length;
        registerResolver(context -> new BlockInfo(stateProvider.apply(variants[clampIndex(context.getTier(channel),offset, variantCount)])), channel);
    }

    public BlockInfo resolve(PlaceholderContext context) {
        PlaceholderResolver resolver = this.resolver;
        return resolver == null ? null : resolver.create(context);
    }

    public static int clampIndex(int value, int offset, int arrayLen) {
        return Math.max(0, Math.min(value - offset, arrayLen - 1));
    }

    public static int mteVoltageClamp(int voltageTier) {
        return Math.min(voltageTier, GTValues.V.length -1);
    }

    public static BlockInfo mteHolder(MetaTileEntity mte, EnumFacing facing) {
        MetaTileEntityHolder holder = new MetaTileEntityHolder();
        holder.setMetaTileEntity(mte);
        holder.getMetaTileEntity().setFrontFacing(facing);
        return new BlockInfo(MetaBlocks.MACHINE.getDefaultState(), holder, null);
    }


    @FunctionalInterface
    public interface PlaceholderResolver {
        BlockInfo create(PlaceholderContext context);
    }

    public static final class PlaceholderContext {
        private final ChannelState channelState;
        public final EnumFacing facing;
        public final BlockPos position;

        public PlaceholderContext(ChannelState state, EnumFacing facing, BlockPos position) {
            this.channelState = state;
            this.facing = facing;
            this.position = position;
        }

        public int getTier(Channel channel) {
            return this.channelState.get(channel);
        }
    }
}
