package io.github.smokahs.culinarycompat.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.ForgeRegistries;

import io.github.smokahs.culinarycompat.CulinaryCompat;

// strip EMI's "(+NBT)" tag from the ME kitchen station tooltip, weird
@net.neoforged.fml.common.EventBusSubscriber(modid = CulinaryCompat.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Tooltips {

	private static final String AE2_STATION_PATH = "ae2_kitchen_station";
	private static final String NBT_INDICATOR = "(+NBT)";

	private Tooltips() {
	}

	private static boolean isOurAe2Station(ItemStack stack) {
		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		return itemId != null && CulinaryCompat.MODID.equals(itemId.getNamespace())
				&& AE2_STATION_PATH.equals(itemId.getPath());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderTooltip(RenderTooltipEvent.GatherComponents event) {
		if (!isOurAe2Station(event.getItemStack())) {
			return;
		}
		event.getTooltipElements().removeIf(e -> e.left().filter(t -> {
			String s = t.getString();
			return s != null && NBT_INDICATOR.equals(s.trim());
		}).isPresent());
	}
}
