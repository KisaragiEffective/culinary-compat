package io.github.smokahs.culinarycompat.mixin.cfb;

import java.util.List;

import net.blay09.mods.cookingforblockheads.crafting.KitchenImpl;
import net.blay09.mods.cookingforblockheads.menu.KitchenMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.smokahs.culinarycompat.compat.cfb.KitchenBridges;

@Mixin(value = KitchenMenu.class, remap = false)
public abstract class RecipeBook {
	@Shadow
	@Final
	private Player player;
	@Shadow
	@Final
	private KitchenImpl kitchen;
	@Inject(method = "craft", at = @At("HEAD"), cancellable = true)
	private void culinarycompat$gate(ResourceLocation id, List<ItemStack> inputs, boolean fullStack, boolean inventory,
			CallbackInfo ci) {
		if (!KitchenBridges.hasWorkstation(kitchen, id, player))
			ci.cancel();
	}
}
