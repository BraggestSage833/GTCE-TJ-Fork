package gregtech.api.metatileentity.multiblock;

import java.util.List;

public interface IMultiAbilityProvider {
    /**
     * Returns all abilities this part provides.
     */
    MultiblockAbility<?>[] getAbilities();

    /**
     * Registers the correct handler for the given ability.
     */
    void registerAbilityFor(MultiblockAbility<?> ability, List<Object> list);
}


