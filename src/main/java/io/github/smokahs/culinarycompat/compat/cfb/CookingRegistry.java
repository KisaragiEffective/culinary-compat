package io.github.smokahs.culinarycompat.compat.cfb;

import java.util.Collection;

import net.blay09.mods.cookingforblockheads.registry.CookingForBlockheadsRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import io.github.smokahs.culinarycompat.recipe.RecipeIds;

/**
 * Adapter for inserting the generated bridge recipes into CFB's modern
 * registry.
 */
public final class CookingRegistry {
	private static final java.util.Map<ResourceLocation, RecipeHolder<?>> GENERATED = new java.util.LinkedHashMap<>();
	private static net.minecraft.world.item.crafting.RecipeManager manager;
	private CookingRegistry() {
	}
	public static void begin(net.minecraft.world.item.crafting.RecipeManager recipeManager) {
		manager = recipeManager;
		GENERATED.clear();
		RecipeIds.values(recipeManager);
	}
	public static void finish() {
		if (manager == null || GENERATED.isEmpty())
			return;
		java.util.Map<ResourceLocation, RecipeHolder<?>> recipes = new java.util.LinkedHashMap<>();
		manager.getRecipes().forEach(holder -> recipes.put(holder.id(), holder));
		recipes.putAll(GENERATED);
		manager.replaceRecipes(recipes.values());
	}
	public static Collection<RecipeHolder<Recipe<?>>> getFoodRecipes(ResourceLocation output) {
		return CookingForBlockheadsRegistry.getRecipesByItemId().get(output);
	}
	public static void addFoodRecipe(Recipe<?> recipe, RegistryAccess registries) {
		ResourceLocation output = BuiltInRegistries.ITEM.getKey(recipe.getResultItem(registries).getItem());
		var recipes = getFoodRecipes(output);
		ResourceLocation id = RecipeIds.id(recipe);
		recipes.removeIf(existing -> existing.id().equals(id));
		recipes.add(new RecipeHolder<>(id, recipe));
		if ("culinarycompat".equals(id.getNamespace()))
			GENERATED.put(id, new RecipeHolder<>(id, recipe));
	}
}
