package gregtech.api.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.util.EnumFacing;

public final class SideRendererCache {

    public enum RendererType {
        NONE,
        FAST,
        DYNAMIC
    }

    // 2 bits per side, 6 sides = 12 bits total
    private int mask;

    private static final RendererType[] VALUES = RendererType.values();

    public void clear() {
        mask = 0;
    }

    public void set(EnumFacing side, RendererType type) {
        int shift = side.getIndex() * 2;

        mask &= ~(3 << shift);
        mask |= type.ordinal() << shift;
    }

    public RendererType get(EnumFacing side) {
        int shift = side.getIndex() * 2;
        int value = (mask >> shift) & 3;

        return VALUES[value];
    }

    public boolean isFast(EnumFacing side) {
        return get(side) == RendererType.FAST;
    }

    public boolean isDynamic(EnumFacing side) {
        return get(side) == RendererType.DYNAMIC;
    }

    public boolean hasAnyRenderer() {
        return mask != 0;
    }


    public boolean hasRenderer(EnumFacing side) {
        return get(side) != RendererType.NONE;
    }

    public int getMask() {
        return mask;
    }
}
