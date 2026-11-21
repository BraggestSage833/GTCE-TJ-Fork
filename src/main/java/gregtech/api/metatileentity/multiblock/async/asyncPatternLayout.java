package gregtech.api.metatileentity.multiblock.async;

import net.minecraft.util.EnumFacing;

import java.util.List;

public final class asyncPatternLayout {
    public final EnumFacing facing;
    public final List<PatternLayoutEntry> entries;

    public asyncPatternLayout(EnumFacing facing, List<PatternLayoutEntry> entries) {
        this.facing = facing;
        this.entries = entries;
    }
}
