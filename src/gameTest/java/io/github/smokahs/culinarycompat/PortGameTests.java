package io.github.smokahs.culinarycompat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import io.github.smokahs.culinarycompat.bridges.Bridges;
import io.github.smokahs.culinarycompat.compat.cfb.CFB;
import io.github.smokahs.culinarycompat.network.Sync;
import io.github.smokahs.culinarycompat.recipe.MultiCutting;
import io.github.smokahs.culinarycompat.registry.Components;
import io.netty.buffer.Unpooled;

@GameTestHolder(CulinaryCompat.MODID)
@PrefixGameTestTemplate(false)
public final class PortGameTests {
	@GameTest(template = "empty")
	public static void recipeAndPayloadRoundTrip(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();
		var ops = registries.createSerializationContext(JsonOps.INSTANCE);
		var serializer = new MultiCutting.Serializer();
		var recipe = serializer.codec().codec()
				.parse(ops,
						JsonParser.parseString(
								"""
										{"ingredients":[{"item":"minecraft:carrot"},{"item":"minecraft:potato"}],"result":{"id":"minecraft:bread","count":2}}
										"""))
				.getOrThrow();
		var inventory = new ItemStackHandler(2);
		inventory.setStackInSlot(0, new ItemStack(Items.POTATO));
		inventory.setStackInSlot(1, new ItemStack(Items.CARROT));
		helper.assertTrue(recipe.matches(new RecipeWrapper(inventory), helper.getLevel()),
				"Unordered cutting inputs must match");
		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try {
			serializer.streamCodec().encode(buffer, recipe);
			var decoded = serializer.streamCodec().decode(buffer);
			helper.assertTrue(decoded.getResultItem(registries).getCount() == 2,
					"Recipe network codec must retain result count");
			var entry = new Bridges.Entry("test", ResourceLocation.parse("culinarycompat:test"),
					new ArrayList<>(recipe.getIngredients()), new ItemStack(Items.BREAD, 2),
					new ItemStack(Items.CRAFTING_TABLE));
			Sync.STREAM_CODEC.encode(buffer, new Sync(List.of(entry)));
			var sync = Sync.STREAM_CODEC.decode(buffer);
			helper.assertTrue(sync.entries().size() == 1 && sync.entries().getFirst().inputs().size() == 2,
					"Bridge payload must preserve ingredients and outputs");
		} finally {
			buffer.release();
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void kitchenRecipesStayOutOfCraftingTables(GameTestHelper helper) {
		var ingredients = NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.CARROT));
		var recipe = new io.github.smokahs.culinarycompat.registry.Recipes.BridgeShapeless(null, "",
				net.minecraft.world.item.crafting.CraftingBookCategory.MISC, new ItemStack(Items.BREAD), ingredients);
		var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(new ItemStack(Items.CARROT)));
		helper.assertTrue(!recipe.matches(input, helper.getLevel()),
				"Kitchen bridges must not work on crafting tables");
		var codec = io.github.smokahs.culinarycompat.registry.Recipes.BRIDGE_SHAPELESS_SERIALIZER.get().streamCodec();
		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		try {
			codec.encode(buffer, recipe);
			var decoded = codec.decode(buffer);
			helper.assertTrue(!decoded.matches(input, helper.getLevel()) && decoded.isSpecial(),
					"Client recipe sync must retain kitchen restrictions");
			var shapedIngredients = NonNullList.withSize(6, Ingredient.EMPTY);
			shapedIngredients.set(0, Ingredient.of(Items.CARROT));
			var shaped = new io.github.smokahs.culinarycompat.registry.Recipes.BridgeShaped(null, "",
					net.minecraft.world.item.crafting.CraftingBookCategory.MISC, 3, 2, shapedIngredients,
					new ItemStack(Items.BREAD));
			var shapedCodec = io.github.smokahs.culinarycompat.registry.Recipes.BRIDGE_SHAPED_SERIALIZER.get()
					.streamCodec();
			shapedCodec.encode(buffer, shaped);
			var decodedShaped = shapedCodec.decode(buffer);
			helper.assertTrue(decodedShaped.getIngredients().size() == 6 && decodedShaped.isSpecial(),
					"Shaped kitchen recipes must preserve empty matrix cells");
		} finally {
			buffer.release();
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void kitchenCraftingRequiresWorkstation(GameTestHelper helper) {
		if (CFB.isActive())
			KitchenAssertions.run(helper);
		helper.succeed();
	}

	private static final class KitchenAssertions {
		private static void run(GameTestHelper helper) {
			var container = new net.minecraft.world.SimpleContainer(2);
			container.setItem(0, new ItemStack(Items.CARROT));
			var provider = new net.blay09.mods.cookingforblockheads.kitchen.ContainerKitchenItemProvider(container);
			var kitchen = new net.blay09.mods.cookingforblockheads.api.Kitchen() {
				@Override
				public List<net.blay09.mods.cookingforblockheads.api.KitchenItemProvider> getItemProviders(
						net.minecraft.world.entity.player.Player player) {
					return List.of(provider);
				}
				@Override
				public List<net.blay09.mods.cookingforblockheads.api.KitchenItemProcessor> getItemProcessors() {
					return List.of();
				}
				@Override
				public boolean canProcess(net.minecraft.world.item.crafting.RecipeType<?> type) {
					return true;
				}
			};
			var id = ResourceLocation.parse("culinarycompat:workstation_test");
			var recipe = new io.github.smokahs.culinarycompat.registry.Recipes.BridgeShapeless(id, "",
					net.minecraft.world.item.crafting.CraftingBookCategory.MISC, new ItemStack(Items.BREAD),
					NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.CARROT)));
			Bridges.register(
					new Bridges.Entry("gametest", id, List.of(Ingredient.of(Items.CARROT)), new ItemStack(Items.BREAD),
							new ItemStack(io.github.smokahs.culinarycompat.registry.Items.BAKEWARE.get())));
			try {
				var context = new net.blay09.mods.cookingforblockheads.crafting.CraftingContext(kitchen, null);
				var operation = context
						.createOperation(new net.minecraft.world.item.crafting.RecipeHolder<>(id, recipe));
				helper.assertTrue(!operation.prepare().canCraft(), "Missing workstation must prevent kitchen crafting");
				container.setItem(1, new ItemStack(io.github.smokahs.culinarycompat.registry.Items.BAKEWARE.get()));
				helper.assertTrue(operation.prepare().canCraft(), "A stored workstation must enable kitchen crafting");
				var output = operation.craft(null, helper.getLevel().registryAccess());
				helper.assertTrue(output.is(Items.BREAD) && container.getItem(0).isEmpty(),
						"Kitchen crafting must consume ingredients and produce the result");
			} finally {
				Bridges.clearBySource("gametest");
			}
		}
	}

	@GameTest(template = "empty")
	public static void persistentAccessPointComponent(GameTestHelper helper) {
		var stack = new ItemStack(io.github.smokahs.culinarycompat.registry.Items.BAKEWARE.get());
		var position = GlobalPos.of(helper.getLevel().dimension(), new BlockPos(12, 64, 20));
		stack.set(Components.ACCESS_POINT.get(), position);
		var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
		var decoded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
		helper.assertTrue(position.equals(decoded.get(Components.ACCESS_POINT.get())),
				"Access point must survive item serialization");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void kitchenBridgeRecipesAreAddressable(GameTestHelper helper) {
		if (CFB.isActive()) {
			helper.assertTrue(!Bridges.getAll().isEmpty(),
					"A kitchen with the optional food mods must generate bridge recipes");
			for (var entry : Bridges.getAll()) {
				helper.assertTrue(helper.getLevel().getRecipeManager().byKey(entry.bridgeId()).isPresent(),
						"Kitchen recipe must be addressable by its holder ID: " + entry.bridgeId());
			}
			var board = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("farmersdelight:cutting_board"));
			helper.setBlock(new BlockPos(1, 0, 1), net.minecraft.world.level.block.Blocks.STONE);
			helper.setBlock(new BlockPos(1, 1, 1), board);
			helper.assertTrue(
					helper.getBlockEntity(new BlockPos(1, 1,
							1)) instanceof io.github.smokahs.culinarycompat.recipe.MultiCuttingExtras,
					"Cutting board Mixin must expose extra ingredient slots");
		}
		helper.succeed();
	}
}
