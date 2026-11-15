package dev.seeruk.bundle.discord;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;

public class SeersDiscordGameTest implements CustomTestMethodInvoker {
    @GameTest
    public void test(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.AIR, 0, 0, 0);
        helper.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
