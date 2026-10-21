package io.github.smokahs.culinarycompat.mixin.cfb;

import net.blay09.mods.cookingforblockheads.registry.CookingForBlockheadsRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.smokahs.culinarycompat.compat.cfb.Croptopia;
import io.github.smokahs.culinarycompat.compat.cfb.FarmersDelight;
import io.github.smokahs.culinarycompat.compat.cfb.Pam;

@Mixin(value = CookingForBlockheadsRegistry.class, remap = false)
public abstract class Registry {
	@Inject(method = "reload", at = @At("HEAD"))
	private static void culinarycompat$begin(RecipeManager recipes, RegistryAccess registries, CallbackInfo ci) {
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server == null || server.getRecipeManager() != recipes)
			return;
		io.github.smokahs.culinarycompat.compat.cfb.CookingRegistry.begin(recipes);
	}
	@Inject(method = "reload", at = @At("RETURN"))
	private static void culinarycompat$afterInit(RecipeManager recipeManager, RegistryAccess registryAccess,
			CallbackInfo ci) {
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server == null || server.getRecipeManager() != recipeManager)
			return;
		FarmersDelight.Cutting.registerBridges(recipeManager, registryAccess);
		FarmersDelight.Campfire.registerBridges(recipeManager, registryAccess);
		if (ModList.get().isLoaded("farmersdelight")) {
			FarmersDelight.Pot.registerBridges(recipeManager, registryAccess);
		}
		if (ModList.get().isLoaded(Pam.PAM_MODID) && ServerLifecycleHooks.getCurrentServer() != null) {
			Pam.Cutting.registerBridges(recipeManager, registryAccess);
			Pam.Skillet.registerBridges(recipeManager, registryAccess);
			Pam.Pot.registerBridges(recipeManager, registryAccess);
			Pam.Bakeware.registerBridges(recipeManager, registryAccess);
		}
		if (ModList.get().isLoaded(Croptopia.CROPTOPIA_MODID) && ServerLifecycleHooks.getCurrentServer() != null) {
			Croptopia.Stripper.registerBridges(registryAccess);
		}
		io.github.smokahs.culinarycompat.compat.cfb.CookingRegistry.finish();
	}
}
