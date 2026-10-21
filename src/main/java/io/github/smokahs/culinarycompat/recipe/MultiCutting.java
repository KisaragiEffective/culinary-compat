package io.github.smokahs.culinarycompat.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import io.github.smokahs.culinarycompat.registry.Recipes;

public class MultiCutting implements Recipe<RecipeWrapper> {
	public static final int MAX_INPUTS = 9;

	private final ResourceLocation id;
	private final NonNullList<Ingredient> ingredients;
	private final ItemStack result;

	public MultiCutting(ResourceLocation id, NonNullList<Ingredient> ingredients, ItemStack result) {
		this.id = id;
		if (id != null)
			RecipeIds.remember(id, this);
		this.ingredients = ingredients;
		this.result = result;
	}

	@Override
	public boolean matches(RecipeWrapper inv, Level level) {
		List<ItemStack> stored = new ArrayList<>();
		for (int i = 0; i < inv.size(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty()) {
				stored.add(s);
			}
		}
		if (stored.size() != ingredients.size()) {
			return false;
		}
		boolean[] used = new boolean[stored.size()];
		for (Ingredient ing : ingredients) {
			int found = -1;
			for (int j = 0; j < stored.size(); j++) {
				if (!used[j] && ing.test(stored.get(j))) {
					found = j;
					break;
				}
			}
			if (found < 0) {
				return false;
			}
			used[found] = true;
		}
		return true;
	}

	@Override
	public ItemStack assemble(RecipeWrapper inv, net.minecraft.core.HolderLookup.Provider registries) {
		return result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= ingredients.size();
	}

	@Override
	public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return ingredients;
	}

	public ResourceLocation getId() {
		return id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return Recipes.MULTI_CUTTING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return Recipes.MULTI_CUTTING_TYPE.get();
	}

	public static final class Serializer implements RecipeSerializer<MultiCutting> {
		private static final com.mojang.serialization.MapCodec<MultiCutting> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder
				.mapCodec(
						instance -> instance
								.group(Ingredient.CODEC_NONEMPTY.listOf()
										.validate(list -> list.isEmpty() || list.size() > MAX_INPUTS
												? com.mojang.serialization.DataResult
														.error(() -> "Expected 1-9 ingredients")
												: com.mojang.serialization.DataResult.success(list))
										.fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
										ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result))
								.apply(instance, (ingredients, result) -> {
									NonNullList<Ingredient> list = NonNullList.create();
									list.addAll(ingredients);
									return new MultiCutting(null, list, result);
								}));
		private static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, MultiCutting> STREAM_CODEC = net.minecraft.network.codec.StreamCodec
				.of((buf, recipe) -> {
					buf.writeVarInt(recipe.ingredients.size());
					for (Ingredient ingredient : recipe.ingredients)
						Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
					ItemStack.STREAM_CODEC.encode(buf, recipe.result);
				}, buf -> {
					int size = buf.readVarInt();
					if (size < 1 || size > MAX_INPUTS)
						throw new IllegalArgumentException("Invalid ingredient count: " + size);
					NonNullList<Ingredient> list = NonNullList.create();
					for (int i = 0; i < size; i++)
						list.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
					return new MultiCutting(null, list, ItemStack.STREAM_CODEC.decode(buf));
				});
		@Override
		public com.mojang.serialization.MapCodec<MultiCutting> codec() {
			return CODEC;
		}
		@Override
		public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, MultiCutting> streamCodec() {
			return STREAM_CODEC;
		}
	}

}
