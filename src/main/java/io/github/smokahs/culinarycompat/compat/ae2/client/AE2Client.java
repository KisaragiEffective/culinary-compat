package io.github.smokahs.culinarycompat.compat.ae2.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import io.github.smokahs.culinarycompat.CulinaryCompat;
import io.github.smokahs.culinarycompat.compat.ae2.Bridge;

// client-only registration for the ME station menu screen
@net.neoforged.fml.common.EventBusSubscriber(modid = CulinaryCompat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AE2Client {

	private AE2Client() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		if (!Bridge.isActive()) {
			return;
		}
		event.enqueueWork(() -> MenuScreens.register(Bridge.KITCHEN_STATION_MENU.get(), StationScreen::new));
	}
}
