package io.github.smokahs.culinarycompat.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.blay09.mods.cookingforblockheads.api.CacheHint;
import net.blay09.mods.cookingforblockheads.api.IngredientToken;
import net.blay09.mods.cookingforblockheads.api.KitchenItemProvider;
import net.minecraft.world.item.ItemStack;

/**
 * Translates the former bulk/slot test operations to CFB's one-item token API.
 */
final class ProviderApi {
	private final KitchenItemProvider provider;
	private final List<ItemStack> candidates;
	private final List<IngredientToken> reserved = new ArrayList<>();
	private final Map<net.minecraft.world.item.Item, IngredientToken> consumed = new HashMap<>();

	ProviderApi(KitchenItemProvider provider, List<ItemStack> candidates) {
		this.provider = provider;
		this.candidates = candidates;
	}

	int getSlots() {
		return (int) candidates.stream()
				.filter(stack -> provider.findIngredient(stack, List.of(), CacheHint.NONE) != null).count();
	}

	ItemStack getStackInSlot(int slot) {
		var token = provider.findIngredient(candidates.get(slot), List.of(), CacheHint.NONE);
		return token == null ? ItemStack.EMPTY : token.peek();
	}

	int getCountInSlot(int slot) {
		return available(candidates.get(slot), List.of());
	}

	private int available(ItemStack stack, List<IngredientToken> alreadyReserved) {
		var tokens = new ArrayList<>(alreadyReserved);
		int count = 0;
		IngredientToken token;
		while (count < 128 && (token = provider.findIngredient(stack, tokens, CacheHint.NONE)) != null) {
			if (token.peek().isEmpty())
				break;
			tokens.add(token);
			count++;
		}
		return count;
	}

	int getSimulatedUseCount(int slot) {
		return available(candidates.get(slot), List.of()) - available(candidates.get(slot), reserved);
	}

	int getSimulatedUseCount(String id) {
		return getSimulatedUseCount(index(net.minecraft.core.registries.BuiltInRegistries.ITEM
				.get(net.minecraft.resources.ResourceLocation.parse(id))));
	}

	private int index(net.minecraft.world.item.Item item) {
		for (int i = 0; i < candidates.size(); i++)
			if (candidates.get(i).is(item))
				return i;
		throw new AssertionError("Required provider item is missing: " + item);
	}

	void resetSimulation() {
		reserved.clear();
	}

	ItemStack useItemStack(int slot, int amount, boolean simulate, List<ItemStack> containers,
			boolean requireContainer) {
		return useItemStack(candidates.get(slot), amount, simulate, containers, requireContainer);
	}

	ItemStack useItemStack(ItemStack stack, int amount, boolean simulate, List<ItemStack> containers,
			boolean requireContainer) {
		if (requireContainer)
			throw new AssertionError("CFB token API has no container requirement parameter");
		var tokens = new ArrayList<>(reserved);
		var requested = new ArrayList<IngredientToken>();
		for (int i = 0; i < amount; i++) {
			var token = provider.findIngredient(stack, tokens, CacheHint.NONE);
			if (token == null || token.peek().isEmpty())
				return ItemStack.EMPTY;
			tokens.add(token);
			requested.add(token);
		}
		if (simulate) {
			reserved.addAll(requested);
			return requested.getFirst().peek().copyWithCount(amount);
		}
		ItemStack result = ItemStack.EMPTY;
		for (var token : requested) {
			var item = token.consume();
			if (item.isEmpty())
				return ItemStack.EMPTY;
			consumed.put(item.getItem(), token);
			if (result.isEmpty())
				result = item.copy();
			else
				result.grow(item.getCount());
		}
		return result;
	}

	ItemStack returnItemStack(ItemStack stack, Object ignored) {
		var token = consumed.get(stack.getItem());
		return token == null ? stack : token.restore(stack);
	}
}
