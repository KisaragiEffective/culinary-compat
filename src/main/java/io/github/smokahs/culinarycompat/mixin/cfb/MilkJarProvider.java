package io.github.smokahs.culinarycompat.mixin.cfb;

import java.util.Collection;

import net.blay09.mods.cookingforblockheads.api.CacheHint;
import net.blay09.mods.cookingforblockheads.api.IngredientToken;
import net.blay09.mods.cookingforblockheads.block.entity.MilkJarBlockEntity;
import net.blay09.mods.cookingforblockheads.compat.Compat;
import net.blay09.mods.cookingforblockheads.tag.ModItemTags;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.smokahs.culinarycompat.compat.cfb.FluidIngredients;

@Mixin(targets = "net.blay09.mods.cookingforblockheads.block.entity.MilkJarBlockEntity$MilkJarItemProvider", remap = false)
public abstract class MilkJarProvider {
	@Shadow
	@Final
	private MilkJarBlockEntity milkJar;
	@Inject(method = "findIngredient(Lnet/minecraft/world/item/ItemStack;Ljava/util/Collection;Lnet/blay09/mods/cookingforblockheads/api/CacheHint;)Lnet/blay09/mods/cookingforblockheads/api/IngredientToken;", at = @At("HEAD"), cancellable = true)
	private void culinarycompat$find(ItemStack stack, Collection<IngredientToken> reserved, CacheHint hint,
			CallbackInfoReturnable<IngredientToken> cir) {
		cir.setReturnValue(stack.is(ModItemTags.MILK)
				? FluidIngredients.find(milkJar.getFluidTank(), Compat.getMilkFluid(), stack, reserved)
				: null);
	}
}
