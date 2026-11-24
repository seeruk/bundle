package dev.seeruk.bundle.core.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives access to every stat a counter holds, as Vanilla only looks stats up one at a time. Without
 * this, we'd have to get every stat in the registries and iterate over them all, even if they're
 * zero (i.e. not present?).
 */
@Mixin(StatsCounter.class)
public interface StatsCounterAccessor {
    @Accessor("stats")
    Object2IntMap<Stat<?>> getStats();
}
