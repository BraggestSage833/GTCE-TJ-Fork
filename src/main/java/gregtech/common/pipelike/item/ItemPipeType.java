package gregtech.common.pipelike.item;

import gregtech.api.pipenet.block.material.IMaterialPipeType;
import gregtech.api.unification.ore.OrePrefix;

public enum ItemPipeType implements IMaterialPipeType<ItemPipeProperties> {

    SMALL("small", 0.375f, 0.5f, OrePrefix.pipeItemSmall, 1.5f),
    NORMAL("medium", 0.5f, 1f, OrePrefix.pipeItemMedium, 1.f),
    LARGE("large", 0.75f, 2f, OrePrefix.pipeItemLarge, .75f),
    HUGE("huge", .875f,4f, OrePrefix.pipeItemHuge, .5f);

    public final String name;
    public final float thickness;
    public final float capacityMultiplier;
    public final float resistanceMultiplier;
    public final OrePrefix orePrefix;


    ItemPipeType(String name, float thickness, float capacityMultiplier, OrePrefix orePrefix, float resistanceMultiplier) {
        this.name = name;
        this.thickness = thickness;
        this.capacityMultiplier = capacityMultiplier;
        this.resistanceMultiplier = resistanceMultiplier;
        this.orePrefix = orePrefix;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getThickness() {
        return thickness;
    }

    @Override
    public OrePrefix getOrePrefix() {
        return orePrefix;
    }

    @Override
    public ItemPipeProperties modifyProperties(ItemPipeProperties baseProperties) {
        return new ItemPipeProperties((int) ((baseProperties.getPriority() * resistanceMultiplier) + 0.5),
                baseProperties.getTransferRate() * capacityMultiplier);
    }

    @Override
    public boolean isPaintable() {
        return true;
    }
}
