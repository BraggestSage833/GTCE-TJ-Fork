package gregtech.mixins.theoneprobe;


import gregtech.common.pipelike.fluidpipe.tile.TileEntityFluidPipe;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = mcjty.theoneprobe.network.PacketGetInfo.class, remap = false)
public class MixinPacketGetInfo {

    @Redirect(method = "getProbeInfo",
            at = @At(value = "INVOKE",
                    target = "Lmcjty/theoneprobe/api/IProbeInfoProvider;addProbeInfo(Lmcjty/theoneprobe/api/ProbeMode;Lmcjty/theoneprobe/api/IProbeInfo;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/block/state/IBlockState;Lmcjty/theoneprobe/api/IProbeHitData;)V"))
    private static void filterProviders(IProbeInfoProvider provider, ProbeMode mode, IProbeInfo info, EntityPlayer player, World world, IBlockState state, IProbeHitData data) {
        if ("topaddons:base".equals(provider.getID()) && world.getTileEntity(data.getPos()) instanceof TileEntityFluidPipe) {
            return;
        }
        
        provider.addProbeInfo(mode, info, player, world, state, data);
    }
}
