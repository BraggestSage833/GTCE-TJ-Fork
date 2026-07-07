package gregtech.api.sound;

import net.minecraft.client.audio.ISound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public interface IGTSoundSystem {
    SoundEvent registerSound(String modName, String soundName);

    SoundEvent registerSound(String soundName);

}
