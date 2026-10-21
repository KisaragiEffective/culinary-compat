package io.github.smokahs.culinarycompat.registry;

import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.smokahs.culinarycompat.CulinaryCompat;
import io.github.smokahs.culinarycompat.config.Configs;
import io.github.smokahs.culinarycompat.recipe.MultiCutting;
import io.github.smokahs.culinarycompat.recipe.RecipeIds;

public final class Recipes {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister
			.create(BuiltInRegistries.RECIPE_SERIALIZER, CulinaryCompat.MODID);
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE,
			CulinaryCompat.MODID);

	public static final Supplier<RecipeSerializer<MultiCutting>> MULTI_CUTTING_SERIALIZER = RECIPE_SERIALIZERS
			.register("multi_cutting", (Supplier<RecipeSerializer<MultiCutting>>) MultiCutting.Serializer::new);
	public static final Supplier<RecipeSerializer<BridgeShapeless>> BRIDGE_SHAPELESS_SERIALIZER = RECIPE_SERIALIZERS
			.register("kitchen_shapeless", () -> serializer(BridgeShapeless.CODEC));
	public static final Supplier<RecipeSerializer<BridgeShaped>> BRIDGE_SHAPED_SERIALIZER = RECIPE_SERIALIZERS
			.register("kitchen_shaped", () -> serializer(BridgeShaped.CODEC));
	public static final Supplier<RecipeType<MultiCutting>> MULTI_CUTTING_TYPE = RECIPE_TYPES.register("multi_cutting",
			() -> new RecipeType<MultiCutting>() {
				@Override
				public String toString() {
					return CulinaryCompat.MODID + ":multi_cutting";
				}
			});

	private Recipes() {
	}

	private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> serializer(
			com.mojang.serialization.MapCodec<T> codec) {
		return new RecipeSerializer<>() {
			@Override
			public com.mojang.serialization.MapCodec<T> codec() {
				return codec;
			}
			@Override
			public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, T> streamCodec() {
				return net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(codec.codec());
			}
		};
	}

	public static class BridgeShapeless extends ShapelessRecipe {
		public static final com.mojang.serialization.MapCodec<BridgeShapeless> CODEC = new ShapelessRecipe.Serializer()
				.codec().xmap(recipe -> new BridgeShapeless(null, recipe.getGroup(), recipe.category(),
						recipe.getResultItem(null), recipe.getIngredients()), recipe -> recipe);
		public BridgeShapeless(ResourceLocation id, String group, CraftingBookCategory category, ItemStack result,
				NonNullList<Ingredient> ingredients) {
			super(group, category, result, ingredients);
			if (id != null)
				RecipeIds.remember(id, this);
		}
		@Override
		public RecipeSerializer<?> getSerializer() {
			return BRIDGE_SHAPELESS_SERIALIZER.get();
		}
	}

	public static class BridgeShaped extends ShapedRecipe {
		public static final com.mojang.serialization.MapCodec<BridgeShaped> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder
				.mapCodec(instance -> instance.group(
						com.mojang.serialization.Codec.intRange(1, 3).fieldOf("width")
								.forGetter(BridgeShaped::getWidth),
						com.mojang.serialization.Codec.intRange(1, 3).fieldOf("height")
								.forGetter(BridgeShaped::getHeight),
						Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.getIngredients()),
						ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.getResultItem(null)))
						.apply(instance, (width, height, ingredients, output) -> {
							var list = NonNullList.<Ingredient>create();
							list.addAll(ingredients);
							return new BridgeShaped(null, "", CraftingBookCategory.MISC, width, height, list, output);
						}));
		public BridgeShaped(ResourceLocation id, String group, CraftingBookCategory category, int width, int height,
				NonNullList<Ingredient> ingredients, ItemStack result) {
			super(group, category, new ShapedRecipePattern(width, height, ingredients, java.util.Optional.empty()),
					result);
			if (id != null)
				RecipeIds.remember(id, this);
		}
		@Override
		public RecipeSerializer<?> getSerializer() {
			return BRIDGE_SHAPED_SERIALIZER.get();
		}
	}

	public static class ToolBridgeShapeless extends BridgeShapeless {
		private final Predicate<ItemStack> toolTest;
		private final ResourceLocation immuneItemId;

		public ToolBridgeShapeless(ResourceLocation id, String group, CraftingBookCategory category, ItemStack result,
				NonNullList<Ingredient> ingredients, Predicate<ItemStack> toolTest, ResourceLocation immuneItemId) {
			super(id, group, category, result, ingredients);
			this.toolTest = toolTest;
			this.immuneItemId = immuneItemId;
		}

		@Override
		public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
			NonNullList<ItemStack> remaining = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
			for (int i = 0; i < remaining.size(); i++) {
				ItemStack stack = inv.getItem(i);
				if (stack.isEmpty())
					continue;
				if (toolTest != null && toolTest.test(stack)) {
					remaining.set(i, preserveTool(stack));
				} else if (stack.hasCraftingRemainingItem()) {
					remaining.set(i, stack.getCraftingRemainingItem());
				}
			}
			return remaining;
		}

		private ItemStack preserveTool(ItemStack stack) {
			ItemStack copy = stack.copy();
			copy.setCount(1);
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(copy.getItem());
			boolean isImmune = immuneItemId != null && immuneItemId.equals(id) && Configs.Common.opNetheriteKnife;
			if (isImmune || !copy.isDamageableItem()) {
				return copy;
			}
			int newDamage = copy.getDamageValue() + 1;
			if (newDamage >= copy.getMaxDamage()) {
				return ItemStack.EMPTY;
			}
			copy.setDamageValue(newDamage);
			return copy;
		}
	}
}
