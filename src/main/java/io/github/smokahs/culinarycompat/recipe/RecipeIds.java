package io.github.smokahs.culinarycompat.recipe;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Keeps recipe identities when adapting holder-based recipes into kitchen
 * bridges.
 */
public final class RecipeIds {
	private static final Map<Recipe<?>, ResourceLocation> IDS = new IdentityHashMap<>();
	private RecipeIds() {
	}
	public static synchronized <T extends Recipe<?>> T remember(ResourceLocation id, T recipe) {
		IDS.put(recipe, id);
		return recipe;
	}
	public static synchronized ResourceLocation id(Recipe<?> recipe) {
		ResourceLocation id = IDS.get(recipe);
		if (id == null)
			throw new IllegalStateException("Recipe was not indexed: " + recipe);
		return id;
	}
	public static List<Recipe<?>> values(RecipeManager manager) {
		return manager.getRecipes().stream().<Recipe<?>>map(holder -> remember(holder.id(), holder.value())).toList();
	}
	public static <I extends RecipeInput, T extends Recipe<I>> List<T> allFor(RecipeManager manager,
			RecipeType<T> type) {
		return manager.getAllRecipesFor(type).stream().map(holder -> remember(holder.id(), holder.value())).toList();
	}
	public static void replace(RecipeManager manager, List<Recipe<?>> recipes) {
		manager.replaceRecipes(
				recipes.stream().<RecipeHolder<?>>map(recipe -> new RecipeHolder<>(id(recipe), recipe)).toList());
	}
}
