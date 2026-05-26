package gregtech.common.sound;

import gregtech.api.GTValues;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import gregtech.common.ConfigHolder;
import paulscode.sound.SoundSystemConfig;
import gregtech.api.sound.IGTSoundSystem;

public final class GTSoundSystem implements IGTSoundSystem {
    private static final GTSoundSystem INSTANCE = new GTSoundSystem();

    public static GTSoundSystem getInstance() {
        return INSTANCE;
    }

    @SideOnly(Side.CLIENT)
    public void init() {
        int maxNumberOfSounds = ConfigHolder.soundConfiguration.maxNumberOfSounds;
        SoundSystemConfig.setNumberNormalChannels(maxNumberOfSounds);
    }

    @Override
    public SoundEvent registerSound(String soundName) {
        return registerSound(GTValues.MODID, soundName);
    }

    @Override
    public SoundEvent registerSound(String modName, String soundName) {
        ResourceLocation location = new ResourceLocation(modName, soundName);
        SoundEvent event = new SoundEvent(location);
        event.setRegistryName(location);
        ForgeRegistries.SOUND_EVENTS.register(event);
        return event;
    }
}
