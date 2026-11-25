package dev.seeruk.bundle.leaders.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.Mannequin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Gives access to mannequin settings Vanilla only uses from saved data or commands.
 */
@Mixin(Mannequin.class)
public interface MannequinInvoker {
    @Invoker("getDescription")
    Component invokeGetDescription();

    @Invoker("setDescription")
    void invokeSetDescription(Component description);

    @Invoker("setImmovable")
    void invokeSetImmovable(boolean immovable);
}
