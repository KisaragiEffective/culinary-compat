package io.github.smokahs.culinarycompat.compat.ae2;

import java.util.Collection;

import net.blay09.mods.cookingforblockheads.api.CacheHint;
import net.blay09.mods.cookingforblockheads.api.IngredientToken;
import net.blay09.mods.cookingforblockheads.api.KitchenItemProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.MachineSource;

/**
 * Provides component-aware, reservable ingredient tokens backed by the linked
 * ME network.
 */
public final class ItemProvider implements KitchenItemProvider {
	private final StationBlockEntity station;
	public ItemProvider(StationBlockEntity station) {
		this.station = station;
	}
	private MEStorage storage() {
		return station.getMode().allowsAEToCFB() ? station.getNetworkStorage() : null;
	}
	@Override
	public IngredientToken findIngredient(Ingredient ingredient, Collection<IngredientToken> reserved, CacheHint hint) {
		var storage = storage();
		if (storage == null)
			return null;
		for (var candidate : storage.getAvailableStacks()) {
			if (candidate.getKey() instanceof AEItemKey key && ingredient.test(key.toStack())) {
				var token = findIngredient(key.toStack(), reserved, hint);
				if (token != null)
					return token;
			}
		}
		return null;
	}
	@Override
	public IngredientToken findIngredient(ItemStack stack, Collection<IngredientToken> reserved, CacheHint hint) {
		var storage = storage();
		if (storage == null || stack.isEmpty())
			return null;
		AEItemKey key = AEItemKey.of(stack);
		long used = reserved.stream()
				.filter(token -> token instanceof Token t && t.owner() == this && t.key.equals(key)).count();
		long available = storage.extract(key, used + 1, Actionable.SIMULATE,
				new MachineSource(station.getActionHost()));
		return available > used ? new Token(key) : null;
	}
	@Override
	public CacheHint getCacheHint(IngredientToken token) {
		return CacheHint.NONE;
	}
	private final class Token implements IngredientToken {
		private final AEItemKey key;
		private Token(AEItemKey key) {
			this.key = key;
		}
		private ItemProvider owner() {
			return ItemProvider.this;
		}
		@Override
		public ItemStack peek() {
			return key.toStack();
		}
		@Override
		public ItemStack consume() {
			var storage = storage();
			return storage != null
					&& storage.extract(key, 1, Actionable.MODULATE, new MachineSource(station.getActionHost())) == 1
							? key.toStack()
							: ItemStack.EMPTY;
		}
		@Override
		public ItemStack restore(ItemStack stack) {
			var storage = storage();
			if (storage == null || stack.isEmpty())
				return stack;
			long inserted = storage.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE,
					new MachineSource(station.getActionHost()));
			return stack.copyWithCount(stack.getCount() - (int) inserted);
		}
	}
}
