package io.github.smokahs.culinarycompat.gametest;

import java.util.List;

import net.blay09.mods.cookingforblockheads.CookingForBlockheadsConfig;
import net.blay09.mods.cookingforblockheads.api.capability.IKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.registry.CookingRegistry;
import net.blay09.mods.cookingforblockheads.tile.SinkBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("culinarycompat")
@PrefixGameTestTemplate(false)
public final class SinkTests {
	@GameTest(template = "empty")
	public static void finiteSinkBottleBucketBoundariesSimulationAndRefund(GameTestHelper helper) {
		var config = CookingForBlockheadsConfig.getActive();
		boolean previous = config.sinkRequiresWater;
		var waterItems = CookingRegistry.getWaterItems();
		var savedWaterItems = waterItems.stream().map(ItemStack::copy).toList();
		try {
			config.sinkRequiresWater = true;
			// Register a known bottle-sized water item through CFB's public API;
			// this fixture is not a recipe and is restored after the test.
			var waterBottle = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
			CookingRegistry.addWaterItem(waterBottle);
			helper.setBlock(BlockTestSupport.POS, BlockTestSupport.block("cookingforblockheads:sink"));
			var sink = (SinkBlockEntity) helper.getBlockEntity(BlockTestSupport.POS);
			var tank = sink.getFluidTank();
			tank.setFluid(Fluids.WATER, 1000);
			var provider = sink.getProviders().stream().map(p -> p.getInstance())
					.filter(IKitchenItemProvider.class::isInstance).map(IKitchenItemProvider.class::cast).findFirst()
					.orElseThrow();
			int bottle = slot(provider, Items.POTION);
			int bucket = slot(provider, Items.WATER_BUCKET);
			for (int amount : new int[]{0, 249, 250, 999, 1000}) {
				tank.setAmount(amount);
				helper.assertTrue(!provider.getStackInSlot(bottle).isEmpty() == (amount >= 250),
						"Bottle availability boundary: " + amount);
				helper.assertTrue(!provider.getStackInSlot(bucket).isEmpty() == (amount >= 1000),
						"Bucket availability boundary: " + amount);
			}
			tank.setAmount(1000);
			for (int i = 0; i < 4; i++) {
				var simulated = provider.useItemStack(bottle, 1, true, List.of(), false);
				helper.assertTrue(simulated.is(Items.POTION) && tank.getAmount() == 1000,
						"Bottle simulation must reserve 250 mB without draining tank");
				helper.assertTrue(provider.getSimulatedUseCount(bottle) == i + 1,
						"Bottle simulated count must use quarter-bucket units");
			}
			helper.assertTrue(provider.useItemStack(bottle, 1, true, List.of(), false).isEmpty(),
					"Reservations must prevent overusing finite water");
			helper.assertTrue(provider.getSimulatedUseCount(bucket) == 1, "Four bottle reservations equal one bucket");
			provider.resetSimulation();
			helper.assertTrue(provider.getSimulatedUseCount(bottle) == 0, "Reset clears reservations");
			tank.setAmount(250);
			var bottleResult = provider.useItemStack(bottle, 1, false, List.of(), false);
			helper.assertTrue(bottleResult.is(Items.POTION) && tank.getAmount() == 0,
					"Bottle use must drain exactly 250 mB");
			provider.returnItemStack(bottleResult, null);
			helper.assertTrue(tank.getAmount() == 250, "Bottle refund must restore exactly 250 mB");
			tank.setAmount(999);
			helper.assertTrue(
					provider.useItemStack(bucket, 1, false, List.of(), false).isEmpty() && tank.getAmount() == 999,
					"Insufficient bucket use must not drain tank");
			tank.setAmount(1000);
			var bucketResult = provider.useItemStack(bucket, 1, false, List.of(), false);
			helper.assertTrue(bucketResult.is(Items.WATER_BUCKET) && tank.getAmount() == 0,
					"Bucket use must drain exactly 1000 mB");
			provider.returnItemStack(bucketResult, null);
			helper.assertTrue(tank.getAmount() == 1000, "Bucket refund must restore exactly 1000 mB");
		} finally {
			config.sinkRequiresWater = previous;
			waterItems.clear();
			waterItems.addAll(savedWaterItems);
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void infiniteSinkSuppliesWaterWithEmptyTank(GameTestHelper helper) {
		var config = CookingForBlockheadsConfig.getActive();
		boolean previous = config.sinkRequiresWater;
		try {
			config.sinkRequiresWater = false;
			helper.setBlock(BlockTestSupport.POS, BlockTestSupport.block("cookingforblockheads:sink"));
			var sink = (SinkBlockEntity) helper.getBlockEntity(BlockTestSupport.POS);
			sink.getFluidTank().setFluid(Fluids.WATER, 0);
			var provider = sink.getProviders().stream().map(p -> p.getInstance())
					.filter(IKitchenItemProvider.class::isInstance).map(IKitchenItemProvider.class::cast).findFirst()
					.orElseThrow();
			int bucket = slot(provider, Items.WATER_BUCKET);
			helper.assertTrue(provider.useItemStack(bucket, 1, false, List.of(), false).is(Items.WATER_BUCKET),
					"Infinite sink must supply water even with empty tank");
			helper.assertTrue(sink.getFluidTank().getAmount() == Integer.MAX_VALUE,
					"Infinite sink exposes unbounded water through its tank API");
			config.sinkRequiresWater = true;
			helper.assertTrue(sink.getFluidTank().getAmount() == 0, "Infinite use must not create stored fluid");
		} finally {
			config.sinkRequiresWater = previous;
		}
		helper.succeed();
	}

	static int slot(IKitchenItemProvider provider, Item item) {
		for (int i = 0; i < provider.getSlots(); i++) {
			if (provider.getStackInSlot(i).is(item))
				return i;
		}
		throw new AssertionError("Required provider item is missing: " + item);
	}
}
