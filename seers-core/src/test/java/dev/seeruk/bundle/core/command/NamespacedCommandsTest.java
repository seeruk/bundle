package dev.seeruk.bundle.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NamespacedCommandsTest {
    private static final String MOD_ID = "seers-core-test";
    private static final String NAME = "test-command";
    private static final String NAMESPACED = MOD_ID + ":" + NAME;

    // Avoids Minecraft needing to be bootstrapped for this set of tests.
    private static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    @Nested
    class Register {
        private CommandDispatcher<CommandSourceStack> dispatcher;
        private List<String> builtNames;

        @BeforeEach
        void setUp() {
            dispatcher = new CommandDispatcher<>();
            builtNames = new ArrayList<>();

            NamespacedCommands.register(
                dispatcher, MOD_ID, NAME, name -> {
                    builtNames.add(name);
                    return literal(name)
                        .executes(context -> 1)
                        .then(literal("sub").executes(context -> 2));
                }
            );
        }

        @Test
        void shouldRegisterUnderNameAndNamespacedName() {
            assertNotNull(dispatcher.getRoot().getChild(NAME));
            assertNotNull(dispatcher.getRoot().getChild(NAMESPACED));
        }

        @Test
        void shouldNotRegisterExtraCommands() {
            assertEquals(List.of(NAME, NAMESPACED), builtNames);
        }

        @Test
        void shouldNotUseRedirects() {
            final var plain = dispatcher.getRoot().getChild(NAME);
            final var namespaced = dispatcher.getRoot().getChild(NAMESPACED);

            assertNotSame(plain, namespaced);
            assertNull(plain.getRedirect());
            assertNull(namespaced.getRedirect());
        }

        @Test
        void shouldRunRootCommandUnderBothNames() throws CommandSyntaxException {
            assertEquals(1, dispatcher.execute(NAME, null));
            assertEquals(1, dispatcher.execute(NAMESPACED, null));
        }

        @Test
        void shouldRunSubcommandsUnderBothNames() throws CommandSyntaxException {
            assertEquals(2, dispatcher.execute(NAME + " sub", null));
            assertEquals(2, dispatcher.execute(NAMESPACED + " sub", null));
        }
    }
}
