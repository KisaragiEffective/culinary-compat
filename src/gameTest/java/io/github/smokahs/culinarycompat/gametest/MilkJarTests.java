package io.github.smokahs.culinarycompat.gametest;

import java.util.List;

import net.blay09.mods.cookingforblockheads.api.KitchenItemProvider;
import net.blay09.mods.cookingforblockheads.block.entity.MilkJarBlockEntity;
import net.blay09.mods.cookingforblockheads.compat.Compat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("culinarycompat")
@PrefixGameTestTemplate(false)
public final class MilkJarTests {
	@GameTest(template = "empty")
	public static void milkBottleAndBucketQuantitiesSimulationAndRefund(GameTestHelper helper) {
		helper.setBlock(BlockTestSupport.POS, BlockTestSupport.block("cookingforblockheads:milk_jar"));
		var jar = (MilkJarBlockEntity) helper.getBlockEntity(BlockTestSupport.POS);
		var tank = jar.getFluidTank();
		tank.setFluid(Compat.getMilkFluid(), 1000);
		var kitchenProvider = jar.getProviders().stream().map(p -> p.getInstance())
				.filter(KitchenItemProvider.class::isInstance).map(KitchenItemProvider.class::cast).findFirst()
				.orElseThrow();
		var bottleId = ResourceLocation.fromNamespaceAndPath("farmersdelight", "milk_bottle");
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(bottleId), "Required FD milk bottle must be loaded");
		var milkBottle = BuiltInRegistries.ITEM.get(bottleId);
		var provider = new ProviderApi(kitchenProvider, List.of(new net.minecraft.world.item.ItemStack(milkBottle),
				new net.minecraft.world.item.ItemStack(Items.MILK_BUCKET)));
		int bottle = SinkTests.slot(provider, milkBottle);
		int bucket = SinkTests.slot(provider, Items.MILK_BUCKET);
		for (int amount : new int[]{0, 249, 250, 750, 999, 1000}) {
			tank.setAmount(amount);
			helper.assertTrue(!provider.getStackInSlot(bottle).isEmpty() == (amount >= 250),
					"Milk bottle availability boundary: " + amount);
			helper.assertTrue(!provider.getStackInSlot(bucket).isEmpty() == (amount >= 1000),
					"Milk bucket availability boundary: " + amount);
			helper.assertTrue(provider.getCountInSlot(bottle) == amount / 250,
					"Milk bottle count must use quarter-bucket units: " + amount);
			helper.assertTrue(provider.getCountInSlot(bucket) == amount / 1000,
					"Milk bucket count must retain bucket units: " + amount);
		}
		tank.setAmount(1000);
		for (int i = 0; i < 4; i++) {
			helper.assertTrue(
					provider.useItemStack(bottle, 1, true, List.of(), false).is(milkBottle) && tank.getAmount() == 1000,
					"Bottle simulation reserves milk without draining");
			helper.assertTrue(provider.getSimulatedUseCount(bottle) == i + 1,
					"Simulated milk bottle count must increase by one");
		}
		helper.assertTrue(provider.useItemStack(bottle, 1, true, List.of(), false).isEmpty(),
				"Milk simulation must reject over-reservation");
		helper.assertTrue(provider.getSimulatedUseCount(bucket) == 1, "Four milk bottles reserve one bucket");
		provider.resetSimulation();
		helper.assertTrue(provider.getSimulatedUseCount(bottle) == 0, "Reset releases milk reservations");
		tank.setAmount(250);
		var bottleResult = provider.useItemStack(bottle, 1, false, List.of(), false);
		helper.assertTrue(bottleResult.is(milkBottle) && tank.getAmount() == 0,
				"Milk bottle must consume exactly 250 mB");
		provider.returnItemStack(bottleResult, null);
		helper.assertTrue(tank.getAmount() == 250, "Milk bottle return must refund exactly 250 mB");
		tank.setAmount(1000);
		var bucketResult = provider.useItemStack(bucket, 1, false, List.of(), false);
		helper.assertTrue(bucketResult.is(Items.MILK_BUCKET) && tank.getAmount() == 0,
				"Milk bucket must consume exactly 1000 mB");
		provider.returnItemStack(bucketResult, null);
		helper.assertTrue(tank.getAmount() == 1000, "Milk bucket return must refund exactly 1000 mB");
		tank.setAmount(249);
		helper.assertTrue(
				provider.useItemStack(bottle, 1, false, List.of(), false).isEmpty() && tank.getAmount() == 249,
				"Insufficient milk use must not drain fluid");
		helper.succeed();
	}
}
