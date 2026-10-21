package io.github.smokahs.culinarycompat.gametest;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import io.github.smokahs.culinarycompat.registry.Blocks;

@GameTestHolder("culinarycompat")
@PrefixGameTestTemplate(false)
public final class BakewareTests {
	@GameTest(template = "empty")
	public static void placementFacesPlayerInEveryDirection(GameTestHelper helper) {
		var player = helper.makeMockPlayer();
		var pos = helper.absolutePos(BlockTestSupport.POS);
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			player.setYRot(direction.toYRot());
			var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
					new ItemStack(Blocks.BAKEWARE.get()),
					new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
			var state = Blocks.BAKEWARE.get().getStateForPlacement(context);
			helper.assertTrue(state.getValue(HorizontalDirectionalBlock.FACING) == direction.getOpposite(),
					"Bakeware must face the placing player: " + direction);
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void shapeTracksDirectionAndKitchenSupport(GameTestHelper helper) {
		var pos = helper.absolutePos(BlockTestSupport.POS);
		for (String support : new String[]{"minecraft:air", "minecraft:stone", "cookingforblockheads:counter",
				"cookingforblockheads:corner", "cookingforblockheads:cooking_table"}) {
			helper.setBlock(BlockTestSupport.POS.below(), BlockTestSupport.block(support));
			boolean lowered = support.startsWith("cookingforblockheads:") && !support.endsWith(":cooking_table");
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				var state = Blocks.BAKEWARE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
						direction);
				helper.setBlock(BlockTestSupport.POS, state);
				VoxelShape expected = direction.getAxis() == Direction.Axis.Z
						? Shapes.or(Block.box(2, 0, 3, 14, 4, 13), Block.box(1, 2, 6, 2, 4, 10),
								Block.box(14, 2, 6, 15, 4, 10))
						: Shapes.or(Block.box(3, 0, 2, 13, 4, 14), Block.box(6, 2, 1, 10, 4, 2),
								Block.box(6, 2, 14, 10, 4, 15));
				BlockTestSupport.sameShape(helper, state.getShape(helper.getLevel(), pos),
						lowered ? expected.move(0, -1.0 / 16.0, 0) : expected,
						"Bakeware shape for " + direction + " on " + support);
			}
		}
		helper.succeed();
	}
}
