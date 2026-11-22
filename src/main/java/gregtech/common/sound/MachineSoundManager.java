package gregtech.common.sound;

import gregtech.api.capability.IControllable;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.common.ConfigHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public final class MachineSoundManager {
    private static final Map<BlockPos, GTSoundLoop> ACTIVE_SOUNDS = new HashMap<>();
    private static final int MAX_DISTANCE = 20;
    private static final int TICK_SKIP = 4;
    private static World lastWorld;

    private static final java.util.Random RANDOM = new java.util.Random();


    public static void update(MetaTileEntity mte) {
        World current = Minecraft.getMinecraft().world;
        if (current != lastWorld) {
            ACTIVE_SOUNDS.clear();
            lastWorld = current;
        }

        if (!shouldUpdateThisTick(mte)) {
            return;
        }

        SoundEvent sound = mte.getSound();
        if (sound == null) {
            stop(mte);
            return;
        }

        if (!playerInRange(mte.getPos())) {
            stop(mte);
            return;
        }

        if (!allowedToPlay(mte)) {
            stop(mte);
            return;
        }

        playSoundLoop(mte, sound);
    }


    public static void stop(MetaTileEntity mte) {
        stop(mte.getPos());
    }

    public static void stop(BlockPos pos) {
        GTSoundLoop loop = ACTIVE_SOUNDS.remove(pos);

        if (loop != null) {
            loop.setFade(true);
        }
    }

    public static void clear() {
        ACTIVE_SOUNDS.clear();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        MachineSoundManager.clear();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            MachineSoundManager.clear();
        }
    }

    private static void playSoundLoop(MetaTileEntity mte, SoundEvent sound) {
        BlockPos pos = mte.getPos();

        GTSoundLoop existing = ACTIVE_SOUNDS.get(pos);
        if (existing != null) {
            if (!existing.isDonePlaying() && Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(existing)) {
                return;
            }

            ACTIVE_SOUNDS.remove(pos);
        }

        final int DELAY_MAX_TICKS = 40;
        int delay = mte.getTimer() < DELAY_MAX_TICKS ? RANDOM.nextInt(DELAY_MAX_TICKS) : 0; // 0 to 1,5 s

        GTSoundLoop loop = new GTSoundLoop(sound, pos, false, true, delay);
        ACTIVE_SOUNDS.put(pos, loop);
        Minecraft.getMinecraft().getSoundHandler().playSound(loop);
    }

    private static boolean allowedToPlay(MetaTileEntity mte) {
        boolean allowed =
                ConfigHolder.soundConfiguration.machineSounds &&
                        !mte.isMuffled() &&
                        mte.shouldPlaySound();

        if (mte instanceof IControllable controllable) {
            allowed &= controllable.isWorkingEnabled();
        }

        return allowed;
    }

    private static boolean playerInRange(BlockPos pos) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return false;

        return player.getDistanceSq(pos) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    private static boolean shouldUpdateThisTick(MetaTileEntity mte) {
        return (mte.getOffsetTimer() + mte.getPos().hashCode()) % TICK_SKIP == 0;
    }
}
