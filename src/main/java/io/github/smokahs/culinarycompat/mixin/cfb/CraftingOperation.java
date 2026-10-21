package io.github.smokahs.culinarycompat.mixin.cfb;

import java.util.ArrayList;
import java.util.List;

import net.blay09.mods.cookingforblockheads.api.IngredientToken;
import net.blay09.mods.cookingforblockheads.crafting.CraftingContext;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.smokahs.culinarycompat.bridges.Bridges;
import io.github.smokahs.culinarycompat.compat.cfb.BakeState;
import io.github.smokahs.culinarycompat.compat.cfb.KitchenBridges;
import io.github.smokahs.culinarycompat.config.Configs;
import io.github.smokahs.culinarycompat.network.Network.BakePhase;
import io.github.smokahs.culinarycompat.recipe.RecipeIds;

@Mixin(value = net.blay09.mods.cookingforblockheads.crafting.CraftingOperation.class, remap = false)
public abstract class CraftingOperation {
	@Shadow
	@Final
	private CraftingContext context;
	@Shadow
	@Final
	private Recipe<?> recipe;
	@Shadow
	@Final
	private List<IngredientToken> ingredientTokens;
	@Shadow
	@Final
	private List<net.minecraft.world.item.crafting.Ingredient> missingIngredients;
	@Shadow
	public abstract boolean canCraft();
	@Unique
	private ResourceLocation culinarycompat$id;

	@Inject(method = "craft(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
	private void culinarycompat$crafted(AbstractContainerMenu menu, RegistryAccess registries,
			CallbackInfoReturnable<ItemStack> cir) {
		var player = ((KitchenBridges.Context) context).culinarycompat$player();
		if (player != null && KitchenBridges.find(culinarycompat$id) != null && !cir.getReturnValue().isEmpty())
			io.github.smokahs.culinarycompat.compat.cfb.UI.CraftSound.crafted(player, cir.getReturnValue());
	}

	@Inject(method = "prepare(Z)Lnet/blay09/mods/cookingforblockheads/crafting/CraftingOperation;", at = @At("RETURN"))
	private void culinarycompat$missingWorkstation(boolean includeOptions,
			CallbackInfoReturnable<net.blay09.mods.cookingforblockheads.crafting.CraftingOperation> cir) {
		var view = (KitchenBridges.Context) context;
		var entry = KitchenBridges.find(culinarycompat$id);
		if (entry != null && !KitchenBridges.hasWorkstation(view.culinarycompat$kitchen(), culinarycompat$id,
				view.culinarycompat$player()))
			missingIngredients.add(net.minecraft.world.item.crafting.Ingredient.of(entry.workstation()));
	}
	@Inject(method = "<init>", at = @At("RETURN"))
	private void culinarycompat$init(CraftingContext context, RecipeHolder<Recipe<?>> holder, CallbackInfo ci) {
		culinarycompat$id = holder.id();
		RecipeIds.remember(holder.id(), holder.value());
	}
	@Inject(method = "canCraft", at = @At("RETURN"), cancellable = true)
	private void culinarycompat$gate(CallbackInfoReturnable<Boolean> cir) {
		var view = (KitchenBridges.Context) context;
		if (!KitchenBridges.hasWorkstation(view.culinarycompat$kitchen(), culinarycompat$id,
				view.culinarycompat$player()))
			cir.setReturnValue(false);
	}
	@Inject(method = "craft(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private void culinarycompat$bake(AbstractContainerMenu menu, RegistryAccess registries,
			CallbackInfoReturnable<ItemStack> cir) {
		var entry = KitchenBridges.find(culinarycompat$id);
		if (entry == null || Bridges.Kind.fromSource(entry.source()) != Bridges.Kind.BAKEWARE
				|| !Configs.Common.bakewareEnabled)
			return;
		if (!(((KitchenBridges.Context) context).culinarycompat$player() instanceof ServerPlayer player))
			return;
		cir.setReturnValue(ItemStack.EMPTY);
		if (!canCraft())
			return;
		var pending = BakeState.Manager.get(player.getUUID());
		if (pending != null && pending.phase == BakePhase.BAKING)
			return;
		var outputId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.output().getItem());
		if (pending == null || !pending.outputId.equals(outputId)) {
			BakeState.Manager.setConfirm(player, outputId);
			return;
		}
		List<ItemStack> consumed = new ArrayList<>();
		for (int slot = 0; slot < ingredientTokens.size(); slot++) {
			var token = ingredientTokens.get(slot);
			if (recipe.getIngredients().get(slot).isEmpty()) {
				consumed.add(ItemStack.EMPTY);
				continue;
			}
			var stack = token.consume();
			if (stack.isEmpty()) {
				for (int i = 0; i < consumed.size(); i++)
					context.restore(ingredientTokens.get(i).restore(consumed.get(i)));
				return;
			}
			consumed.add(stack);
		}
		var remainders = consumed.stream()
				.map(stack -> stack.isEmpty() ? ItemStack.EMPTY : stack.getCraftingRemainingItem()).toList();
		BakeState.Manager.setBaking(player, outputId, entry.output(), consumed, remainders);
	}
}
