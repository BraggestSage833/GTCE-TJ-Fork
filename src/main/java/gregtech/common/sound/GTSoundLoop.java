package gregtech.common.sound;


import gregtech.api.block.machines.BlockMachine;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GTSoundLoop extends MovingSound {

    public static final float VOLUME_RAMP = 0.0625f;
    private final boolean whileActive;
    private final boolean whileInactive;
    private int dimension = 0;
    private BlockPos pos = null;
    boolean fade;
    private float targetVolume = 1;

    public GTSoundLoop(SoundEvent sound, BlockPos pos, boolean stopWhenActive,
                       boolean stopWhenInactive) {
        super(sound, SoundCategory.PLAYERS);

        this.whileActive = stopWhenActive;
        this.whileInactive = stopWhenInactive;
        xPosF = pos.getX();
        yPosF = pos.getY();
        zPosF = pos.getZ();
        dimension = Minecraft.getMinecraft().player.dimension;
        repeat = true;
        this.volume = VOLUME_RAMP;
        this.pos = pos;
    }

    @Override
    public void update() {
        if (donePlaying) {
            return;
        }

        if (fade) {
            volume -= VOLUME_RAMP * targetVolume;
            if (volume <= 0) {
                volume = 0;
                stop();
                return;
            }
        } else if (volume < targetVolume) {
            volume += VOLUME_RAMP * targetVolume;
        }

        World world = Minecraft.getMinecraft().player.world;

        if (world.provider.getDimension() != dimension) {
            stop();
            return;
        }

        if (!world.isBlockLoaded(pos)) {
            stop();
            return;
        }

        MetaTileEntity entity = BlockMachine.getMetaTileEntity(world,pos);

        if (entity == null) {
            stop();
            return;
        }

        fade |= entity.isValid() ? whileActive : whileInactive;
    }

    public void stop() {
        this.donePlaying = true;
    }

    public GTSoundLoop setVolume(float volume) {
        targetVolume = volume;
        return this;
    }

    public GTSoundLoop setFade(boolean value) {
        fade = value;
        return this;
    }
}

