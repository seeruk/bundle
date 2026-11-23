package dev.seeruk.bundle.resourcezones.edit;

import net.minecraft.util.StringRepresentable;

/**
 * Enumerates the possible different edit modes available for editing resource zones.
 */
public enum EditMode implements StringRepresentable {
    ADD("adding"),
    REMOVE("removing"),
    PAUSED("paused");

    private final String name;

    /**
     * @param name How the mode's described to players, e.g. in their editing status.
     */
    EditMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
