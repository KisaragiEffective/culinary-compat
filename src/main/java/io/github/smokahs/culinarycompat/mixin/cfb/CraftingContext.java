package io.github.smokahs.culinarycompat.mixin.cfb;

import net.blay09.mods.cookingforblockheads.api.Kitchen;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.smokahs.culinarycompat.compat.cfb.KitchenBridges;

@Mixin(value = net.blay09.mods.cookingforblockheads.crafting.CraftingContext.class, remap = false)
public abstract class CraftingContext implements KitchenBridges.Context {
	@Shadow
	@Final
	private Player player;
	@Unique
	private Kitchen culinarycompat$kitchen;
	@Inject(method = "<init>", at = @At("RETURN"))
	private void culinarycompat$init(Kitchen kitchen, Player player, CallbackInfo ci) {
		culinarycompat$kitchen = kitchen;
	}
	@Override
	public Kitchen culinarycompat$kitchen() {
		return culinarycompat$kitchen;
	}
	@Override
	public Player culinarycompat$player() {
		return player;
	}
}
