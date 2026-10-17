package gregtech.api.capability;

public interface IControllable {

    boolean isWorkingEnabled();

    void setWorkingEnabled(boolean isActivationAllowed);

    default boolean isHasProblems() {
        return false;
    }
}
