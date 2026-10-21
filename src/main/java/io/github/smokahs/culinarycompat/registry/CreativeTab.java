package io.github.smokahs.culinarycompat.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import io.github.smokahs.culinarycompat.CulinaryCompat;
import io.github.smokahs.culinarycompat.compat.ae2.Bridge;

@net.neoforged.fml.common.EventBusSubscriber(modid = CulinaryCompat.MODID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class CreativeTab {
	private static final ResourceLocation CROPTOPIA_KNIFE = ResourceLocation.fromNamespaceAndPath("croptopia", "knife");

	private CreativeTab() {
	}

	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			// bakeware serves Pam + croptopia baked goods; hide the block when both absent
			if (ModList.get().isLoaded("pamhc2foodcore") || ModList.get().isLoaded("croptopia")) {
				event.accept(Items.BAKEWARE.get());
			}
			event.accept(Items.GUIDEBOOK.get());
			if (Bridge.isActive()) {
				event.accept(Bridge.KITCHEN_STATION_ITEM.get());
			}
		}
		// FD present: nuke the redundant croptopia knife from every tab (incl. search)
		if (ModList.get().isLoaded("croptopia") && ModList.get().isLoaded("farmersdelight")) {
			removeCroptopiaKnife(event);
		}
	}

	private static void removeCroptopiaKnife(BuildCreativeModeTabContentsEvent event) {
		Item knife = BuiltInRegistries.ITEM.get(CROPTOPIA_KNIFE);
		if (knife == null) {
			return;
		}
		event.getParentEntries().removeIf(stack -> stack.is(knife));
		event.getSearchEntries().removeIf(stack -> stack.is(knife));
	}
}
