package gregtech.common.sound;

import gregtech.api.capability.IControllable;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.common.ConfigHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public final class MachineSoundManager {
    private static final Map<BlockPos, GTSoundLoop> ACTIVE_SOUNDS = new HashMap<>();
    private static final int MAX_DISTANCE = 20;
    private static final int TICK_SKIP = 4;

    public static void update(MetaTileEntity mte) {
        if (!shouldUpdateThisTick(mte)) return;

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

    public static boolean isPlaying(BlockPos pos) {
        return ACTIVE_SOUNDS.containsKey(pos);
    }

    private static void playSoundLoop(MetaTileEntity mte, SoundEvent sound) {
        BlockPos pos = mte.getPos();

        if (ACTIVE_SOUNDS.containsKey(pos)) return;

        GTSoundLoop loop = new GTSoundLoop(sound, pos, false, true);
        ACTIVE_SOUNDS.put(pos, loop);

        Minecraft.getMinecraft()
                .getSoundHandler()
                .playSound(loop);
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
