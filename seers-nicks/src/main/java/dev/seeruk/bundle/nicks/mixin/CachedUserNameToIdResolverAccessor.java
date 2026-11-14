package dev.seeruk.bundle.nicks.mixin;

import net.minecraft.server.players.CachedUserNameToIdResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Gives access to the server's cache of usernames it knows about, keyed by lowercase name, without
 * looking up anything it doesn't have from Mojang.
 */
@Mixin(CachedUserNameToIdResolver.class)
public interface CachedUserNameToIdResolverAccessor {
    @Accessor("profilesByName")
    Map<String, ?> getProfilesByName();
}
