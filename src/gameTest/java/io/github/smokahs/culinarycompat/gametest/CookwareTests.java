package io.github.smokahs.culinarycompat.gametest;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("culinarycompat")
@PrefixGameTestTemplate(false)
public final class CookwareTests {
    @GameTest(template = "empty")
    public static void cookingPotShapeTracksKitchenSupport(GameTestHelper helper) {
        checkShape(helper, "farmersdelight:cooking_pot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skilletShapeTracksKitchenSupport(GameTestHelper helper) {
        checkShape(helper, "farmersdelight:skillet");
        helper.succeed();
    }

    private static void checkShape(GameTestHelper helper, String id) {
        var block = BlockTestSupport.block(id);
        var pos = helper.absolutePos(BlockTestSupport.POS);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(BlockTestSupport.POS.below(), net.minecraft.world.level.block.Blocks.STONE);
            var state = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction);
            helper.setBlock(BlockTestSupport.POS, state);
            var nativeShape = state.getShape(helper.getLevel(), pos);
            helper.assertTrue(!nativeShape.isEmpty(), id + " must expose a native shape");
            for (String support : new String[]{"minecraft:stone", "minecraft:oak_slab", "minecraft:air",
                    "cookingforblockheads:counter", "cookingforblockheads:corner",
                    "cookingforblockheads:cooking_table"}) {
                helper.setBlock(BlockTestSupport.POS.below(), BlockTestSupport.block(support));
                boolean lowered = support.startsWith("cookingforblockheads:") && !support.endsWith(":cooking_table");
                BlockTestSupport.sameShape(helper, state.getShape(helper.getLevel(), pos),
                        lowered ? nativeShape.move(0, -1.0 / 16.0, 0) : nativeShape,
                        id + " shape for " + direction + " on " + support);
            }
        }
    }
}
