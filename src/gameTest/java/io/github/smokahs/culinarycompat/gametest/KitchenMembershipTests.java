package io.github.smokahs.culinarycompat.gametest;

import net.blay09.mods.cookingforblockheads.block.entity.SinkBlockEntity;
import net.blay09.mods.cookingforblockheads.crafting.KitchenImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import io.github.smokahs.culinarycompat.compat.cfb.CFB;

@GameTestHolder("culinarycompat")
@PrefixGameTestTemplate(false)
public final class KitchenMembershipTests {
	private static final BlockPos ORIGIN = new BlockPos(2, 2, 2);

	private static CFB.KitchenMemberView kitchen(GameTestHelper helper) {
		var kitchen = new KitchenImpl(helper.getLevel(), helper.absolutePos(ORIGIN));
		helper.assertTrue(kitchen instanceof CFB.KitchenMemberView, "Kitchen membership Mixin must be applied");
		return (CFB.KitchenMemberView) kitchen;
	}

	@GameTest(template = "empty")
	public static void adjacentCookwareRecognizedInAllSixDirections(GameTestHelper helper) {
		helper.setBlock(ORIGIN, BlockTestSupport.block("cookingforblockheads:counter"));
		for (String target : new String[]{"culinarycompat:bakeware", "farmersdelight:cutting_board",
				"farmersdelight:skillet", "farmersdelight:cooking_pot"}) {
			for (Direction direction : Direction.values()) {
				var pos = ORIGIN.relative(direction);
				helper.setBlock(pos, BlockTestSupport.block(target));
				helper.assertTrue(
						kitchen(helper).culinarycompat$getMemberBlocks().contains(ResourceLocation.parse(target)),
						"Kitchen must recognize " + target + " to its " + direction);
				helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
			}
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void airNonTargetsAndDistantCookwareAreExcluded(GameTestHelper helper) {
		var empty = kitchen(helper);
		helper.assertTrue(empty.culinarycompat$getMemberBlocks().isEmpty(),
				"Empty origin and air neighbours are not members");
		helper.setBlock(ORIGIN, BlockTestSupport.block("cookingforblockheads:counter"));
		helper.setBlock(ORIGIN.east(), net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(ORIGIN.south(2), BlockTestSupport.block("culinarycompat:bakeware"));
		var members = kitchen(helper).culinarycompat$getMemberBlocks();
		helper.assertTrue(members.contains(ResourceLocation.fromNamespaceAndPath("cookingforblockheads", "counter")),
				"The visited kitchen origin itself must be recorded");
		helper.assertTrue(!members.contains(ResourceLocation.fromNamespaceAndPath("minecraft", "stone")),
				"Unrelated adjacent blocks must not become cookware members");
		helper.assertTrue(!members.contains(ResourceLocation.fromNamespaceAndPath("culinarycompat", "bakeware")),
				"Cookware separated by air must not be recognized as adjacent");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void realKitchenItemProvidersAreExposed(GameTestHelper helper) {
		helper.setBlock(ORIGIN, BlockTestSupport.block("cookingforblockheads:counter"));
		helper.setBlock(ORIGIN.east(), BlockTestSupport.block("cookingforblockheads:sink"));
		var sink = (SinkBlockEntity) helper.getBlockEntity(ORIGIN.east());
		var sinkInstances = sink.getProviders().stream().map(p -> p.getInstance()).toList();
		var providers = kitchen(helper).culinarycompat$getKitchenItemProviders();
		helper.assertTrue(providers.stream().anyMatch(sinkInstances::contains),
				"KitchenMemberView must expose the real adjacent sink provider");
		helper.succeed();
	}
}
