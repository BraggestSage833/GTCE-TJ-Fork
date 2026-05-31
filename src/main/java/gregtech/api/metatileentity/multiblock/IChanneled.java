package gregtech.api.metatileentity.multiblock;

import net.minecraft.item.EnumDyeColor;

public interface IChanneled {
    /**
     * Returns the channel assigned to this component.
     * WHITE (or null) means "unpainted"
     */
    EnumDyeColor getChannel();

    /**
     * Sets the channel for this component.
     * Called by spray cans.
     */
    void setChannel(EnumDyeColor channel);

    /**
     * Returns true if this component is considered "unpainted".
     * Unpainted = wildcard = matches all channels. used to default to old distinct code
     */
    default boolean isWildcard() {
        EnumDyeColor c = getChannel();
        return c == null || c == EnumDyeColor.WHITE;
    }
}
