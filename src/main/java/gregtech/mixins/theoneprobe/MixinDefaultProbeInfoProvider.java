package gregtech.mixins.theoneprobe;

import gregtech.common.pipelike.fluidpipe.net.FluidPipeNet;
import gregtech.common.pipelike.fluidpipe.net.WorldFluidPipeNet;
import gregtech.common.pipelike.fluidpipe.tile.TileEntityFluidPipe;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.apiimpl.ProbeConfig;
import mcjty.theoneprobe.apiimpl.providers.DefaultProbeInfoProvider;
import mcjty.theoneprobe.config.Config;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = DefaultProbeInfoProvider.class, remap = false)
public class MixinDefaultProbeInfoProvider {

    @Shadow
    private void addFluidInfo(
            IProbeInfo probeInfo,
            ProbeConfig config,
            FluidStack fluidStack,
            int maxContents
    ) {}

    @Inject(method = "addProbeInfo", at = @At("TAIL"))
    private void onAddProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player,
                                World world, IBlockState blockState, IProbeHitData data,
                                CallbackInfo ci) {
        BlockPos pos = data.getPos();
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityFluidPipe pipe)) {
            return;
        }

        if (!(pipe.getPipeBlock().getWorldPipeNet(world) instanceof WorldFluidPipeNet worldNet)) {
            return;
        }

        FluidPipeNet net = worldNet.getNetFromPos(pos);
        if (net == null) {
            return;
        }

        FluidStack recent = net.getRecentFluid(pos, world.getTotalWorldTime());
        if (recent == null) {
            return;
        }

        addFluidInfo(probeInfo,Config.getDefaultConfig(), recent, pipe.getNodeData().throughput);
    }

    @Inject(method = "showTankInfo", at = @At("HEAD"), cancellable = true)
    private void onShowTankInfo(IProbeInfo probeInfo, World world, BlockPos pos, CallbackInfo ci) {
        if (world.getTileEntity(pos) instanceof TileEntityFluidPipe) {
            ci.cancel();
        }
    }


}
