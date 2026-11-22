package dev.seeruk.bundle.resourcezones.particles;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * How the particles outlining resource zones are distributed to mark zones. Measured from the
 * player's eyes.
 */
public enum ParticleStyle implements StringRepresentable {
    /**
     * A short band, which works anywhere, e.g. in caves.
     */
    BAND("band", 3, 1),

    /**
     * A taller wall, which is easier to see from a distance, e.g. while flying. Its particles are
     * further apart vertically, so it doesn't need too many more.
     */
    WALL("wall", 24, 4);

    public static final Codec<ParticleStyle> CODEC = StringRepresentable.fromEnum(ParticleStyle::values);

    private final String name;
    private final int reach;
    private final int verticalSpread;

    /**
     * @param name           The name it's set by, in config and commands.
     * @param reach          How many blocks above and below the player's eyes particles go.
     * @param verticalSpread Multiplier between vertical and horizontal particle spacing.
     */
    ParticleStyle(String name, int reach, int verticalSpread) {
        this.name = name;
        this.reach = reach;
        this.verticalSpread = verticalSpread;
    }

    public int reach() {
        return reach;
    }

    public int verticalSpread() {
        return verticalSpread;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
