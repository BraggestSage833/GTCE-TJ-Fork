package gregtech.api.unification.ore.integration;

import gregtech.api.unification.material.Materials;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.unification.ore.StoneType;
import net.minecraft.block.SoundType;
import net.minecraft.util.ResourceLocation;
import mod.emt.ebt.init.EBTBlocks;

public class EBTStones {
    public static StoneType RED_ROCK;
    public static StoneType CRAG_ROCK;

    public static void init() {
        RED_ROCK = new StoneType(14, "red_rock", new ResourceLocation("ebt", "blocks/red_rock"),
                SoundType.STONE, OrePrefix.ore, Materials.Stone, "pickaxe", 0,
                () -> EBTBlocks.red_rock.getDefaultState(),
                state -> state.getBlock() == EBTBlocks.red_rock);


        CRAG_ROCK = new StoneType(15, "crag_rock", new ResourceLocation("ebt", "blocks/crag_rock"),
                SoundType.STONE, OrePrefix.ore, Materials.Stone, "pickaxe", 0,
                () -> EBTBlocks.crag_rock.getDefaultState(),
                state -> state.getBlock() == EBTBlocks.crag_rock);
    }
}
