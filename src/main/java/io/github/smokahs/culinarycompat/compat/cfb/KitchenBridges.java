package io.github.smokahs.culinarycompat.compat.cfb;

import java.util.List;
import java.util.Set;

import net.blay09.mods.cookingforblockheads.api.CacheHint;
import net.blay09.mods.cookingforblockheads.api.Kitchen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.smokahs.culinarycompat.bridges.Bridges;

public final class KitchenBridges {
	private KitchenBridges() {
	}
	public static Bridges.Entry find(ResourceLocation id) {
		return Bridges.find(id);
	}
	public static boolean hasWorkstation(Kitchen kitchen, ResourceLocation id, Player player) {
		var entry = find(id);
		if (entry == null)
			return true;
		var workstation = entry.workstation();
		if (workstation.isEmpty())
			return true;
		if (!hasTool(kitchen, workstation))
			return false;
		var output = BuiltInRegistries.ITEM.getKey(entry.output().getItem());
		if (Croptopia.getFryingPanOutputs().contains(output) && !hasTool(kitchen, stack("farmersdelight:skillet")))
			return false;
		if (Croptopia.getCookingPotOutputs().contains(output) && !hasTool(kitchen, stack("farmersdelight:cooking_pot")))
			return false;
		if (Croptopia.getKnifeOutputs().contains(output) && !hasTool(kitchen, stack("farmersdelight:cutting_board")))
			return false;
		if (Croptopia.getBakewareOutputs().contains(output) && !hasTool(kitchen, stack("culinarycompat:bakeware")))
			return false;
		if (Croptopia.getFoodPressOutputs().contains(output) && !hasTool(kitchen, stack("croptopia:food_press")))
			return false;
		if (Croptopia.getMortarOutputs().contains(output) && !hasTool(kitchen, stack("croptopia:mortar_and_pestle")))
			return false;
		return true;
	}
	private static ItemStack stack(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
	}
	private static boolean hasTool(Kitchen kitchen, ItemStack workstation) {
		if (workstation.isEmpty())
			return false;
		var stationId = BuiltInRegistries.ITEM.getKey(workstation.getItem());
		Set<ResourceLocation> blocks = kitchen instanceof CFB.KitchenMemberView view
				? view.culinarycompat$getMemberBlocks()
				: Set.of();
		if (blocks.contains(stationId))
			return true;
		// Utensils must be in the kitchen, not just in the player's inventory.
		for (var provider : kitchen.getItemProviders(null)) {
			if (provider.findIngredient(workstation, List.of(), CacheHint.NONE) != null)
				return true;
			if (io.github.smokahs.culinarycompat.config.Configs.Common.restorePamsTools) {
				var replacement = switch (stationId.toString()) {
					case "farmersdelight:cutting_board" -> Pam.Cutting.cuttingboardItemId();
					case "farmersdelight:skillet" -> Pam.Skillet.skilletItemId();
					case "farmersdelight:cooking_pot" -> Pam.Pot.potItemId();
					case "culinarycompat:bakeware" -> Pam.Bakeware.PAM_BAKEWARE_ITEM_ID;
					default -> null;
				};
				if (replacement != null
						&& provider.findIngredient(new ItemStack(BuiltInRegistries.ITEM.get(replacement)), List.of(),
								CacheHint.NONE) != null)
					return true;
			}
		}
		return false;
	}
	public interface Context {
		Kitchen culinarycompat$kitchen();
		Player culinarycompat$player();
	}
}
