package gregtech.api.metatileentity.multiblock.async;


public final class PatternLayoutEntry {

    public final int c;
    public final int b;
    public final int a;

    public final int relX;
    public final int relY;
    public final int relZ;

    public PatternLayoutEntry(int c, int b, int a, int relX, int relY, int relZ) {

        this.c = c;
        this.b = b;
        this.a = a;

        this.relX = relX;
        this.relY = relY;
        this.relZ = relZ;
    }
}
