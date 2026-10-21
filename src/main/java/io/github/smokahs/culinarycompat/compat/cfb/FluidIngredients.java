package io.github.smokahs.culinarycompat.compat.cfb;

import java.util.Collection;

import net.blay09.mods.balm.api.fluid.FluidTank;
import net.blay09.mods.cookingforblockheads.api.IngredientToken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

public final class FluidIngredients {
	private FluidIngredients() {
	}
	public static IngredientToken find(FluidTank tank, Fluid fluid, ItemStack stack,
			Collection<IngredientToken> reserved) {
		int cost = stack.is(Items.WATER_BUCKET) || stack.is(Items.MILK_BUCKET) ? 1000 : 250;
		int used = reserved.stream().filter(token -> token instanceof Token t && t.tank == tank)
				.mapToInt(token -> ((Token) token).cost).sum();
		return tank.drain(fluid, used + cost, true) >= used + cost
				? new Token(tank, fluid, stack.copyWithCount(1), cost)
				: null;
	}
	private record Token(FluidTank tank, Fluid fluid, ItemStack stack, int cost) implements IngredientToken {
		@Override
		public ItemStack peek() {
			return tank.drain(fluid, cost, true) == cost ? stack.copy() : ItemStack.EMPTY;
		}
		@Override
		public ItemStack consume() {
			return tank.drain(fluid, cost, false) == cost ? stack.copy() : ItemStack.EMPTY;
		}
		@Override
		public ItemStack restore(ItemStack remainder) {
			if (ItemStack.isSameItemSameComponents(remainder, stack)) {
				return tank.fill(fluid, cost, false) == cost ? ItemStack.EMPTY : remainder;
			}
			return remainder;
		}
	}
}
