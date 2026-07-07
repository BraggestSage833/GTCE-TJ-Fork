package gregtech.common.sound;

import net.minecraft.util.SoundEvent;

import static gregtech.api.GregTechAPI.soundSystem;

public class GTSoundEvents {
    public static SoundEvent WRENCH;
    public static SoundEvent SCREWDRIVER;
    public static SoundEvent SOFT_MALLET;
    public static SoundEvent CROWBAR;
    public static SoundEvent BATH;
    public static SoundEvent MIXER;
    public static SoundEvent ELECTROLYZER;
    public static SoundEvent CENTRIFUGE;
    public static SoundEvent FORGE_HAMMER;
    public static SoundEvent MACERATOR;
    public static SoundEvent CHEMICAL_REACTOR;
    public static SoundEvent ARC;
    public static SoundEvent BOILER;
    public static SoundEvent FURNACE;
    public static SoundEvent TURBINE;
    public static SoundEvent COMBUSTION;
    public static SoundEvent ASSEMBLER;
    public static SoundEvent COMPRESSOR;
    public static SoundEvent REPLICATOR;
    public static SoundEvent COOLING;
    public static SoundEvent FIRE;
    public static SoundEvent MOTOR;
    public static SoundEvent CUT;
    public static SoundEvent SPRAY_CAN;
    public static SoundEvent PORTABLE_SCANNER;
    public static SoundEvent MINER;
    public static SoundEvent DRILL;
    public static SoundEvent COMPUTATION;
    public static SoundEvent PUMP;


    public static void register() {
        WRENCH = soundSystem.registerSound("wrench");
        SCREWDRIVER = soundSystem.registerSound("screwdriver");
        SOFT_MALLET = soundSystem.registerSound("soft_mallet");
        CROWBAR = soundSystem.registerSound("crowbar");
        FORGE_HAMMER = soundSystem.registerSound("forge_hammer");
        MACERATOR = soundSystem.registerSound("macerator");
        CHEMICAL_REACTOR = soundSystem.registerSound("chemical");
        ASSEMBLER = soundSystem.registerSound("assembler");
        CENTRIFUGE = soundSystem.registerSound("centrifuge");
        COMPRESSOR = soundSystem.registerSound("compressor");
        ELECTROLYZER = soundSystem.registerSound("electrolyzer");
        MIXER = soundSystem.registerSound("mixer");
        REPLICATOR = soundSystem.registerSound("replicator");
        ARC = soundSystem.registerSound("arc");
        BOILER = soundSystem.registerSound("boiler");
        FURNACE = soundSystem.registerSound("furnace");
        TURBINE = soundSystem.registerSound("turbine");
        COMBUSTION = soundSystem.registerSound("combustion");
        BATH = soundSystem.registerSound("bath");
        COOLING = soundSystem.registerSound("cooling");
        FIRE = soundSystem.registerSound("fire");
        MOTOR = soundSystem.registerSound("motor");
        CUT = soundSystem.registerSound("cut");
        SPRAY_CAN = soundSystem.registerSound("spray_can");
        PORTABLE_SCANNER = soundSystem.registerSound("portable_scanner");
        MINER = soundSystem.registerSound("miner");
        DRILL = soundSystem.registerSound("drill");
        COMPUTATION = soundSystem.registerSound("computation");
        PUMP = soundSystem.registerSound("pump");
    }
}
