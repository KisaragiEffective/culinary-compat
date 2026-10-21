package io.github.smokahs.culinarycompat.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.registries.BuiltInRegistries;

public final class BlockTestSupport {
    public static final BlockPos POS = new BlockPos(1, 1, 1);

    private BlockTestSupport() {}

    public static Block block(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) {
            throw new AssertionError("Required integration block is missing: " + id);
        }
        return BuiltInRegistries.BLOCK.get(key);
    }

    public static void sameShape(GameTestHelper helper, VoxelShape actual, VoxelShape expected, String message) {
        helper.assertTrue(!Shapes.joinIsNotEmpty(actual, expected, BooleanOp.NOT_SAME), message);
    }
}
